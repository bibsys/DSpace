/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.administer;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.lang3.StringUtils;
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
import org.dspace.uclouvain.exceptions.UserNotFoundException;
import org.dspace.uclouvain.packager.SubmissionValidationException;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A command-line tool to batch ingest some SIP in METS packager format from Fedora.
 *
 * USAGE:
 *   dspace dsrun org.dspace.uclouvain.administer.RabbitMQPackager -e [mail] -q [queue_name] -E
 *
 * MESSAGE:
 *   Each message of the queue is a JSON object:
 *     {"source": "my-producer", "path": "/path/to/sip.zip", "validate": true, "allowReplace": false}
 *   - source:       (required) the name of the producer of the message. It isn't used by the ingestion: it is
 *                   copied into the error message, so that the errors can be sent back to the right producer.
 *   - path:         (required) the file path of the METS SIP archive to ingest.
 *   - validate:     (default true) check the item against the submission validators before archiving it.
 *   - allowReplace: (default false) replace the object if it already exists; otherwise such a SIP is refused.
 *
 * ERROR MESSAGE:
 *   A SIP which cannot be ingested produces a JSON object into the error queue:
 *     {"source": "my-producer", "archivePath": "/path/to/sip.zip", "error": "...", "validationErrors": [...]}
 *   - source:           the `source` of the ingestion message; null if that message could not be read.
 *   - archivePath:      the `path` of the ingestion message; the raw message if it could not be read.
 *   - error:            the description of the error.
 *   - validationErrors: (only for a SIP refused by the validation) the list of the validation errors.
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
    public static final String USAGE_DESCRIPTION = "A command-line tool to batch ingest METS SIP archive";

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
        String errorQueueName = cl.getOptionValue('E', queueName + "_error");
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
     * Entry payload data of each queue message must be a JSON object (see class documentation).
     * If any error occurred in message processing, this error will be logged into another
     * RabbitMQ queue and the original message will be marked as "done".
     *
     * @param email The email of the user that will proceed the archive ingest.
     * @param queueName The RabbitMQ queue name where the message should be read.
     * @param errorQueueName The RabbitMQ queue name where the error messages will be published.
     */
    private void run(String email, String queueName, String errorQueueName) throws Exception {
        // Create a new context and populate it with required data.
        Context context = new Context();
        EPerson ePerson = ePersonService.findByEmail(context, email);
        if (ePerson == null) {
            throw new UserNotFoundException(email);
        }
        context.setCurrentUser(ePerson);

        // Create connection with RabbitMQ server and consumer queue.
        Channel channel = mqConnection.createChannel();
        channel.queueDeclare(queueName, true, false, false, null);
        channel.basicQos(1);  // accept only one unack-ed message at a time
        logger.info(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            logger.info(" [*] Received '" + message + "'...");
            String source = null;          // unknown as long as the message isn't read
            String archivePath = message;  // reported as is when the message cannot be read
            try {
                IngestMessage request = parseMessage(message);
                source = request.source();
                archivePath = StringUtils.defaultIfBlank(request.path(), message);
                checkRequiredAttributes(request);
                ingestBatch(context, getIngester(), archivePath, request.validate(), request.allowReplace());
            } catch (Exception ex) {
                logErrorIntoRabbitQueue(errorQueueName, source, archivePath, ex);
            } finally {
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };
        channel.basicConsume(queueName, false, deliverCallback, consumerTag -> { });
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
     * Read a message of the ingestion queue.
     *
     * @param message The JSON message; `validate` defaults to true and `allowReplace` to false.
     * @return the content of the message; a missing `source` or `path` is null.
     * @throws IOException if the message isn't a JSON object.
     */
    private IngestMessage parseMessage(String message) throws IOException {
        JsonNode json = new ObjectMapper().readTree(message);
        return new IngestMessage(
            json.path("source").asText(null),
            json.path("path").asText(null),
            json.path("validate").asBoolean(true),
            json.path("allowReplace").asBoolean(false)
        );
    }

    /**
     * Refuse a message without `source` or without `path`.
     * This check is separated from the reading of the message so that the attribute which is present can still
     * be reported into the error queue.
     *
     * @param request The content of the message.
     * @throws PackageException if a required attribute is missing.
     */
    private void checkRequiredAttributes(IngestMessage request) throws PackageException {
        if (StringUtils.isBlank(request.source())) {
            throw new PackageException("Invalid message: the 'source' attribute is required");
        }
        if (StringUtils.isBlank(request.path())) {
            throw new PackageException("Invalid message: the 'path' attribute is required");
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
     * @param context The application context.
     * @param sip The SIP ingester to use to ingest object into repository
     * @param archivePath The file path of the SIP archive to ingest/replace
     * @param validate Whether the item must pass the submission validators before being archived
     * @param allowReplace Whether an already existing object may be replaced
     * @throws PackageException if any ingestion problem occurred.
     * @throws SQLException if any database exception occurred.
     */
    private void ingestBatch(
        Context context,
        AbstractMETSIngester sip,
        String archivePath,
        boolean validate,
        boolean allowReplace
    ) throws PackageException, SQLException {
        File workingFile = null;
        context.setMode(Context.Mode.BATCH_EDIT);
        try {
            // Get the working file based on the message from RabbitMQ
            workingFile = getWorkingFileFromSource(archivePath);
            // Load the METS manifest from packager file
            METSManifest manifest = sip.parsePackage(context, workingFile, new PackageParameters());
            if (manifest == null) {
                throw new PackageException("Invalid METS manifest");
            }
            // Get the PID from METS manifest && check if PID already exists into the system.
            // A manifest without PID describes a new object: there is nothing to search or to replace.
            DSpaceObject objectToReplace = null;
            String legacyID = manifest.getID();
            if (StringUtils.isNotBlank(legacyID)) {
                legacyID = legacyID.replace("-", ":");
                logger.info("\tlegacy id is :: " + legacyID);
                objectToReplace = getObjectFromIdentifier(context, "fedora.pid", legacyID);
            }
            if (objectToReplace != null && !allowReplace) {
                throw new PackageException("Object [" + Constants.typeText[objectToReplace.getType()] + "#"
                        + objectToReplace.getID() + "] already exists for PID " + legacyID
                        + " and the message does not allow to replace it");
            }
            // Ingest the object into DSpace system
            //   * If the `objectToReplace` is null, the `replace` function will simply ingest.
            //   * If ingest/replace is well done, we can remove the working packager file.
            //   * If ingest/replace failed, move the working file to `error` directory and raise exception.
            try {
                DSpaceObject ingestedObject = null;
                PackageParameters pkgParams = new PackageParameters();
                pkgParams.setWorkflowEnabled(false);
                pkgParams.setSubmissionValidationEnabled(validate);
                if (objectToReplace != null) {
                    logger.info("\tObject already exists ? [TRUE] --> [" + Constants.typeText[objectToReplace.getType()]
                            + "#" + objectToReplace.getID() + "]");
                    pkgParams.setRestoreModeEnabled(true);
                    pkgParams.setReplaceModeEnabled(true);
                    ingestedObject = sip.replace(context, objectToReplace, workingFile, pkgParams);
                } else {
                    logger.info("\tObject already exists ? [FALSE]");
                    ingestedObject = sip.ingest(context, null, workingFile, pkgParams, null);
                }
                logger.info("\tObject [" + Constants.typeText[ingestedObject.getType()] + "#" + ingestedObject.getID()
                        + "] ingested");
            } catch (CrosswalkException | WorkflowException ex) {
                throw new PackageException(ex.getClass().getSimpleName() + "::" + ex.getMessage());
            }
            workingFile.delete();
            context.commit();
        } catch (SubmissionValidationException ex) {
            // The ingester deleted the refused item: committing that deletion is what lets the standard cleanup
            // remove its files from the asset store ; A simple rollback would leave them there, orphaned.
            context.commit();
            logger.error(ex.getMessage());
            moveWorkingFileToError(workingFile, archivePath);
            throw ex;
        } catch (Exception ex) {
            context.rollback();
            logger.error(ex.getMessage(), ex);
            moveWorkingFileToError(workingFile, archivePath);
            throw new PackageException(ex.getClass().getSimpleName() + " :: " + ex.getMessage());
        }
    }

    /**
     * Give back its original name to the working file and move it into the error directory.
     *
     * @param workingFile the working file; null if the failure occurred before it was created.
     * @param archivePath the original file path of the SIP archive.
     */
    private void moveWorkingFileToError(File workingFile, String archivePath) {
        if (workingFile != null) {
            File originalFile = new File(archivePath);
            workingFile.renameTo(originalFile);
            moveFileToError(originalFile);
        }
    }


    /** Log any error occurring during a batch ingestion process into the RabbitMQ error queue.
     *
     * @param queueName The RabbitMQ queue name where the message must be published.
     * @param source The producer of the ingestion message; null if unknown.
     * @param archivePath The archive filename generating the error.
     * @param cause The exception causing the error.
     * @throws IOException if any errors occur during RabbitMQ connection.
     */
    private void logErrorIntoRabbitQueue(String queueName, String source, String archivePath, Exception cause)
            throws IOException {
        // Build the JSON message that will be sent to RabbitMQ queue.
        Map<String, Object> message = new HashMap<>();
        message.put("source", source);
        message.put("archivePath", archivePath);
        message.put("error", cause.getMessage());
        if (cause instanceof SubmissionValidationException refusal) {
            message.put("validationErrors", refusal.getErrors());
        }
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
        } catch (Exception ignored) {
            // do nothing
        }
    }


    /**
     * Load a packer file to determine if this file exists;
     * Rename the file to avoid paralleling ingest problems.
     *
     * @param archivePath the original packager file to load.
     * @return the archive file to ingest (!! different that original archive path)
     */
    private File getWorkingFileFromSource(String archivePath) throws IOException {
        File sourceFile = new File(archivePath);
        if (!sourceFile.exists()) {
            throw new FileNotFoundException();
        }
        if (!sourceFile.isFile()) {
            throw new IOException(archivePath + "isn't a regular file");
        }
        // At this time, we know the file exists.
        // We will rename it to avoid that another ingester process uses the same file
        // because the rabbit queue contains some duplicates.
        Path inProgressPath = Paths.get(sourceFile.getParentFile().getPath(), "inProgress_" + sourceFile.getName());
        File workingSpace = inProgressPath.toFile();
        if (!sourceFile.renameTo(workingSpace)) {
            throw new IOException("Error to rename '" + sourceFile.getName() + "' to '" + workingSpace.getName() + "'");
        }
        return workingSpace;
    }


    /**
     * Get the package ingester based on the configured package type.
     *
     * @return the package ingester to use for this packager.
     * @throws PackageException if ingester cannot be found
     */
    private AbstractMETSIngester getIngester() throws PackageException {
        AbstractMETSIngester sip =
                (AbstractMETSIngester) pluginService.getNamedPlugin(PackageIngester.class, packageType);
        if (sip == null) {
            throw new PackageException("Unknown package type: " + packageType);
        }
        return sip;
    }


    /**
     * Get any DspaceObject that already exists into the system referencing an identifier value.
     *
     * @param context: the application context
     * @param key the identifier key to search (the key in `Solr.search` core)
     * @param value the identifier value to search.
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
        } catch (IOException ignored) {
            // do nothing
        }
    }

    // PRIVATE CLASS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /** The content of a message of the ingestion queue. */
    private record IngestMessage(String source, String path, boolean validate, boolean allowReplace) { }

}
