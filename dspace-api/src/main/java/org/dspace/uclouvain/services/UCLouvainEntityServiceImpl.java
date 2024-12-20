/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.uclouvain.configurationFiles.files.EntitiesConfigurationFile;
import org.dspace.uclouvain.core.model.Entity;
import org.dspace.uclouvain.core.model.EntityType;
import org.springframework.beans.factory.annotation.Autowired;

/** Implementation of UCLouvainEntityService: to search an UCLouvain entity */
public class UCLouvainEntityServiceImpl implements UCLouvainEntityService {

    @Autowired
    EntitiesConfigurationFile entitiesConfigurationFile;

    /**
     * Find all entities for a specific entity type
     *
     * @param entityType the entity type to search (optional).
     *                   If null, then all entities will be return independent of its entity type.
     * @return an entity list matching search criteria
     */
    @Override
    public List<Entity> find(EntityType entityType) {
        try {
            return this.entitiesConfigurationFile.getData()
                    .stream()
                    .filter(e -> entityType == null || e.getType() == entityType)  // Allow null value to return all
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Find entities matching search criteria
     *
     * @param entityCode the entity code to search.
     * @param entityType the entity type to search (optional)
     * @return an entity list matching search criteria
     */
    @Override
    public List<Entity> find(String entityCode, EntityType entityType) {
        return find(entityType)
                .stream()
                .filter(e -> e.getCode().equals(entityCode))
                .collect(Collectors.toList());
    }

    /**
     * Find a single entity matching criteria
     *
     * @param entityCode the entity code to search.
     * @param entityType the entity type to search (optional)
     * @return the first matching entity.
     */
    @Override
    public Entity findFirst(String entityCode, EntityType entityType) {
        return find(entityCode, entityType).stream().findFirst().orElse(null);
    }
}
