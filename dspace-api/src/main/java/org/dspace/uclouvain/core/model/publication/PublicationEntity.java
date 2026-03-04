/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model.publication;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.OrgUnit;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;
import org.dspace.web.ContextUtil;

/**
 * Object representing an entity related to a Publication
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class PublicationEntity {

    // CLASS ATTRIBUTE =================================================================================================
    private String name;
    private String institution;
    private int place;
    private OrgUnit entityAuthority;

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private Context context;

    // CONSTRUCTOR =====================================================================================================
    public PublicationEntity() {
        try {
            Context context = ContextUtil.obtainCurrentRequestContext();
            this.context = (context != null) ? context : new Context();
        } catch (Exception e) {
            this.context = new Context();
        }
    }

    // GETTER & SETTER =================================================================================================
    public String getName() {
        return name;
    }
    public PublicationEntity setName(String name) {
        this.name = name;
        return this;
    }

    public String getInstitution() {
        return institution;
    }
    public PublicationEntity setInstitution(String institution) {
        this.institution = institution;
        return this;
    }

    public OrgUnit getAuthority() {
        return entityAuthority;
    }
    public PublicationEntity setAuthority(UUID authorityID) {
        if (authorityID == null) {
            return this;
        }
        try {
            Item authorityItem = itemService.find(context, authorityID);
            if (authorityItem == null) {
                throw new IllegalArgumentException("Unable to load linked authority [" + authorityID + "]");
            }
            this.entityAuthority = new OrgUnit(authorityItem);
            return this;
        } catch (InvalidModelEntityTypeException | SQLException e) {
            throw new RuntimeException();
        }
    }
    public boolean hasAuthority() {
        return entityAuthority != null;
    }

    public int getPlace() {
        return place;
    }
    public PublicationEntity setPlace(int place) {
        this.place = place;
        return this;
    }
}
