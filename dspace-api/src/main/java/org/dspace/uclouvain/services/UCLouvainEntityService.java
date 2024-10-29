/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.util.List;

import org.dspace.uclouvain.core.model.Entity;
import org.dspace.uclouvain.core.model.EntityType;

/** Service allowing to search an UCLouvain entity (department, degree, faculty, ...) */
public interface UCLouvainEntityService {

    /**
     * Find entities matching search criteria
     *
     * @param entityCode the entity code to search.
     * @param entityType the entity type to search (optional)
     * @return an entity list matching search criteria
     */
    List<Entity> find(String entityCode, EntityType entityType);

    /**
     * Find a single entity matching criteria
     *
     * @param entityCode the entity code to search.
     * @param entityType the entity type to search (optional)
     * @return the first matching entity.
     */
    Entity findFirst(String entityCode, EntityType entityType);
}
