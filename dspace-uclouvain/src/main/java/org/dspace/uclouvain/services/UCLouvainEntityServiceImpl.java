package org.dspace.uclouvain.services;

import org.dspace.uclouvain.core.model.Entity;
import org.dspace.uclouvain.configurationFiles.files.EntitiesConfigurationFile;
import org.dspace.uclouvain.core.model.EntityType;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Implementation of UCLouvainEntityService : to search an UCLouvain entity */
public class UCLouvainEntityServiceImpl implements UCLouvainEntityService {

    @Autowired
    EntitiesConfigurationFile entitiesConfigurationFile;


    /**
     * Find entities matching search criteria
     *
     * @param entityCode: the entity code to search.
     * @param entityType: the entity type to search (optional)
     * @return an entity list matching search criteria
     */
    @Override
    public List<Entity> find(String entityCode, EntityType entityType) {
        Entity fakeEntity = new Entity(entityCode, entityType);
        try {
            return this.entitiesConfigurationFile.getData()
                    .stream()
                    .filter(e -> e.equals(fakeEntity))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Find a single entity matching criteria
     *
     * @param entityCode: the entity code to search.
     * @param entityType: the entity type to search (optional)
     * @return the first matching entity.
     */
    @Override
    public Entity findFirst(String entityCode, EntityType entityType) {
        Entity fakeEntity = new Entity(entityCode, entityType);
        try {
            return this.entitiesConfigurationFile.getData()
                    .stream()
                    .filter(e -> e.equals(fakeEntity))
                    .findFirst().orElse(null);
        } catch (IOException e) {
            return null;
        }
    }
}
