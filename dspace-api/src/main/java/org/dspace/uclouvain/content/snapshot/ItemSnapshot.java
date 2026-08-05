/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.snapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import org.dspace.content.Item;
import org.dspace.core.ReloadableEntity;
import org.dspace.uclouvain.content.snapshot.element.SnapshotElement;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Class representing an item snapshot into the DSpace system
 * <P>
 * An item snapshot is a picture of important data related to an {@link Item}.
 * Comparing two version of ItemSnapshot for the same item, took at different timestamp, allows to detect any changes
 * that we want to track.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@Entity
@Table(name = "uclouvain_item_snapshot")
public class ItemSnapshot implements ReloadableEntity<UUID> {

    @Id
    @Column(name = "uuid", unique = true, nullable = false, updatable = false)
    private UUID id;

    /**
     * The `last modified` date of the item state captured by this snapshot; it is NEVER the moment the row was
     * written. This column must stay insertable: it is what {@link ItemSnapshot} staleness is evaluated against, and
     * letting the database default it to the insertion time would make the first snapshot of an item meaningless.
     * It must also keep the very same type as {@link Item}'s `last_modified`, since staleness is evaluated by
     * comparing both columns in SQL: a naive timestamp facing a zoned one would be coerced using the database session
     * time zone, shifting the whole detection whenever that zone differs from the JVM one.
     */
    @Column(name = "timestamp", nullable = false, columnDefinition = "timestamp with time zone")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // Maps 'item' to the 'uuid' column and defines the relationship
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid")
    @MapsId
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Item item;

    @Transient
    private List<SnapshotElement> snapshotElements = new ArrayList<>();

    // GETTER & SETTER =================================================================================================
    @Override
    public UUID getID() {
        return id;
    }
    public void setID(UUID id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public Item getItem() {
        return item;
    }
    public void setItem(Item item) {
        this.item = item;
    }

    public List<SnapshotElement> getSnapshotElements() {
        return snapshotElements;
    }
    public <T extends SnapshotElement> List<T> getSnapshotElementsOfType(Class<T> type) {
        return (this.snapshotElements == null)
            ? List.of()
            : this.snapshotElements.stream()
                .filter(type::isInstance) // Keeps only elements of the requested type
                .map(type::cast)          // Safely casts SnapshotElement to T
                .toList();
    }
    public void setSnapshotElements(List<SnapshotElement> snapshotElements) {
        this.snapshotElements = snapshotElements;
    }
    public void addSnapshotElement(SnapshotElement snapshotElement) {
        this.snapshotElements.add(snapshotElement);
    }
    public SnapshotElement getSnapshotElement(String path) {
        for (SnapshotElement snapshot : snapshotElements) {
            if (Objects.equals(snapshot.getPath(), path)) {
                return snapshot;
            }
        }
        return null;
    }

}
