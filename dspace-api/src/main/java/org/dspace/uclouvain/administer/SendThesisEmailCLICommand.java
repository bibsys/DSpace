/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.administer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.mails.ThesisAuthorAttestationEmail;
import org.dspace.uclouvain.core.mails.ThesisSupervisorAttestationEmail;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;

public class SendThesisEmailCLICommand extends AbstractCLICommand {
    public static final String USAGE_DESCRIPTION =
        "A command-line tool to send the attestation emails for a specific thesis";

    private final Logger logger = LogManager.getLogger(SendThesisEmailCLICommand.class);

    private static final Option OPT_WORKFLOW_ITEM = Option.builder("w")
        .longOpt("workflowItem")
        .type(Integer.class)
        .hasArg(true)
        .desc("Id of a workflow item")
        .required(true)
        .build();

    private final Context context;
    private XmlWorkflowItemService workflowItemService;

    protected SendThesisEmailCLICommand() {
        context = new Context();
        workflowItemService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowItemService();
    }

    public static void main(String[] argv) throws Exception {
        SendThesisEmailCLICommand cli = new SendThesisEmailCLICommand();
        CommandLine cl = cli.validateCLIArgument(argv);
        cli.run(cl.getParsedOptionValue("w"));
    }

    protected void buildOptions() {
        serviceOptions.addOption(OPT_WORKFLOW_ITEM);
        infoOptions.addOption(OPT_HELP);
    }

    protected String getUsageDescription() {
        return USAGE_DESCRIPTION;
    }

    private void run(int workflowId) {
        try {
            XmlWorkflowItem wi = workflowItemService.find(context, workflowId);
            if (wi == null) {
                throw new Exception("Could not find workflow item to send attestation email");
            }
            Item item = wi.getItem();
            if (item == null) {
                throw new Exception("Could not find item to send attestation email");
            }

            new ThesisAuthorAttestationEmail(context, item).sendEmail();

            new ThesisSupervisorAttestationEmail(context, item).sendEmail();
        } catch (Exception e) {
            logger.error("Could not send attestation email :: " + e.getMessage(), e);
            System.err.println("Could not send attestation email :: " + e.getMessage());
        }
    }
}
