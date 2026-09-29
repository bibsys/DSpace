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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.submit.listener.ExternalIdGenerator;
import org.dspace.submit.listener.MetadataListener;
import org.dspace.submit.listener.SimpleMetadataListener;
import org.dspace.uclouvain.content.enrichment.ItemEnrichment.Status;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.ItemEnrichmentService;
import org.dspace.utils.DSpace;

/**
 * Enrich the metadata of an item from an external source (CrossRef, PubMed, arXiv...) as soon as it carries one of
 * the identifiers listened by the {@link SimpleMetadataListener} (DOI, PMID...). Only the metadata the item does
 * not have yet are added.
 *
 * Every attempt is recorded by the {@link ItemEnrichmentService}, so that a source is queried once per identifier
 * value: a SUCCESS or NOT_FOUND answer is final for that value, an ERROR is retried after a configurable delay.
 * This consumer runs inside the {@code context.commit()} of the caller, which may be the enhancer poller thread:
 * it only ever uses the Context it is given.
 *
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class EnrichMetadataConsumer implements Consumer {

    private static final Logger log = LogManager.getLogger(EnrichMetadataConsumer.class);

    private SimpleMetadataListener listener;
    private ItemService itemService;
    private ItemEnrichmentService enrichmentService;

    private final Set<UUID> itemsToEnrich = new HashSet<>();

    @Override
    public void initialize() throws Exception {
        listener = new DSpace()
            .getServiceManager()
            .getServiceByName(MetadataListener.class.getName(), SimpleMetadataListener.class);
        itemService = ContentServiceFactory.getInstance().getItemService();
        enrichmentService = UCLouvainServiceFactory.getInstance().getItemEnrichmentService();
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
        try {
            for (UUID uuid : itemsToEnrich) {
                Item item = itemService.find(context, uuid);
                if (item != null) {
                    enrich(context, item);
                }
            }
        } finally {
            // Whatever happened, never carry items over to the next commit handled by this (pooled) consumer.
            itemsToEnrich.clear();
        }
    }

    @Override
    public void finish(Context context) throws Exception {
    }

    private void enrich(Context context, Item item) throws Exception {
        Set<String> existingFields = item.getMetadata()
            .stream()
            .map(metadata -> metadata.getMetadataField().toString('.'))
            .collect(Collectors.toSet());

        for (String field : listener.getMetadataToListen()) {
            if (!existingFields.contains(field)) {
                continue;
            }
            for (ExternalDataProvider provider : listener.getExternalDataProvidersMap().get(field)) {
                String identifier = generateExternalId(context, provider, item, field);
                if (StringUtils.isBlank(identifier)
                    || !enrichmentService.shouldAttempt(context, item, provider.getSourceIdentifier(), identifier)) {
                    continue;
                }
                ExternalDataObject result = query(context, item, provider, field, identifier);
                if (result != null) {
                    addMissingMetadata(context, item, existingFields, result);
                }
            }
        }
    }

    /**
     * Query the provider and record the attempt.
     *
     * @return the external data, or null when the source does not know the identifier or is unavailable.
     */
    private ExternalDataObject query(Context context, Item item, ExternalDataProvider provider, String field,
        String identifier) throws Exception {
        String source = provider.getSourceIdentifier();
        ExternalDataObject result = null;
        Status status;
        String reason = null;
        try {
            result = provider.getExternalDataObject(context, identifier)
                .filter(data -> !data.getMetadata().isEmpty())
                .orElse(null);
            status = result != null ? Status.SUCCESS : Status.NOT_FOUND;
        } catch (RuntimeException e) {
            // ExternalSourceException from the UCLouvain providers, a wrapped MetadataSourceException from the
            // upstream ones: either way the source did not answer, the attempt is recorded and the others go on.
            status = Status.ERROR;
            reason = e.getMessage();
            log.warn("Could not enrich item {} from {} with {} '{}': {}", item.getID(), source, field, identifier,
                e.getMessage(), e);
        }
        enrichmentService.record(context, item, source, field, identifier, status, reason);
        return result;
    }

    private void addMissingMetadata(Context context, Item item, Set<String> existingFields,
        ExternalDataObject result) throws Exception {
        for (MetadataValueDTO metadata : result.getMetadata()) {
            if (existingFields.contains(metadata.getMetadataField())) {
                continue;
            }
            itemService.addMetadata(
                context, item,
                metadata.getSchema(), metadata.getElement(), metadata.getQualifier(),
                null,
                metadata.getValue(),
                metadata.getAuthority(), metadata.getConfidence()
            );
        }
        // A field added by this source is now present for the next one.
        result.getMetadata().forEach(metadata -> existingFields.add(metadata.getMetadataField()));
    }

    /** Same identifier generation as {@link SimpleMetadataListener}: the first generator supporting the provider. */
    private String generateExternalId(Context context, ExternalDataProvider provider, Item item, String field) {
        for (ExternalIdGenerator generator : listener.getGenerators()) {
            if (generator.support(provider)) {
                return generator.generateExternalId(context, provider, item, field);
            }
        }
        return null;
    }
}
