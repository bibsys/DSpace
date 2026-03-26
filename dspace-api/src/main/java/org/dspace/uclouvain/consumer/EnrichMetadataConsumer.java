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
import java.util.stream.Collectors;

import org.dspace.content.Item;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.submit.listener.MetadataListener;
import org.dspace.utils.DSpace;

/**
 * The main goal of this consumer is to add additional metadata to a freshly created Item which
 * would already have some metadata.
 * The intent is to enrich the metadata of the object with some external source (Crossref for example).
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class EnrichMetadataConsumer implements Consumer {

    private MetadataListener listener;
    private ItemService itemService;

    private Set<UUID> itemsToEnrich = new HashSet<>();

    @Override
    public void initialize() throws Exception {
        listener = new DSpace().getSingletonService(MetadataListener.class);
        itemService = ContentServiceFactory.getInstance().getItemService();
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        if (event.getSubjectType() != Constants.ITEM) {
            return;
        }
        Item item = (Item) event.getSubject(context);
        if (item != null) {
            itemsToEnrich.add(item.getID());
        }
    }

    @Override
    public void end(Context context) throws Exception {
        for (UUID uuid: itemsToEnrich) {
            Item item = itemService.find(context, uuid);

            Set<String> existingFields = item.getMetadata()
                .stream()
                .map(metadata -> metadata.getMetadataField().toString('.'))
                .distinct()
                .collect(Collectors.toSet());
            if (existingFields.isEmpty()) {
                // If no fields are present in the item, we can just exit here.
                continue;
            }

            Set<String> externalMetadata = listener.getMetadataToListen()
                .stream()
                .filter(listenerMetadata -> existingFields.contains(listenerMetadata))
                .collect(Collectors.toSet());
            if (externalMetadata.isEmpty()) {
                continue;
            }

            ExternalDataObject externalObject = listener.getExternalDataObject(context, item, externalMetadata);
            if (externalObject != null) {
                for (MetadataValueDTO metadata : externalObject.getMetadata()) {
                    if (!existingFields.contains(metadata.getMetadataField())) {
                        itemService.addMetadata(
                            context, item,
                            metadata.getSchema(), metadata.getElement(), metadata.getQualifier(),
                            null,
                            metadata.getValue(),
                            metadata.getAuthority(), metadata.getConfidence()
                        );
                    }
                }
            }
        }
        itemsToEnrich.clear();
    }

    @Override
    public void finish(Context context) throws Exception {
    }
}
