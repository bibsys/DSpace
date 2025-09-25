/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.profileIngester;

import java.io.BufferedReader;
import java.io.FileReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.dspace.uclouvain.administer.AbstractCLICommand;
import org.dspace.uclouvain.core.model.PersonEventModel;
import org.dspace.uclouvain.rabbitMQ.connectors.PersonEventConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CLI to import a batch of fgs in RabbitMQ for profile ingestion.
 * The CLI takes a file containing fgs identifiers and pushes events with the given action type in RabbitMQ.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class ProfileBatchImportCLI extends AbstractCLICommand {
    private static final Option OPT_FILE = Option.builder("f")
        .longOpt("file")
        .hasArg(true)
        .desc("File path containing fgs to import")
        .required(true)
        .build();
    private static final Option OPT_EVENT_ACTION = Option.builder("ea")
        .longOpt("event_action")
        .hasArg(true)
        .desc("Optional event action to send for each fgs ('create', 'update' or 'delete'). Default is 'create'")
        .required(false)
        .build();
    private static final Option OPT_EVENT_INFORMATION = Option.builder("ei")
        .longOpt("event_information")
        .hasArg(true)
        .desc("Optional event information string to send for each fgs. Default is null")
        .required(false)
        .build();
    public static final String USAGE_DESCRIPTION =
        "A command-line tool to create events for profile ingester from a file containing fsg identifiers.";

    private static final Logger logger = LoggerFactory.getLogger(ProfileBatchImportCLI.class);

    @Override
    protected String getUsageDescription() {
        return USAGE_DESCRIPTION;
    }

    @Override
    protected void buildOptions() {
        serviceOptions.addOption(OPT_FILE);
        serviceOptions.addOption(OPT_EVENT_ACTION);
        serviceOptions.addOption(OPT_EVENT_INFORMATION);
        infoOptions.addOption(OPT_HELP);
    }

    public static void main(String[] arguments) throws Exception {
        ProfileBatchImportCLI profileBatchImport = new ProfileBatchImportCLI();
        CommandLine CLI = profileBatchImport.validateCLIArgument(arguments);

        PersonEventConnector connector = new PersonEventConnector();

        String filePath = CLI.getOptionValue("f");
        String action = CLI.getOptionValue("ea", PersonEventModel.ACTION_CREATE);
        String information = CLI.getOptionValue("ei");

        if (!PersonEventModel.AVAILABLE_ACTIONS.contains(action)) {
            throw new IllegalArgumentException("Invalid event action type :: " + action);
        }

        profileBatchImport.run(connector, filePath, action, information);
    }

    /**
     * Use provided file path to extract the fgs identifiers and create/push events for each one in RabbitMQ.
     * 
     * @param connector The RabbitMQ connection to push events to the queue.
     * @param filePath The path of the file containing the fgs identifiers.
     * @param action The action to use to craft the event.
     * @param info Any information to add to the event 'information' property.
     */
    public void run(PersonEventConnector connector, String filePath, String action, String info) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        logger.info("Found file, pushing entries to RabbitMQ...");

        String line;
        long pushed = 0;
        while ((line = reader.readLine()) != null) {
            PersonEventModel event = new PersonEventModel();
            event.setFgs(line);
            event.setAction(action);
            event.setInformation(info);
            try {
                connector.publishJSONMessage(event);
                pushed++;
            } catch (Exception e) {
                logger.error("Could not push event in rabbitMQ. " + event + " :"  + e.getLocalizedMessage());
            }
        }
        logger.info("Finished execution, pushed " + pushed + " events to RabbitMQ.");
        reader.close();
        connector.closeChannel();
    }
}
