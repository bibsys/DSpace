/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.uclouvain.core.model.OrgUnit;
import org.dspace.uclouvain.discovery.indexing.SolrServicePublicationOrgUnitIndexingPlugin;
import org.dspace.uclouvain.services.OrgUnitService;
import org.springframework.beans.factory.annotation.Autowired;

public class OrgUnitServiceImpl implements OrgUnitService {

    @Autowired
    SearchService searchService;

    @Override
    public OrgUnit findByName(
            Context context,
            String instAcronym,
            String instName,
            String entityAcronym,
            String entityName
    ) {
        // BUILD SOLR QUERY
        List<String> clauses = new ArrayList<>();
        if (!StringUtils.isBlank(entityAcronym) || !StringUtils.isBlank(entityName)) {
            if (!StringUtils.isBlank(entityAcronym)) {
                clauses.add("%s:\"%s\"".formatted(OrgUnit.ACRONYM_FIELD, entityAcronym));
            }
            if (!StringUtils.isBlank(entityName)) {
                clauses.add("%s:\"%s\"".formatted(OrgUnit.TITLE_FIELD, entityName));
            }
            if (!StringUtils.isBlank(instAcronym)) {
                clauses.add("%s:\"%s\"".formatted(
                        SolrServicePublicationOrgUnitIndexingPlugin.PARENT_UNIVERSITY_ACRONYM_KEY, instAcronym));
            }
            if (!StringUtils.isBlank(instName)) {
                clauses.add("%s:\"%s\"".formatted(
                        SolrServicePublicationOrgUnitIndexingPlugin.PARENT_UNIVERSITY_NAME_KEY, instName));
            }
        } else {
            if (!StringUtils.isBlank(instAcronym)) {
                clauses.add("%s:\"%s\"".formatted(OrgUnit.ACRONYM_FIELD, instAcronym));
            }
            if (!StringUtils.isBlank(instName)) {
                clauses.add("%s:\"%s\"".formatted(OrgUnit.TITLE_FIELD, instName));
            }
        }
        if (clauses.isEmpty()) {
            throw new IllegalArgumentException("At least one search criteria is required");
        }
        String query = String.join(" AND ", clauses);

        // EXECUTE SOLR QUERY
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.addFilterQueries("dspace.entity.type:" + OrgUnit.ENTITY_TYPE);
        discoverQuery.setMaxResults(1);
        discoverQuery.setQuery(query);
        try {
            DiscoverResult result = searchService.search(context, discoverQuery);
            return result.getIndexableObjects()
                .stream()
                .map(indexableObject -> buildOrgUnit(((IndexableItem) indexableObject).getIndexedObject()))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        } catch (SearchServiceException sse) {
            return null;
        }
    }
    private OrgUnit buildOrgUnit(Item item) {
        try {
            return new OrgUnit(item);
        } catch (Exception ignored) {
            return null;
        }
    }

    public List<OrgUnit> findByName(Context context, List<String> affiliations) {
        // We have a list of strings that represent affiliations:
        // - ex: ["SSS/SIONS", "SSS/IONS/CEMO", "SSS/MEDE", "StLuc"]
        // We want to split each string based on the '/' character, so we will have a list of arrays.
        // Then get the largest list (beginning with size 3) and see if we can find a matching OrgUnit acronym.
        return affiliations.stream()
            .map(aff -> List.of(aff.split("/")))
            .flatMap(parts -> Stream.of(3, 2)
                .filter(i -> parts.size() >= i)
                .map(i -> String.join("/", parts.subList(0, i))))
            .sorted(Comparator.comparingInt(this::getAffiliationLevel).reversed())
            .map(name -> findByName(context, null, null, name, null))
            .filter(Objects::nonNull)
            .toList();
    }

    public OrgUnit findFirstByName(Context context, List<String> affiliations) {
        return findByName(context, affiliations).stream().findFirst().orElse(null);
    }

    /**
     * Get the level of a specific affiliation, based on the number of '/' in the string.
     * @param affiliation The affiliation to process the level of.
     * @return The level of the affiliation.
     */
    private int getAffiliationLevel(String affiliation) {
        // Subtract the length of the string to the length of the string without slashes.
        return affiliation.length() - affiliation.replace("/", "").length();
    }
}
