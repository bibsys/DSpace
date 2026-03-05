/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.consumer;

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
import org.dspace.uclouvain.core.model.publication.DissertationPublication;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationFactory;

/**
 * Consumer to extract the year from the defense date and put it into the default issue date of the dissertation.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class DissertationExtractYearConsumer implements Consumer {

    private Set<UUID> idsToProcess = new HashSet<>();
    private ItemService itemService;
    private MetadataField dateIssuedField;
    private Logger logger;

    @Override
    public void initialize() throws Exception {
        itemService = ContentServiceFactory.getInstance().getItemService();
        dateIssuedField = new MetadataField(Publication.DATE_ISSUED_FIELD);
        logger = LogManager.getLogger(DissertationExtractYearConsumer.class);
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        // If event.detail is null, we cannot know which metadata was affected so we add the item to the list anyway.
        if (areDetailsValid(event)) {
            idsToProcess.add(event.getSubjectID());
        }
    }

    private boolean areDetailsValid(Event event) {
        return event.getDetail() == null && event.getDetailsMetadata(".").stream()
            .anyMatch(detail -> detail.equals(Publication.DEFENSE_DATE_FIELD));
    }

    @Override
    public void end(Context context) throws Exception {
        for (UUID itemId: idsToProcess) {
            try {
                Item item = itemService.find(context, itemId);
                if (item == null) {
                    continue;
                }
                // Make sure that the publication type is 'text::thesis' before doing anything.
                Publication publication = PublicationFactory.build(item);
                if (!(publication instanceof DissertationPublication dissertationPublication)) {
                    continue;
                }
                int defenseYear = dissertationPublication.getDefenseDateYear();
                int issueYear = publication.getIssuedYear();
                if (defenseYear != -1) {
                    if (defenseYear != issueYear) { // If the two values are different, override the metadata.
                        itemService.setMetadataSingleValue(
                            context,
                            item,
                            dateIssuedField,
                            null,
                            String.valueOf(defenseYear)
                        );
                    }
                } else if (issueYear != -1) { // Clear issue year
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
                logger.error("Could not extract defense date year for item with id: [{}]", itemId, e);
            }
        }
        // At the end of process, clear the set.
        idsToProcess.clear();
    }

    @Override
    public void finish(Context ctx) throws Exception {}
}
