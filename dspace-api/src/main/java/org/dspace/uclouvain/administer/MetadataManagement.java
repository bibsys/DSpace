package org.dspace.uclouvain.administer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.dspace.app.util.DSpaceObjectUtilsImpl;
import org.dspace.app.util.service.DSpaceObjectUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * A command-line tool for manager item metadata.
 * With this command, we can set, add or remove any item metadata.
 *
 * USAGE:
 *   dspace dsrun org.dspace.uclouvain.administer.MetadataManagement -t [objUUID] -a [set|add|delete] -f [schema.element.qualifier] -v [value]
 *
 * ARGUMENTS:
 *   -t, --target: the item uuid that will be affected.
 *   -a, --action: [set] Remove all existing sibling metadata and set a new one.
 *                 [add] Add a new metadata without removing siblings.
 *                 [delete] Remove all existing sibling metadata.
 *   -f, --field:  The metadata field to manage (schema.element[.qualifier])
 *   -v, --value:  The value to add|set (not used for remove action)
 *   -h, --help:   display metadata management options.
 *
 * @author Renaud Michotte <renaud.michotte@uclouvain.be>
 * @version $Revision$
 */
public class MetadataManagement extends AbstractCLICommand {

    // CLASS CONSTANTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /** CLI available options */
    private static final Option OPT_TARGET = Option.builder("t")
            .longOpt("target")
            .hasArg(true)
            .desc("the targeted object UUID")
            .required(true)
            .build();
    private static final Option OPT_ACTION = Option.builder("a")
            .longOpt("action")
            .hasArg(true)
            .desc("the action to applied on the object")
            .required(true)
            .build();
    private static final Option OPT_FIELD = Option.builder("f")
            .longOpt("field")
            .hasArg(true)
            .desc("the affected metadata field")
            .required(true)
            .build();
    private static final Option OPT_VALUE = Option.builder("v")
            .longOpt("value")
            .hasArg(true)
            .desc("the field value")
            .required(false)
            .build();

    public static final String USAGE_DESCRIPTION = "A command-line tool for managing object metadata";
    public static final String ACTION_ADD = "add";
    public static final String ACTION_SET = "set";
    public static final String ACTION_DELETE = "delete";

    // CLASS ATTRIBUTES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private final Context context;
    protected DSpaceObjectUtils dsoUtils;

    // CONSTRUCTOR & MAIN ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /** constructor, which just creates an object with a ready context. */
    protected MetadataManagement() {
        context = new Context();
        this.dsoUtils = new DSpace().getServiceManager()
                .getServiceByName(DSpaceObjectUtilsImpl.class.getName(), DSpaceObjectUtilsImpl.class);
    }

    /**
     * For invoking via the command line.
     *
     * @param argv: the command line arguments given
     * @throws MissingArgumentException : If a required argument is missing.
     */
    public static void main(String[] argv) throws Exception {
        MetadataManagement cli = new MetadataManagement();
        CommandLine cl = cli.validateCLIArgument(argv);
        cli.run(cl.getOptionValue('t'), cl.getOptionValue('a'), cl.getOptionValue('f'), cl.getOptionValue('v'));
    }

    protected void buildOptions() {
        serviceOptions.addOption(OPT_TARGET);
        serviceOptions.addOption(OPT_ACTION);
        serviceOptions.addOption(OPT_FIELD);
        serviceOptions.addOption(OPT_VALUE);
        infoOptions.addOption(OPT_HELP);
    }
    protected String getUsageDescription() {
        return USAGE_DESCRIPTION;
    }

    // PRIVATE FUNCTIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void run (String targetUUID, String action, String fieldName, String fieldValue) throws SQLException {
        context.turnOffAuthorisationSystem();
        UUID uuid = UUID.fromString(targetUUID);
        DSpaceObject dso = dsoUtils.findDSpaceObject(context, uuid);
        if (dso == null) {
            throw new IllegalArgumentException("object "+uuid+" not found!");
        }
        MetadataFieldName field = new MetadataFieldName(fieldName);
        switch (action) {
            case ACTION_ADD: addMetadata(dso, field, fieldValue); break;
            case ACTION_SET: setMetadata(dso, field, fieldValue); break;
            case ACTION_DELETE: delMetadata(dso, field); break;
            default: throw new IllegalArgumentException("invalid action");
        }
        context.complete();
    }


    private void setMetadata(DSpaceObject dso, MetadataFieldName field, String fieldValue) throws SQLException {
        this.delMetadata(dso, field);
        this.addMetadata(dso, field, fieldValue);
    }

    private void delMetadata(DSpaceObject dso, MetadataFieldName field) throws SQLException {
        DSpaceObjectService<DSpaceObject>  dsoService = ContentServiceFactory.getInstance().getDSpaceObjectService(dso);
        List<MetadataValue> metadataValues = dsoService.getMetadataByMetadataString(dso, field.toString());
        dsoService.removeMetadataValues(this.context, dso, metadataValues);
    }

    private void addMetadata(DSpaceObject dso, MetadataFieldName field, String fieldValue) throws SQLException {
        DSpaceObjectService<DSpaceObject> dsoService = ContentServiceFactory.getInstance().getDSpaceObjectService(dso);
        dsoService.addMetadata(this.context, dso, field.schema, field.element, field.qualifier, null, fieldValue);
    }

}
