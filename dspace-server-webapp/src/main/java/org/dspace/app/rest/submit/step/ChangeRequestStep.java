package org.dspace.app.rest.submit.step;

import static org.apache.commons.lang.StringUtils.isEmpty;

import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.DataChangeRequest;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.model.MetadataField;

/**
 * Section step class for the Change Request section. It searches for a change request value and returns it in a DataChangeRequest form.
 * 
 * @Authored: Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class ChangeRequestStep extends AbstractProcessingStep {

    private ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    // Active Request Field
    private MetadataField activeRF = new MetadataField(configService.getProperty("uclouvain.global.metadata.activerequestchange.field"));

    @Override
    public DataChangeRequest getData(SubmissionService submissionService, InProgressSubmission obj, SubmissionStepConfig config) throws Exception {
        DataChangeRequest result = new DataChangeRequest();
        Item item = obj.getItem();
        ItemService itemService = obj.getItem().getItemService();
        // Get the change request metadata value from the item.
        String requiredChanges = itemService.getMetadataFirstValue(item, activeRF, null);
        if (!isEmpty(requiredChanges)) {
            result.setChangeData(requiredChanges);
        }
        return result;
    }

    // Empty method since no specific operation can be done with the ChangeRequestStep.
    @Override
    public void doPatchProcessing(Context context, HttpServletRequest currentRequest, InProgressSubmission source, Operation op, SubmissionStepConfig stepConf) throws Exception {}
}
