/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.profileIngester;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.logging.log4j.ThreadContext;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.profile.service.ResearcherProfileService;
import org.dspace.uclouvain.administer.AbstractCLICommand;
import org.dspace.uclouvain.core.model.PersonEventModel;
import org.dspace.uclouvain.exceptions.UserNotFoundException;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.profileIngester.actions.factory.ProfileActionFactory;
import org.dspace.uclouvain.profileIngester.exceptions.IDMCheckException;
import org.dspace.uclouvain.profileIngester.exceptions.ProfileActionException;
import org.dspace.uclouvain.profileIngester.services.IDMPersonValidityService;
import org.dspace.uclouvain.rabbitMQ.connectors.PersonEventConnector;
import org.dspace.uclouvain.rabbitMQ.connectors.PersonEventErrorConnector;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CLI to track user events from RabbitMQ and update corresponding researcher profile.
 * The event action type present in Rabbit determines the action to perform on the researcher profile.
 * This CLI can be executed multiple times together to increased processing rate.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class ProfileIngesterCLI extends AbstractCLICommand {

    private static final Option OPT_PERSON = Option.builder("e")
        .longOpt("eperson")
        .hasArg(true)
        .desc("email address of eperson doing importing.")
        .required(true)
        .build();
    private static final Option OPT_ERROR_QUEUE_NAME = Option.builder("eq")
        .longOpt("error_queue")
        .hasArg(true)
        .desc("RabbitMQ error queue name to store potential errors. (Defaults to {queue_name + '_error'})")
        .required(false)
        .build();
    public static final String USAGE_DESCRIPTION =
        "A command-line tool to listen for person event and process corresponding action in DSpace.";

    private static final Logger logger = LoggerFactory.getLogger(ProfileIngesterCLI.class);

    // EXTERNAL SERVICES
    protected EPersonService ePersonService;
    protected ResearcherProfileService researcherProfileService;
    protected PersonEventConnector personEventConnector;
    protected IDMPersonValidityService idmService;

    // CONSTRUCTOR------------------------------------------------
    ProfileIngesterCLI() throws IOException, TimeoutException {
        researcherProfileService = new DSpace().getSingletonService(ResearcherProfileService.class);
        idmService = UCLouvainServiceFactory.getInstance().getIDMPersonValidityService();
    }

    // COMMON CLICommand METHODS----------------------------------
    @Override
    protected void buildOptions() {
        serviceOptions.addOption(OPT_PERSON);
        serviceOptions.addOption(OPT_ERROR_QUEUE_NAME);
        infoOptions.addOption(OPT_HELP);
    }

    @Override
    protected String getUsageDescription() {
        return USAGE_DESCRIPTION;
    }

    // METHODS----------------------------------------------------
    public static void main(String[] arguments) throws Exception {
        ProfileIngesterCLI profileIngester = new ProfileIngesterCLI();
        CommandLine cli = profileIngester.validateCLIArgument(arguments);

        PersonEventConnector connector = new PersonEventConnector();

        String epersonEmail = cli.getOptionValue("e");
        String errorQueueName = cli.getOptionValue("eq", connector.getQueueName() + "_error");

        Context context = new Context();
        // Get the eperson to use as initiator.
        EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().findByEmail(context, epersonEmail);
        if (ePerson == null) {
            throw new UserNotFoundException(epersonEmail);
        }
        context.setCurrentUser(ePerson);

        while (true) {
            try {
                profileIngester.ingestEvent(context, connector, errorQueueName);
            } catch (InterruptedException ie) {
                logger.info("Execution interrupted manually");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Launches the cli ingest process: wait for events in the rabbit queue and do the required action.
     * @param context The current DSpace context.
     * @param connector The RabbitMQ connector to use to get event messages from queue.
     * @param errorQueueName The queue name to post potential error message to.
     * @throws Exception
     */
    private void ingestEvent(Context context, PersonEventConnector connector, String errorQueueName)
        throws Exception {
        // Set a random uuid to this thread context to identify this job in the logs.
        String jobId = UUID.randomUUID().toString();
        ThreadContext.put("jobId", jobId);

        // Instantiate a rabbit channel to pull events from.
        Channel channel = connector.getChannel();
        logger.info("[WAITING FOR MESSAGES] - Press CTRL+C to exit");

        // Create a delivery callback that will be executed for each event that is red.
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            // Put the jobId again since rabbitMQ creates a new thread context.
            ThreadContext.put("jobId", jobId);
            // Extract event data from message.
            String eventString = new String(delivery.getBody(), StandardCharsets.UTF_8);
            logger.info("[RECEIVED EVENT] Received '" + eventString + "', started processing...");
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                PersonEventModel event = objectMapper.readValue(eventString, PersonEventModel.class);
                processEvent(context, event);
            } catch (Exception e) {
                logError(errorQueueName, eventString, e);
            } finally {
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                ThreadContext.remove("jobId");
            }
        };
        // Use the created callback to consume events on the queue.
        channel.basicConsume(connector.getQueueName(), false, deliverCallback, consumerTag -> { });

        // Block to keep the application running after the consume.
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
     * Using the recovered event, try to find and execute a profile action class.
     * Only process the event if the person has a valid IDM entry id.
     * 
     * @param context The current DSpace context.
     * @param event The event to get a profile action class for.
     */
    private void processEvent(Context context, PersonEventModel event) throws ProfileActionException {
        try {
            if (idmService.isPersonIDMValid(event.getFgs())) {
                ProfileActionFactory.getInstance().getProfileActionClass(event.getAction()).process(context, event);
            } else {
                logger.info(
                    "[IGNORED EVENT] Received person with fgs " + event.getFgs() + " has no valid IDM membership..."
                );
            }
        } catch (IDMCheckException idme) {
            logger.warn(
                "[IGNORED EVENT] Impossible to check IDM validity of " + event.getFgs() + " :: " + idme.getMessage()
            );
        }
    }

    /**
     * Log an error in a specific rabbitMQ queue.
     * @param queueName The queue to log the error in.
     * @param event The event that caused the error.
     * @param cause The error itself.
     * @throws IOException
     */
    private void logError(String queueName, String event, Exception cause) throws IOException {
        PersonEventErrorConnector errorConnector = new PersonEventErrorConnector(queueName);
        try {
            // Push error into specific queue.
            errorConnector.publishErrorMessage(event, cause);
        } catch (Exception ignored) {
            // Ignore this exception if occurs.
        }
        logger.error("Cannot process event: " + event, cause);
    }
}
