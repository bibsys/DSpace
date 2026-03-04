/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.discovery.indexing;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.uclouvain.core.model.OrgUnit;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationAuthor;
import org.dspace.uclouvain.core.model.publication.PublicationEntity;
import org.dspace.uclouvain.core.model.publication.PublicationFactory;
import org.dspace.uclouvain.validation.fnrs.FNRSValidator;
import org.dspace.util.UUIDUtils;
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
    private FNRSValidator fnrsValidator;
    @Autowired
    private ItemService itemService;
    @Autowired
    private EPersonService ePersonService;

    @Override
    @SuppressWarnings("rawtypes")
    public void additionalIndex(Context context, IndexableObject dso, SolrInputDocument document) {
        try {
            Publication publication = PublicationFactory.build(getItem(dso));
            addFWBValidationKeys(context, publication.getItem(), document);
            addFNRSValidationKeys(publication.getItem(), document);
            addAncestorEntities(publication, document);
            authorFgsIndexing(publication, document);
            readPermissionsIndexing(context, publication, document);
        } catch (InvalidModelEntityTypeException e) {
            log.debug(e.getMessage());
        }

    }

    /**
     * Index 2 keys in the item document:
     * - 'fwbCompliant_b': Is the item compliant for Fulltext including based on FWB requirements.
     * - 'fwbExportable_b': Is the item exportable into a FWB bibliography.
     *
     * @param context The current DSpace context.
     * @param item    The DSpace Item to process.
     * @param document The Solr document to add the keys to.
     */
    private void addFWBValidationKeys(Context context, Item item, SolrInputDocument document) {
        try {
            Publication publication = PublicationFactory.build(item);
            document.addField("fwbCompliant_b", publication.isFWBCompliant(context).getLeft());
            document.addField("fwbExportable_b", publication.isFWBExportable(context));
        } catch (Exception e) {
            log.error("Error while indexing FWB data in SOLR.", e);
        }
    }

    /**
     * Index 2 keys in the item document:
     * - 'fnrsRelevant_b': Is the item is relevant based on FNRS categories
     * - 'fnrsValid_b': Is the item is valid regarding FNRS rules (only present if item is relevant)
     *
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
     * Index all entities linked to an existing {@link org.dspace.uclouvain.core.model.OrgUnit} and their ancestors.
     *
     * @param publication the publication to analyze.
     * @param document The Solr document to add the keys to.
     */
    private void addAncestorEntities(Publication publication, SolrInputDocument document) {
        Set<UUID> ancestorUUIDs = new HashSet<>();
        publication.getEntities().stream()
            .filter(PublicationEntity::hasAuthority)
            .map(PublicationEntity::getAuthority)
            .forEach(entity -> {
                OrgUnit current = entity;
                while (current != null) {
                    // .add() return false if UUID is already present into the set.
                    // This protects against infinite parenthood loops.
                    if (!ancestorUUIDs.add(current.getID())) {
                        break;
                    }
                    current = current.getParent();
                }
            });
        if (!ancestorUUIDs.isEmpty()) {
            document.addField("hierarchical_entity_authority", ancestorUUIDs.stream().map(UUID::toString).toList());
        }
    }

    /**
     * Index authors FGS identifier into a Solr item document (only for authors authority linked)
     * @param publication The {@class Publication} item to analyze
     * @param document The Solr document to add the keys to.
     */
    private void authorFgsIndexing(Publication publication, SolrInputDocument document) {
        List<String> authorsFgsIdentifier = publication.getAuthors().stream()
            .map(PublicationAuthor::getFgs)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(ArrayList::new)); // create a new mutable list. `.toList()` is immutable
        if (!authorsFgsIdentifier.isEmpty()) {
            document.addField("authors.identifier.fgs", authorsFgsIdentifier);
        }
    }

    /**
     * Index read permissions for the publication in the solr document.
     * This adds a read permission for the submitter of the publication and for all authors of the publication.
     * @param context The current DSpace application context.
     * @param publication The publication to add read permission to.
     * @param document The solr document to add the read permissions to.
     */
    private void readPermissionsIndexing(Context context, Publication publication, SolrInputDocument document) {
        // Add read permission for submitter
        addRead(document, Optional.ofNullable(publication.getItem().getSubmitter()));
        // Add read permission for any author
        publication.getAuthors().stream()
            .map(PublicationAuthor::getAuthority)
            .filter(Objects::nonNull)
            .forEach((author) -> {
                addRead(document, findOwner(context, author.getItem()));
            });
    }

    /**
     * Add a read permission to the document for the given person.
     * @param document The solr Document.
     * @param person The person to add a read permission for.
     */
    private void addRead(SolrInputDocument document, Optional<EPerson> person) {
        person.ifPresent(personObj -> document.addField("read", "e" + personObj.getID().toString()));
    }

    /**
     * Find the owner of a given item and return it as a EPerson.
     * @param context The current DSpace application context.
     * @param profile The profile item to find the owner of.
     * @return An Optional containing the EPerson owner of the item, or an empty Optional if
     * no owner is found or if an error occurs.
     */
    private Optional<EPerson> findOwner(Context context, Item profile) {
        return itemService.getMetadata(profile, "dspace", "object", "owner", Item.ANY)
            .stream()
            .findFirst()
            .map(MetadataValue::getAuthority)
            .filter(StringUtils::isNotEmpty)
            // Check uuid conversion since uuid is a string and could be other things than an actual uuid.
            .map(UUIDUtils::fromString)
            .filter(Objects::nonNull)
            .map(uuid -> {
                try {
                    return Optional.ofNullable(ePersonService.find(context, uuid));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            })
            .orElse(Optional.empty());
    }
}
