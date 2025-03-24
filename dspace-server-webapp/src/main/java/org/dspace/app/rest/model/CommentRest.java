/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

@LinksRest(links = {
    @LinkRest(name = CommentRest.ITEM, method = "getItem")
})
public class CommentRest extends BaseObjectRest<UUID> {

    public static final String NAME = "comment";
    public static final String PLURAL_NAME = "comments";
    public static final String CATEGORY = RestAddressableModel.CORE;

    public static final String ITEM = "item";

    // CLASS ATTRIBUTES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private UUID id;
    private UUID owner;
    private String authorName;
    private UUID authorAuthority;
    private String content;
    private Date created;
    private Date modified;

    // GETTER & SETTER ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @JsonIgnore
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @JsonIgnore
    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getType() {
        return NAME;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    public UUID getOwner() {
        return owner;
    }
    public void setOwner(UUID owner) {
        this.owner = owner;
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

    public Date getCreated() {
        return created;
    }
    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getModified() {
        return modified;
    }
    public void setModified(Date modified) {
        this.modified = modified;
    }
}
