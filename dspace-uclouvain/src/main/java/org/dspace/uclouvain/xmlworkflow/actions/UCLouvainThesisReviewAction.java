package org.dspace.uclouvain.xmlworkflow.actions;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.processingaction.ReviewAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.WorkflowItemRoleService;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Custom review action for master theses.
 * This Action contains 3 outputs: 'Accepted', 'Accepted without diffusion' and 'Rejected'.
 * In the case 'Accepted', we continue the workflow;
 * In the case 'Accepted without diffusion', same as 'Accepted' but we restrict bitstream && add a message to the metadata;
 * In the case 'Rejected', we change the state of the workflow item to 'Withdrawn' && we add it to archive;
 *
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 * @version $Revision$
 */
public class UCLouvainThesisReviewAction extends ReviewAction {

    private static final String SUBMITTER_IS_DELETED_PAGE = "submitter_deleted";

    private static final String SUBMIT_APPROVE_WITHOUT_DIFFUSION = "submit_approve_no_diffusion";

    @Autowired(required = true)
    protected WorkflowItemRoleService workflowItemRoleService;
    @Autowired(required = true)
    protected InstallItemService installItemService;
    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private BitstreamService bitstreamService;
    @Autowired
    private ItemService itemService;

    /**
     * Method executed to map each option to a specific action.
     * The option is extracted form the incoming request by splitting the submit button name.
     */
    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {
        if (super.isOptionInParam(request)) {
            switch (Util.getSubmitButton(request, SUBMIT_CANCEL)) {
                case SUBMIT_APPROVE:
                    return processAccept(c, wfi);
                case SUBMIT_APPROVE_WITHOUT_DIFFUSION:
                    return this.processAcceptWithoutDiffusion(c, wfi, request);
                case SUBMIT_REJECT:
                    return this.processRejectPage(c, wfi, request);
                case SUBMITTER_IS_DELETED_PAGE:
                    return processSubmitterIsDeletedPage(c, wfi, request);
                default:
                    return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
            }
        }
        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }

    /**
     * Get the list of options available for this action.
     * This is also used for rendering buttons on the frontend.
     */
    @Override
    public List<String> getOptions() {
        List<String> options = new ArrayList<String>();
        options.add(SUBMIT_APPROVE);
        options.add(SUBMIT_APPROVE_WITHOUT_DIFFUSION);
        options.add(SUBMIT_REJECT);
        options.add(RETURN_TO_POOL);
        return options;
    }

    /**
     * Process result when option 'SUBMIT_APPROVE_WITHOUT_DIFFUSION' is selected.
     * - First delete all bitstream restriction and add only administrator access.
     * - Add a new tag to keep a trace of the operation in the 'dc.description.X' metadata field.
     * If reason is not given => error
     */
    public ActionResult processAcceptWithoutDiffusion(Context ctx, XmlWorkflowItem wfi, HttpServletRequest request) throws SQLException, AuthorizeException {
        Item currentItem = wfi.getItem();
        String reason = request.getParameter(REJECT_REASON);

        // 1. Change bitstreams access to admin only
        Group adminGroup = this.groupService.findByName(ctx, "Administrator");
        if (adminGroup != null){
            // For all bitstream of the bundle 'ORIGINAL' replace the policies to 'ADMIN ONLY'
            for (Bitstream bitstream: this.bitstreamService.getBitstreamByBundleName(currentItem, "ORIGINAL")){
                this.restrictBitstream(ctx, bitstream, adminGroup);
            }
        }

        // 2. Add provenance && a custom message tag for the item in "dc.description.diffusion"
        this.addProvenance(ctx, currentItem, "Approved with no diffusion for entry into archive by");
        this.itemService.addMetadata(ctx, currentItem, "dc", "description", "diffusion", null, reason);
        this.itemService.update(ctx, currentItem);

        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    /**
     * Process result when option 'SUBMIT_REJECT' is selected.
     * - Archive the item.
     * - Once archived, withdrawn it.
     * If reason is not given => error
     */
    @Override
    public ActionResult processRejectPage(Context c, XmlWorkflowItem wfi, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        String reason = request.getParameter(REJECT_REASON);
        if (reason == null || reason.isBlank()) {
            addErrorField(request, REJECT_REASON);
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }

        this.addProvenance(c, wfi.getItem(), "Rejected for entry into archive by");
        
        c.turnOffAuthorisationSystem();
        // Archive the item, then instantly withdraw it 
        Item archivedItem = this.archive(c, wfi);
        this.itemService.withdraw(c, archivedItem);
        c.restoreAuthSystemState();

        return new ActionResult(ActionResult.TYPE.TYPE_PAGE);
    }

    /**
     * Used to archive an item and remove all metadata related to the workflow.
     * @param context: The current DSpace context.
     * @param wfi: The workflow item to archive.
     * @return: The archived item.
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    private Item archive(Context context, XmlWorkflowItem wfi) throws SQLException, IOException, AuthorizeException {
        Item item = wfi.getItem();

        workflowItemRoleService.deleteForWorkflowItem(context, wfi);
        installItemService.installItem(context, wfi);
        this.itemService.clearMetadata(context, item, WorkflowRequirementsService.WORKFLOW_SCHEMA, Item.ANY, Item.ANY, Item.ANY);
        this.itemService.update(context, item);

        return item;
    }

    /**
     * Take a bitstream and restricts the access to the administrator group only.
     * @param ctx: The current DSpace context.
     * @param bs: The bitstream to restrict.
     * @param adminGroup: The administrator group to grant read rights to.
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void restrictBitstream(Context ctx, Bitstream bs, Group adminGroup) throws SQLException, AuthorizeException {
        authorizeService.removeAllPolicies(ctx, bs);
        authorizeService.addPolicy(ctx, bs, Constants.READ, adminGroup, ResourcePolicy.TYPE_CUSTOM);
    }

    /**
     * Add provenance information to the item using a custom message.
     * @param ctx: The current DSpace context.
     * @param item: The item to which the provenance information will be added to.
     * @param message: The custom message to be added to the provenance information.
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void addProvenance(Context ctx, Item item, String message) throws SQLException, AuthorizeException {
        // Retrieve current datetime
        String now = DCDate.getCurrent().toString();

        // Get user's name + email address
        String usersName =
            XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService().getEPersonName(ctx.getCurrentUser());

        String provDescription = getProvenanceStartId() + " " + message + " " + usersName + " on "
            + now + " (GMT) ";

        // Add provenance info in the 'dc.description.provenance' field
        ctx.turnOffAuthorisationSystem();
        this.itemService.addMetadata(ctx, item, MetadataSchemaEnum.DC.getName(), "description", "provenance", "en",
            provDescription);
        this.itemService.update(ctx, item);
        ctx.restoreAuthSystemState();
    } 
}
