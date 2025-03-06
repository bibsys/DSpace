/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content;

import java.util.Date;
import java.util.UUID;

import org.dspace.content.Item;

/**
 * Describe a comment related to an {@link org.dspace.content.Item}.
 *    An {@link org.dspace.content.Item} can have multiple {@link org.dspace.uclouvain.content.Comment} (0..N)
 *    A {@link org.dspace.uclouvain.content.Comment} belongs to a single {@link org.dspace.content.Item} (1..1)
 * We don't use UUID as comment id because, depending on the comment implementation in the system, the ID could have
 * several forms: UUID, timestamp, hash, ...
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class Comment {

    private String id;
    private Item parent;
    private Date creationDate;
    private Date modifiedDate;
    private String authorName;
    private UUID authorAuthority;
    private String content;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Item getParent() {
        return parent;
    }

    public void setParent(Item parent) {
        this.parent = parent;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public UUID getAuthorAuthority() {
        return authorAuthority;
    }

    public void setAuthorAuthority(UUID authorAuthority) {
        this.authorAuthority = authorAuthority;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
