/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.xmlworkflow.actions;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.processingaction.ProcessingAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;


/**
 * Method to clean the active change request field value when the user submits the item.
 * Before being deleted, the value can be stored in a request history field.
 * This is determined by a configuration property 'uclouvain.feature.send_back_to_submitter.store_reason'.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class UCLouvainThesisClearChangeRequestAction extends ProcessingAction {

    private Logger logger = LogManager.getLogger(UCLouvainThesisClearChangeRequestAction.class);

    private ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private MetadataField activeRequestField = new MetadataField(configService
            .getProperty("uclouvain.global.metadata.activerequestchange.field"));

    @Override
    public void activate(Context context, XmlWorkflowItem wfItem) {}

    @Override
    public ActionResult execute(Context context,  XmlWorkflowItem wfi, Step step, HttpServletRequest request) {
        Item item = wfi.getItem();
        try {
            // Retrieve the value of the active request field
            String value = this.itemService.getMetadataFirstValue(item, activeRequestField, Item.ANY);
            if (value != null) {
                this.itemService.clearMetadata(
                        context,
                        item,
                        activeRequestField.getSchema(),
                        activeRequestField.getElement(),
                        activeRequestField.getQualifier(),
                        Item.ANY
                );
            }
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        } catch (Exception e) {
            logger.error("An error occurred while clearing the active request change metadata field.", e);
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }

    }

    @Override
    public List<String> getOptions() {
        return new ArrayList<>();
    }
}
