/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.xmlworkflow.actions;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choices;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.processingaction.ProcessingAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

public class UCLouvainAnonymizeAuthorsAction extends ProcessingAction {

    private Logger logger = LogManager.getLogger(UCLouvainThesisClearChangeRequestAction.class);

    private final ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private final MetadataField anonymizeAuthorField = new MetadataField(configService.getProperty(
        "uclouvain.global.metadata.authoranonymize.field"));
    private final MetadataField authorNameField = new MetadataField(configService.getProperty(
        "uclouvain.global.metadata.authorname.field"));

    private final String ANONYMOUS_PLACEHOLDER = "Anonymous";

    @Override
    public void activate(Context context, XmlWorkflowItem wf) {}

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem wfi, Step step, HttpServletRequest request) {
        Item item = wfi.getItem();
        List<MetadataValue> anonymizeList =
            itemService.getMetadataByMetadataString(item, anonymizeAuthorField.toString());
        if (anonymizeList.isEmpty()) {
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        }
        for (MetadataValue mv : anonymizeList) {
            if (!"true".equalsIgnoreCase(mv.getValue())) {
                continue;
            }
            int place = mv.getPlace();
            try {
                itemService.replaceMetadata(
                    context, item,
                    authorNameField.getSchema(), authorNameField.getElement(), authorNameField.getQualifier(),
                    null, ANONYMOUS_PLACEHOLDER, null, Choices.CF_UNSET, place
                );
            } catch (SQLException e) {
                logger.error("Could not anonymize author for item {} at place {}", item.getID(), place);
                return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
            }
        }
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    public List<String> getOptions() {
        return new ArrayList<>();
    }
}
