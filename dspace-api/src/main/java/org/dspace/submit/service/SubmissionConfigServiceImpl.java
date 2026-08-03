/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.service;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.InitializingBean;

/**
 * An implementation for Submission Config service
 *
 * @author paulo.graca at fccn.pt
 */
public class SubmissionConfigServiceImpl implements SubmissionConfigService, InitializingBean {

    private Logger logger = LogManager.getLogger(SubmissionConfigServiceImpl.class);

    protected SubmissionConfigReader submissionConfigReader;

    public SubmissionConfigServiceImpl () throws SubmissionConfigReaderException {
        submissionConfigReader = new SubmissionConfigReader();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        submissionConfigReader.reload();
    }

    @Override
    public void reload() throws SubmissionConfigReaderException {
        submissionConfigReader.reload();
    }

    @Override
    public String getDefaultSubmissionConfigName() {
        return submissionConfigReader.getDefaultSubmissionConfigName();
    }

    @Override
    public List<SubmissionConfig> getAllSubmissionConfigs(Integer limit, Integer offset) {
        return submissionConfigReader.getAllSubmissionConfigs(limit, offset);
    }

    @Override
    public int countSubmissionConfigs() {
        return submissionConfigReader.countSubmissionConfigs();
    }

    @Override
    public SubmissionConfig getSubmissionConfigByCollection(Collection collection) {
        return submissionConfigReader.getSubmissionConfigByCollection(collection);
    }

    @Override
    public SubmissionConfig getSubmissionConfigByName(String submitName) {
        return submissionConfigReader.getSubmissionConfigByName(submitName);
    }

    /**
     * Get the submission config for a given workflow item.
     * This is determined by the collection's default submission config and the workflow step.
     * 
     * @param context The current DSpace context.
     * @param item The workflow item to get the submission config for.
     * @param collection The collection the workflow item belongs to.
     * @return the submission config for the given workflow item, or null if none.
     */
    @Override
    public SubmissionConfig getSubmissionConfigForWorkflowItem(Context context, Item item, Collection collection) {
        XmlWorkflowServiceFactory factory = XmlWorkflowServiceFactory.getInstance();
        try {
            XmlWorkflowItem wi = (XmlWorkflowItem) factory.getWorkflowItemService().findByItem(context, item);
            if (wi == null) {
                return null;
            }

            ClaimedTask ct = factory
                .getClaimedTaskService()
                .findByWorkflowIdAndEPerson(context, wi, context.getCurrentUser());
            if (ct == null) {
                return null;
            }
            // Get default submission form for the collection.
            String defaultSubmission = getSubmissionConfigByCollection(collection).getSubmissionName();
            // Get full workflow step name.
            String stepName = ct.getStepID();

            // Try to find a specific submission form.
            return getSubmissionConfigByName(defaultSubmission + "-workflow-" +  stepName);
        } catch (Exception e) {
            logger.debug(
                "Could not retrieve the submission config for the given item :: %s, exception was :: %s".formatted(
                    item.getID(), e.getMessage()
                ));
            return null;
        }
    }

    @Override
    public SubmissionStepConfig getStepConfig(String stepID) throws SubmissionConfigReaderException {
        return submissionConfigReader.getStepConfig(stepID);
    }

    @Override
    public List<Collection> getCollectionsBySubmissionConfig(Context context, String submitName)
            throws IllegalStateException, SQLException {
        return submissionConfigReader.getCollectionsBySubmissionConfig(context, submitName);
    }

    public SubmissionConfig getCorrectionSubmissionConfigByCollection(Collection collection) {
        return submissionConfigReader.getCorrectionSubmissionConfigByCollection(collection);
    }

}
