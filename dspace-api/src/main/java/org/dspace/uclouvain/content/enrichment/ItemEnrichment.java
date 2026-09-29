/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.enrichment;

import java.util.Date;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.dspace.core.ReloadableEntity;

/**
 * One attempt to enrich an item from an external source (CrossRef, PubMed, arXiv...) for a given identifier value.
 * The item is referenced by its UUID only: the row is deleted with the item (ON DELETE CASCADE).
 */
@Entity
@Table(name = "uclouvain_item_enrichment")
public class ItemEnrichment implements ReloadableEntity<Integer> {

    /** Outcome of the attempt. */
    public enum Status {
        SUCCESS,    // The source answered with data.
        NOT_FOUND,  // The source does not know the identifier
        ERROR       // The source could not be reached or answered with an error.
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "uclouvain_item_enrichment_id_seq")
    @SequenceGenerator(name = "uclouvain_item_enrichment_id_seq", sequenceName = "uclouvain_item_enrichment_id_seq",
        allocationSize = 1)
    private Integer id;

    @Column(name = "item_uuid", nullable = false, updatable = false)
    private UUID itemUuid;

    @Column(name = "provider", nullable = false, updatable = false)
    private String provider;

    @Column(name = "identifier_field", nullable = false, updatable = false)
    private String identifierField;

    @Column(name = "identifier_value", nullable = false, updatable = false)
    private String identifierValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, updatable = false)
    private Status status;

    @Column(name = "reason", updatable = false)
    private String reason;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date attemptedAt;

    // GETTER & SETTER =================================================================================================
    @Override
    public Integer getID() {
        return id;
    }

    public UUID getItemUuid() {
        return itemUuid;
    }

    public void setItemUuid(UUID itemUuid) {
        this.itemUuid = itemUuid;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getIdentifierField() {
        return identifierField;
    }

    public void setIdentifierField(String identifierField) {
        this.identifierField = identifierField;
    }

    public String getIdentifierValue() {
        return identifierValue;
    }

    public void setIdentifierValue(String identifierValue) {
        this.identifierValue = identifierValue;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Date getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(Date attemptedAt) {
        this.attemptedAt = attemptedAt;
    }
}
