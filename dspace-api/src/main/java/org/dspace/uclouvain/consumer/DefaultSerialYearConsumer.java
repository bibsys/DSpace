/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.consumer;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.core.model.publication.ArticlePublication;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationFactory;
import org.dspace.uclouvain.core.model.publication.SpeechPublication;

/**
 * When a publication date issued is provided, pre-fill the serial issued year if no value is present.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class DefaultSerialYearConsumer implements Consumer {

    private ItemService itemService;
    private MetadataField journalIssuedField;
    private Set<Publication> publicationsToProcess = new HashSet<>();

    private static final Logger logger = LogManager.getLogger(DefaultSerialYearConsumer.class);

    public void initialize() {
        itemService = ContentServiceFactory.getInstance().getItemService();
        journalIssuedField = new MetadataField(Publication.JOURNAL_DATE_ISSUED_FIELD);
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        Item item = (Item) event.getSubject(context);
        if (item == null || !isEventValid(context, event)) {
            return;
        }
        Publication publication = PublicationFactory.build(item);
        if (publication instanceof ArticlePublication ||
                (publication instanceof SpeechPublication speech && speech.publishedInSerial())) {
            publicationsToProcess.add(publication);
        }
    }

    @Override
    public void end(Context context) {
        if (publicationsToProcess.isEmpty()) {
            return;
        }
        for (Publication publication : publicationsToProcess) {
            Item item = publication.getItem();
            if (item == null) {
                continue;
            }
            // Prevent updating an already existing value.
            if (itemService.getMetadata(item, Publication.JOURNAL_DATE_ISSUED_FIELD) != null) {
                continue;
            }
            int issuedYear = publication.getIssuedYear();
            if (issuedYear == -1) { // if no issued year, exit.
                continue;
            }
            context.turnOffAuthorisationSystem();
            try {
                itemService.setMetadataSingleValue(
                    context,
                    item,
                    journalIssuedField,
                    null,
                    String.valueOf(issuedYear)
                );
                itemService.update(context, item);
            } catch (Exception e) {
                logger.warn("Could not update modified publication :: {}", item.getID().toString(), e);
            }
            context.restoreAuthSystemState();
        }
        publicationsToProcess.clear();
    }

    @Override
    public void finish(Context context) {
        // Nothing to do here.
    }

    private boolean isEventValid(Context context, Event event) throws SQLException {
        return event.getDetailsMetadata(".")
                .stream()
                .anyMatch(Publication.DATE_ISSUED_FIELD::equals);
    }
}
