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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.dspace.content.Item;

/**
 * Representation of an update request stored in the 'uclouvain_item_authority_metadata_enhancement' table.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
@Entity
@Table(name = "uclouvain_item_authority_metadata_enhancement")
public class ItemToEnhance implements Serializable {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_uuid",
        insertable = true, updatable = false, nullable = false, referencedColumnName = "uuid"
    )
    private Item sourceItem;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_uuid",
        insertable = true, updatable = false, nullable = false, referencedColumnName = "uuid"
    )
    private Item targetItem;

    @Column(name = "date_queued", insertable = true, updatable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateQueued = new Date();

    public boolean equals(ItemToEnhance ite) {
        return sourceItem == ite.getSourceItem() && targetItem == ite.getTargetItem();
    }

    // GETTERS && SETTERS
    public Item getSourceItem() {
        return sourceItem;
    }

    public void setSourceItem(Item source_uuid) {
        this.sourceItem = source_uuid;
    }

    public Item getTargetItem() {
        return targetItem;
    }

    public void setTargetItem(Item targetItem) {
        this.targetItem = targetItem;
    }

    public Date getDateQueued() {
        return dateQueued;
    }

    public void setDateQueued(Date dateQueued) {
        this.dateQueued = dateQueued;
    }
}
