/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.discovery.indexing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationAuthor;
import org.dspace.uclouvain.services.UCLouvainFWBValidationService;
import org.dspace.uclouvain.validation.fnrs.FNRSValidator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Solr indexer for {@class Publication} item.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class SolrServicePublicationIndexingPlugin
    extends SolrServiceUCLouvainIndexingPlugin
    implements SolrServiceIndexPlugin {

    private static final Logger log = LogManager.getLogger(SolrServicePublicationIndexingPlugin.class);

    @Autowired
    private UCLouvainFWBValidationService uclouvainFWBValidationService;
    @Autowired
    private FNRSValidator fnrsValidator;


    @Override
    @SuppressWarnings("rawtypes")
    public void additionalIndex(Context context, IndexableObject dso, SolrInputDocument document) {
        try {
            Publication publication = new Publication(getItem(dso));
            addFWBValidationKeys(context, publication.getItem(), document);
            addFNRSValidationKeys(publication.getItem(), document);
            addAuthorsIndexingKeys(publication, document);
        } catch (InvalidModelEntityTypeException e) {
            log.debug(e.getMessage());
        }

    }

    /**
     * Index 2 keys in the item document:
     * - 'fwbEligible_b': Is the item eligible based on FWB requirements.
     * - 'fwbCompliant_b': Is the item eligible based on FWB requirements.
     * @param context The current DSpace context.
     * @param item    The DSpace Item to process.
     * @param document The Solr document to add the keys to.
     */
    private void addFWBValidationKeys(Context context, Item item, SolrInputDocument document) {
        try {
            boolean isEligible = uclouvainFWBValidationService.isFWBEligible(context, item);
            boolean isCompliant = isEligible && uclouvainFWBValidationService.isFWBCompliantAsBoolean(context, item);
            document.addField("fwbEligible_b", isEligible);
            document.addField("fwbCompliant_b", isCompliant);
        } catch (Exception e) {
            log.error("Error while indexing FWB data in SOLR.", e);
        }
    }

    /**
     * Index 2 keys in the item document:
     * - 'fnrsRelevant_b': Is the item is relevant based on FNRS categories
     * - 'fnrsValid_b': Is the item is valid regarding FNRS rules (only present if item is relevant)
     * @param item The DSpace Item to process.
     * @param document The Solr document to add the keys to.
     */
    private void addFNRSValidationKeys(Item item, SolrInputDocument document) {
        boolean isRelevant = fnrsValidator.isRelevant(item);
        document.addField("fnrsRelevant_b", isRelevant);
        if (isRelevant) {
            document.addField("fnrsValid_b", fnrsValidator.isValid(item));
        }
    }

    /**
     * Index authors FGS identifier into a Solr item document (only for authors authority linked)
     * @param publication The {@class Publication} item to analyze
     * @param document The Solr document to add the keys to.
     */
    private void addAuthorsIndexingKeys(Publication publication, SolrInputDocument document) {
        List<String> authorsFgsIdentifier = publication.getAuthors().stream()
            .map(PublicationAuthor::getFgs)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(ArrayList::new)); // create a new mutable list. `.toList()` is immutable
        if (!authorsFgsIdentifier.isEmpty()) {
            document.addField("authors.identifier.fgs", authorsFgsIdentifier);
        }

    }
}
