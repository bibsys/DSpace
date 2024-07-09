package org.dspace.uclouvain.administer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.packager.AbstractMETSIngester;
import org.dspace.content.packager.METSManifest;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageIngester;
import org.dspace.content.packager.PackageParameters;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import com.rabbitmq.client.Channel;
import org.dspace.uclouvain.exceptions.UserNotFoundException;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * A command-line tool to batch ingest some SIP in METS packager format from Fedora.
 *
 * USAGE:
 *   dspace dsrun org.dspace.uclouvain.administer.RabbitMQPackager -e [mail] -q [queue_name] -E
 *
 * ARGUMENTS:
 *   -e, --email:       the email of the ePerson that will ingest the archive.
 *   -q, --queue:       the RabbitMQ queue name to consume.
 *   -E, --error_queue: the RabbitMQ queue name where error will be published (default is `-queue` param + '_error')
 *   -h, --help:        display CLI command options.
 *
 * @author Renaud Michotte <renaud.michotte@uclouvain.be>
 * @version $Revision$
 */
public class RabbitMQPackager extends AbstractCLICommand {

    // CLASS CONSTANTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQPackager.class);
    private static final Option OPT_PERSON = Option.builder("e")
            .longOpt("eperson")
            .hasArg(true)
            .desc("email address of eperson doing importing")
            .required(true)
            .build();
    private static final Option OPT_CONSUMER_QUEUE = Option.builder("q")
            .longOpt("queue")
            .hasArg(true)
            .desc("RabbitMQ queue name where find the message to consume")
            .required(true)
            .build();
    private static final Option OPT_ERROR_CONSUMER_QUEUE = Option.builder("E")
            .longOpt("error_queue")
            .hasArg(true)
            .desc("RabbitMQ queue name where publish the message if errors occurred during ingestion")
            .build();
    public static final String USAGE_DESCRIPTION = "A command-line tool to batch ingest some METS SIP archive from Fedora.";

    // CLASS ATTRIBUTES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected EPersonService ePersonService;
    protected SearchService searchService;
    protected Connection mqConnection;
    protected PluginService pluginService;
    protected String packageType = "METS";

    // MAIN ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public static void main(String[] argv) throws Exception {
        RabbitMQPackager packager = new RabbitMQPackager();
        CommandLine cl = packager.validateCLIArgument(argv);

