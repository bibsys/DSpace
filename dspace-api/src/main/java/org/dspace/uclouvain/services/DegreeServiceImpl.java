/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.uclouvain.core.model.Entity;
import org.dspace.uclouvain.core.model.EntityType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link DegreeService} that searches for degrees from
 * multiple sources:
 * <ul>
 *   <li>MasterThesisService — degrees from existing thesis items in the archive (via Solr)</li>
 *   <li>EntityService — configured degrees from the entity configuration file</li>
 * </ul>
 * Results are merged and deduplicated by degree label.
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class DegreeServiceImpl implements DegreeService {

    public static final String DEGREE_LABEL_FIELD = "masterthesis.degree.label";
    public static final String DEGREE_CODE_FIELD = "masterthesis.degree.code";
    public static final String ROOT_DEGREE_LABEL_FIELD = "masterthesis.rootdegree.label";
    public static final String ROOT_DEGREE_CODE_FIELD = "masterthesis.rootdegree.code";

    @Autowired
    private MasterThesisService masterThesisService;

    @Autowired
    private UCLouvainEntityService entityService;

    @Autowired
    private ItemService itemService;

    @Override
    public List<DegreeSearchResult> search(String query, int limit) {
        // 1) Search MasterThesis items and extract degrees
        List<DegreeSearchResult> thesisResults = searchMasterThesis(query);

        // 2) Search EntityService for configured degrees (fills gap for degrees not in archive)
        List<DegreeSearchResult> entityResults = searchEntities(query);

        // 3) Merge and deduplicate by degreeLabel (thesis results take priority)
        List<DegreeSearchResult> results = deduplicate(thesisResults, entityResults);

        return (limit <= 0 || results.size() <= limit)
            ? results
            : results.subList(0, limit); // Apply pagination
    }

    /**
     * Deduplicate two lists of results by degree label.
     * Results from the first (thesis) list take priority.
     *
     * @param first  primary results (higher priority)
     * @param second secondary results (fill the gap)
     * @return merged and deduplicated list
     */
    private List<DegreeSearchResult> deduplicate(List<DegreeSearchResult> first,
                                                  List<DegreeSearchResult> second) {
        return Stream.concat(first.stream(), second.stream())
            .collect(Collectors.toMap(
                DegreeSearchResult::degreeLabel, // Key
                degreeResult -> degreeResult, // Value
                (existing, replacement) -> existing, // If duplicate key, keep existing key
                LinkedHashMap::new
            ))
            .values().stream().toList();
    }

    /**
     * Search for degrees by querying MasterThesis items via Solr and
     * extracting degree metadata from the matching items.
     *
     * @param query the search text
     * @return list of degree search results from thesis items
     */
    private List<DegreeSearchResult> searchMasterThesis(String query) {
        List<DegreeSearchResult> results = new ArrayList<>();
        try (Context context = new Context()) {
            Triple<String, Object, Boolean> criteria = Triple.of(DEGREE_LABEL_FIELD, query, true);
            Iterator<Item> items = masterThesisService.search(context, criteria);

            while (items.hasNext()) {
                results.addAll(extractDegreesFromItem(items.next(), query));
            }
        } catch (SearchServiceException e) {
            // Log but don't fail — fall back to EntityService results
        }
        return results;
    }

    /**
     * Extract degree metadata values from a single thesis Item.
     * Handles multi-valued metadata fields (parallel lists for label, code,
     * rootdegree label, rootdegree code). Only degrees whose label matches
     * the query text are returned.
     *
     * @param item  the thesis Item
     * @param query the search query text
     * @return list of degree search results for this item
     */
    private List<DegreeSearchResult> extractDegreesFromItem(Item item, String query) {
        List<String> labels = getAllMetadataValues(item, DEGREE_LABEL_FIELD);
        List<String> codes = getAllMetadataValues(item, DEGREE_CODE_FIELD);
        List<String> rootLabels = getAllMetadataValues(item, ROOT_DEGREE_LABEL_FIELD);
        List<String> rootCodes = getAllMetadataValues(item, ROOT_DEGREE_CODE_FIELD);

        return IntStream.range(0, labels.size())
            .filter(i -> labels.get(i) != null)
            .filter(i -> StringUtils.containsIgnoreCase(labels.get(i), query))
            .mapToObj(i -> new DegreeSearchResult(
                labels.get(i),
                getValueOrNull(codes, i),
                getValueOrNull(rootLabels, i),
                getValueOrNull(rootCodes, i)
            )).toList();
    }

    private String getValueOrNull(List<String> list, int index) {
        return index < list.size() ? list.get(index) : null;
    }

    private List<String> getAllMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField)
                .stream().map(mv -> mv.getValue()).toList();
    }

    /**
     * Search for degrees in the EntityService configuration.
     *
     * @param query the search text
     * @return list of configured degree search results matching the query
     */
    private List<DegreeSearchResult> searchEntities(String query) {
        return entityService.find(EntityType.DEGREE).stream()
            .filter(degree -> degree.getName() != null)
            .filter(degree -> StringUtils.containsIgnoreCase(degree.getName(), query))
            .map(degree -> {
                Entity root = getRootDegree(degree);
                return new DegreeSearchResult(
                    degree.getName(),
                    degree.getCode(),
                    (root != null) ? root.getName() : null,
                    (root != null) ? root.getCode() : null
                );
            })
            .toList();
    }

    /**
     * Retrieve the root degree of a given degree.
     */
    private Entity getRootDegree(Entity degree) {
        if (degree == null) {
            return null;
        }
        Entity root = getRootDegree(degree.getParent());
        if (root != null) {
            return root;
        }
        return degree.getType() == EntityType.DEGREE ? degree : null;
    }
}
