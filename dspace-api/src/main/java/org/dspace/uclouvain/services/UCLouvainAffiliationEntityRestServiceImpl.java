/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.AffiliationEntityRestModel;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service to generate an affiliation entity tree.
 * It calls Solr to get all the OrgUnits and convert them into a list of 'AffiliationsEntities'.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class UCLouvainAffiliationEntityRestServiceImpl implements UCLouvainAffiliationEntityRestService {

    private static final Logger logger = LogManager.getLogger(UCLouvainAffiliationEntityRestServiceImpl.class);

    @Autowired
    private OrgUnitService orgUnitService;

    @Override
    public List<AffiliationEntityRestModel> getAffiliationsEntities(
        Context context,
        UUID parentUUID,
        int depth,
        boolean includeDocCount
    ) {
        try {
            // 1. Fetch flat list and map to REST models
            List<AffiliationEntityRestModel> allModels = orgUnitService.findAll(context)
                    .stream()
                    .map(AffiliationEntityRestModel::new)
                    .toList();

            if (allModels.isEmpty()) {
                return Collections.emptyList();
            }

            // 2. Create lookup map for O(1) access
            Map<UUID, AffiliationEntityRestModel> lookup = allModels.stream()
                    .collect(Collectors.toMap(m -> m.uuid, Function.identity()));

            // 3. Rebuild the complete hierarchy
            List<AffiliationEntityRestModel> allRoots = linkNodes(allModels, lookup);

            // 4. Select the starting branch
            List<AffiliationEntityRestModel> result = allRoots;
            if (parentUUID != null) {
                AffiliationEntityRestModel targetNode = lookup.get(parentUUID);
                result = (targetNode != null) ? targetNode.children : Collections.emptyList();
            }

            // 5. Apply recursive filters (Counts & Depth)
            if (!result.isEmpty()) {
                Map<UUID, Long> countMap = includeDocCount ? orgUnitService.getPublicationCount(context) : null;
                applyFiltersRecursive(result, countMap, depth, 1);
            }
            return result;
        } catch (Exception e) {
            logger.error("Error while generating affiliation tree: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Connects children to parents and identifies root nodes.
     * * @param models The flat list of models.
     * @param lookup Map for fast node access.
     * @return The list of root nodes.
     */
    private List<AffiliationEntityRestModel> linkNodes(
            List<AffiliationEntityRestModel> models,
            Map<UUID, AffiliationEntityRestModel> lookup
    ) {
        List<AffiliationEntityRestModel> roots = new ArrayList<>();
        for (AffiliationEntityRestModel model : models) {
            if (model.parent == null) {
                roots.add(model);
            } else {
                AffiliationEntityRestModel parentNode = lookup.get(model.parent);
                if (parentNode != null) {
                    parentNode.children.add(model);
                } else {
                    // If the parent is missing from the list, treat this node as a root
                    roots.add(model);
                }
            }
        }
        return roots;
    }

    /**
     * Recursively applies depth pruning and publication counts.
     * * @param models       Current node list.
     * @param countMap     Map of publication counts.
     * @param maxDepth     The maximum depth to keep.
     * @param currentDepth The current level in the tree.
     */
    private void applyFiltersRecursive(
        List<AffiliationEntityRestModel> models,
        Map<UUID, Long> countMap,
        Integer maxDepth,
        int currentDepth
    ) {
        if (models == null || models.isEmpty()) {
            return;
        }
        for (AffiliationEntityRestModel model : models) {
            if (countMap != null) {
                model.relatedPublicationCount = countMap.getOrDefault(model.uuid, 0L);
            }
            if (maxDepth != null && currentDepth >= maxDepth) {
                // Cut branches beyond maxDepth
                model.children = new ArrayList<>();
            } else {
                applyFiltersRecursive(model.children, countMap, maxDepth, currentDepth + 1);
            }
        }
    }
}