        String ePersonEmail = cl.getOptionValue('e');
        String queueName = cl.getOptionValue('q');
        String errorQueueName = cl.getOptionValue('E', queueName+"_error");
        while (true) {
            try {
                packager.run(ePersonEmail, queueName, errorQueueName);
            } catch (InterruptedException ie) {
                logger.info("Program interrupted");
                Thread.currentThread().interrupt(); // resume interrupt status
                break; // leave the infinite loop
            } catch (Exception ex) {
                logger.error(ex.getClass().getName() + " :: " + ex.getMessage(), ex);
            }
        }
    }

    // CONSTRUCTOR ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /** Constructor */
    protected RabbitMQPackager() throws IOException, TimeoutException {
        ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        searchService = new DSpace().getSingletonService(SearchService.class);
        mqConnection = createRabbitMQConnection();
        pluginService = CoreServiceFactory.getInstance().getPluginService();
    }

    protected void buildOptions() {
        serviceOptions.addOption(OPT_PERSON);
        serviceOptions.addOption(OPT_CONSUMER_QUEUE);
        serviceOptions.addOption(OPT_ERROR_CONSUMER_QUEUE);
        infoOptions.addOption(OPT_HELP);
    }

    protected String getUsageDescription() {
        return USAGE_DESCRIPTION;
    }


    // PRIVATE FUNCTIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * Batch ingestion of METS packager files from a RabbitMQ queue.
     * The function will read the SIP queue (from RabbitMQ server).
     * Entry payload data of each queue message must be a filepath for a METS packager file.
     * If any error occurred in message processing, this error will be logged into another
     * RabbitMQ queue and the original message will be marked as "done".
     *
     * @param email: The email of the user that will proceed the archive ingest.
     * @param queueName: The RabbitMQ queue name where the message should be read.
     * @param errorQueueName: The RabbitMQ queue name where the error messages will be published.
     */
    private void run(String email, String queueName, String errorQueueName) throws Exception {
        // Create a new context and populate it with required data.
        Context context = new Context();
        EPerson ePerson = ePersonService.findByEmail(context, email);
        if (ePerson == null)
            throw new UserNotFoundException(email);
        context.setCurrentUser(ePerson);
        context.setMode(Context.Mode.BATCH_EDIT);

        // Create connection with RabbitMQ server and consumer queue.
        Channel channel = mqConnection.createChannel();
        channel.queueDeclare(queueName, true, false, false, null);
        channel.basicQos(1);  // accept only one unack-ed message at a time
        logger.info(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            logger.info(" [*] Received '" + message + "'...");
            try {
                ingestBatch(context, getIngester(), message);
            } catch(Exception ex) {
                logErrorIntoRabbitQueue(errorQueueName, message, ex);
            }
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
        // Block to keep the application running.
        //     As `basicConsume` is an asynchronous method, we need to simulate
        //     this method keeps running otherwise it will directly after queue
        //     will be totally consumed.
        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                context.complete();
            }
        }
    }


    /**
     * Create and return a connection to a RabbitMQServer from application configured factory
     * @return A connection to the RabbitMQServer
     * @throws IOException if any communication exception occurred.
     * @throws TimeoutException if connection is too slow to establish.
     */
    private Connection createRabbitMQConnection() throws IOException, TimeoutException {
        return ((ConnectionFactory) (new DSpace()
            .getServiceManager()
            .getApplicationContext()
            .getBean("rabbitConnectionFactory")
        )).newConnection();
    }


    /**
     * Ingests/replaces a METS SIP archive into DSpace repository.
     *
     * @param context: The application context.
     * @param sip: The SIP ingester to use to ingest object into repository
     * @param archivePath: The file path of the SIP archive to ingest/replace
     * @throws PackageException if any ingestion problem occurred.
     * @throws SQLException if any database exception occurred.
     */
    private void ingestBatch(Context context, AbstractMETSIngester sip, String archivePath) throws PackageException, SQLException {
        File workingFile = null;
        try {
            // Get the working file based on the message from RabbitMQ
            workingFile = getWorkingFileFromSource(archivePath);
            // Load the METS manifest from packager file
            METSManifest manifest = sip.parsePackage(context, workingFile, new PackageParameters());
            if (manifest == null) {
                throw new PackageException("Invalid METS manifest");
            }
            // Get the PID from METS manifest && check if PID already exists into the system.
            String fedoraPid = manifest.getID();
            if (fedoraPid == null)
                throw new PackageException("Unable to find original PID");
            fedoraPid = fedoraPid.replace("-", ":");
            logger.info("\tFedora pid is :: " + fedoraPid);
            DSpaceObject objectToReplace = getObjectFromIdentifier(context, "fedora.pid", fedoraPid);
            // Ingest the object into DSpace system
            //   * If the `objectToReplace` is null, the `replace` function will simply ingest.
            //   * If ingest/replace is well done, we can remove the working packager file.
            //   * If ingest/replace failed, move the working file to `error` directory and raise exception.
            try {
                DSpaceObject ingestedObject = null;
                PackageParameters pkgParams = new PackageParameters();
                pkgParams.setWorkflowEnabled(false);
                if (objectToReplace != null) {
                    logger.info("\tObject already exists ? [TRUE] --> [" + Constants.typeText[objectToReplace.getType()] + "#" + objectToReplace.getID() + "]");
                    pkgParams.setRestoreModeEnabled(true);
                    pkgParams.setReplaceModeEnabled(true);
                    ingestedObject = sip.replace(context, objectToReplace, workingFile, pkgParams);
                } else {
                    logger.info("\tObject already exists ? [FALSE]");
                    ingestedObject = sip.ingest(context, null, workingFile, pkgParams, null);
                }
                logger.info("\tObject [" + Constants.typeText[ingestedObject.getType()] + "#" + ingestedObject.getID() + "] ingested");
            } catch (CrosswalkException | WorkflowException ex) {
                throw new PackageException(ex.getClass().getSimpleName() + "::" + ex.getMessage());
            }
            workingFile.delete();
            context.commit();
        } catch (Exception ex) {
            context.rollback();
            logger.error(ex.getMessage(), ex);
            if (workingFile != null) {
                File originalFile = new File(archivePath);
                workingFile.renameTo(originalFile);
                moveFileToError(originalFile);
            }
            throw new PackageException(ex.getClass().getSimpleName() + " :: " + ex.getMessage());
        }
    }


    /** Log any error occurring during a batch ingestion process into the RabbitMQ error queue.
     *
     * @param queueName: The RabbitMQ queue name where the message must be published.
     * @param archivePath: The archive filename generating the error.
     * @param cause: The exception causing the error.
     * @throws IOException if any errors occur during RabbitMQ connection.
     */
    private void logErrorIntoRabbitQueue(String queueName, String archivePath, Exception cause) throws IOException {
        // Build the JSON message that will be sent to RabbitMQ queue.
        Map<String, String> message = new HashMap<>();
        message.put("archivePath", archivePath);
        message.put("error", cause.getMessage());
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonMessage = objectMapper.writeValueAsString(message);

        // Publish the message into the RabbitMQ error queue
        try {
            Connection conn = createRabbitMQConnection();
            Channel channel = conn.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
            channel.basicPublish("", queueName, null, jsonMessage.getBytes(StandardCharsets.UTF_8));
            logger.error("!! Error when trying to ingest '" + archivePath + "' :: " + cause.getMessage(), cause);
            channel.close();
            conn.close();
        } catch (Exception ignored) { }
    }


    /**
     * Load a packer file to determine if this file exists;
     * Rename the file to avoid paralleling ingest problems.
     *
     * @param archivePath: the original packager file to load.
     * @return the archive file to ingest (!! different that original archive path)
     */
    private File getWorkingFileFromSource(String archivePath) throws IOException {
        File sourceFile = new File(archivePath);
        if (!sourceFile.exists())
            throw new FileNotFoundException();
        if (!sourceFile.isFile())
            throw new IOException(archivePath + "isn't a regular file");
        // At this time, we know the file exists.
        // We will rename it to avoid that another ingester process uses the same file
        // because the rabbit queue contains some duplicates.
        Path inProgressPath = Paths.get(sourceFile.getParentFile().getPath(), "inProgress_" + sourceFile.getName());
        File workingSpace = inProgressPath.toFile();
        if (!sourceFile.renameTo(workingSpace))
            throw new IOException("Error to rename '" + sourceFile.getName() + "' to '" + workingSpace.getName() + "'");
        return workingSpace;
    }


    /**
     * Get the package ingester based on the configured package type.
     *
     * @return the package ingester to use for this packager.
     * @throws PackageException if ingester cannot be found
     */
    private AbstractMETSIngester getIngester() throws PackageException {
        AbstractMETSIngester sip = (AbstractMETSIngester) pluginService.getNamedPlugin(PackageIngester.class, packageType);
        if (sip == null)
            throw new PackageException("Unknown package type: " + packageType);
        return sip;
    }


    /**
     * Get any DspaceObject that already exists into the system referencing an identifier value.
     *
     * @param context: the application context
     * @param key: the identifier key to search (the key in `Solr.search` core)
     * @param value: the identifier value to search.
     * @return the DSpaceObject referencing the identifier; return `null` if no object is found.
     */
    private DSpaceObject getObjectFromIdentifier(Context context, String key, String value) {
       DiscoverQuery dq = new DiscoverQuery();
        dq.setMaxResults(1);
        dq.setQuery(String.format("%s:\"%s\"", key, value));
        try {
            DiscoverResult result = searchService.search(context, dq);
            return (result.getTotalSearchResults() == 0)
                ? null
                : (DSpaceObject) result.getIndexableObjects().get(0).getIndexedObject();
        } catch (SearchServiceException sse) {
            return null;
        }
    }


    /**
     * Move a working packager file into the error directory.
     *
     * @param fileToMove the packager file to move.
     */
    private void moveFileToError(File fileToMove) {
        File parentDirectory = fileToMove.getParentFile();
        try {
            Path errorDirectory = Paths.get(parentDirectory.getAbsolutePath(), "errors");
            Files.createDirectories(errorDirectory);
            Path errorFilePath = Paths.get(parentDirectory.getAbsolutePath(), "errors", fileToMove.getName());
            Files.move(fileToMove.toPath(), errorFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) { }
    }


}
