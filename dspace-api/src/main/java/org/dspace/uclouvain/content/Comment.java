/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.dspace.eperson.EPerson;
import org.hibernate.annotations.GenericGenerator;


/**
 * Class representing item comment stored into the DSpace system.
 * <P>
 * The corresponding Comment objects are loaded into memory. At present, there is no metadata associated with comments.
 * Each comment is associated with only one Item; but an item could have multiple related comments.
 * A comment could be also linked to an author profile (but it's not required).
 * Some operation on the Item or related Bitstream objects could generate some system comment to trace Item history.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 * @version $Revision$
 */
@Entity
@Table(name = "comment")
public class Comment implements Serializable, ReloadableEntity<UUID> {

    protected static final String SYSTEM_COMMENT_OWNER = "system";

    @Id
    @GeneratedValue(generator = "predefined-uuid")
    @GenericGenerator(name = "predefined-uuid", strategy = "org.dspace.content.PredefinedUUIDGenerator")
    @Column(name = "uuid", unique = true, nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Item owner;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private EPerson authorAuthority;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "modified")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedDate;

    // CONSTRUCTOR ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * Protected constructor, create object using:
     * {@link org.dspace.uclouvain.content.service.CommentService#create(Context, Item, String, String)}
     */
    protected Comment() { }

    // METHODS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public boolean isSystemComment() {
        return authorName.equals(SYSTEM_COMMENT_OWNER) && authorAuthority == null;
    }

    public String toString() {
        String content = (this.content == null)
            ? "null"
            : (this.content.length() > 20)
                ? this.content.substring(0, 20) + "..."
                : this.content;
        return "Comment [id=" + id + ", owner=" + owner + ", author=" + authorName + ", authority=" + authorAuthority
               + ", content=" + content + ", created=" + creationDate + ", modified=" + modifiedDate + "]";
    }

    // GETTER & SETTER ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public UUID getID() {
        return id;
    }
    protected void setId(UUID id) {
        this.id = id;
    }

    public Item getOwner() {
        return owner;
    }
    protected void setOwner(Item owner) {
        this.owner = owner;
    }

    public String getAuthorName() {
        return authorName;
    }
    protected void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public EPerson getAuthorAuthority() {
        return authorAuthority;
    }
    protected void setAuthorAuthority(EPerson authorAuthority) {
        this.authorAuthority = authorAuthority;
    }

    public String getContent() {
        return content;
    }
    protected void setContent(String content) {
        this.content = content;
    }

    public Date getCreationDate() {
        return creationDate;
    }
    protected void setCreationDate(Date date) {
        this.creationDate = date;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }
    protected void setModified() {
        this.modifiedDate = Date.from(Instant.now());
    }
}
