/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer.model;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * Representation of an update request stored in the 'uclouvain_item_authority_metadata_enhancement' table.
 * The table contains 3 columns:
 * - The item_uuid, which is the item being updated.
 * - The entity_type, which is the so called 'entity-type' of the item.
 * - The date_queued, which is giving a hint about when the item was queued.
 * 
 * This table is used as a queue for items that need to be updated using the custom enhancement system.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
@Entity
@Table(name = "uclouvain_item_authority_metadata_enhancement")
public class ItemToEnhance implements Serializable {
    @Id
    @Column(name = "item_uuid", unique = true, nullable = false, insertable = true, updatable = false)
    private UUID itemUUID;

    @Column(name = "entity_type", insertable = true, updatable = false, nullable = false)
    private String entityType;

    @Column(name = "date_queued", insertable = true, updatable = true, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateQueued = new Date();

    public boolean equals(ItemToEnhance ite) {
        return itemUUID == ite.getItemUUID();
    }

    // GETTERS && SETTERS
    public UUID getItemUUID() {
        return itemUUID;
    }

    public void setItemUUID(UUID itemUUID) {
        this.itemUUID = itemUUID;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Date getDateQueued() {
        return dateQueued;
    }

    public void setDateQueued(Date dateQueued) {
        this.dateQueued = dateQueued;
    }
}
