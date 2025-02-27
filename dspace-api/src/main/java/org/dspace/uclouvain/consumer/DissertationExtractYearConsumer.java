/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.consumer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.core.utils.DateUtils;

/**
 * Consumer to extract the year from the defense date and put it into the default issue date of the dissertation.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class DissertationExtractYearConsumer implements Consumer {

    private Set<UUID> idsToProcess = new HashSet<>();
    private ItemService itemService;
    private MetadataField defenseDateField;
    private MetadataField dateIssuedField;
    private Logger logger;

    @Override
    public void initialize() throws Exception {
        itemService = ContentServiceFactory.getInstance().getItemService();
        defenseDateField = new MetadataField("dissertation.defenseDate");
        dateIssuedField = new MetadataField("dc.date.issued");
        logger = LogManager.getLogger(DissertationExtractYearConsumer.class);
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        // If event.detail is null, we cannot know which metadata was affected so we add the item to the list anyway.
        if (event.getDetail() == null || areDetailValid(event.getDetail())) {
            idsToProcess.add(event.getSubjectID());
        }
    }

    private boolean areDetailValid(String details) {
        return Arrays.stream(details.split(", "))
            .anyMatch(detail -> detail.equals(defenseDateField.getFullString("_")));
    }

    @Override
    public void end(Context context) throws Exception {
        for (UUID itemId: idsToProcess) {
            try {
                Item item = itemService.find(context, itemId);
                if (item == null) {
                    continue;
                }

                String defenseDate = itemService.getMetadataFirstValue(item, defenseDateField, null);
                String issueYear = itemService.getMetadataFirstValue(item, dateIssuedField, null);
                if (defenseDate != null) {
                    String defenseYear = Integer.toString(DateUtils.convertDSpaceDate(defenseDate).getYear());
                    if (!defenseYear.equals(issueYear)) {
                        // If the two values are different, override the metadata.
                        itemService.setMetadataSingleValue(
                            context, item, dateIssuedField, null, defenseYear
                        );
                    }
                } else if (issueYear != null) {
                    // Clear issue year
                    itemService.clearMetadata(
                        context,
                        item,
                        dateIssuedField.schema,
                        dateIssuedField.element,
                        dateIssuedField.qualifier,
                        null
                    );
                }
            } catch (Exception e) {
                logger.error(
                    "Could not extract defense date year for item with id: [" + itemId + "]", e
                );
                continue;
            }
        }
        // At the end of process, clear the set.
        idsToProcess.clear();
    }

    @Override
    public void finish(Context ctx) throws Exception {}
}
