 /**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model.publication;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.web.ContextUtil;

/**
 * Object representing an author of a Publication
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class PublicationAuthor {

    // CLASS CONSTANTS =================================================================================================
    public static final String ROLE_AUTHOR = "author";
    public static final String ROLE_COLLABORATOR = "collaborator";
    public static final String ROLE_TRANSLATOR = "translator";
    public static final String ROLE_INVENTOR = "inventor";
    public static final String ROLE_PREFACE_WRITER = "preface_writer";
    public static final String ROLE_DIRECTOR = "scientific_director_editor";
    public static final String ROLE_LAST_AUTHOR = "co_last_author";
    public static final String ROLE_FIRST_AUTHOR = "co_first_author";

    private static final Set<String> AUTHOR_ROLES = Set.of(ROLE_AUTHOR, ROLE_FIRST_AUTHOR, ROLE_LAST_AUTHOR);

    // CLASS ATTRIBUTE =================================================================================================
    private String name;
    private String email;
    private String institution;
    private String role;
    private Map<String, String> identifiers = new HashMap<>();
    private Item researcherProfileAuthority;

    private final ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private Context context;

    // CONSTRUCTOR =====================================================================================================
    public PublicationAuthor() {
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
    public PublicationAuthor setName(String name) {
        this.name = name;
        return this;
    }

    public String getEmail() {
        return email;
    }
    public PublicationAuthor setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getInstitution() {
        return institution;
    }
    public PublicationAuthor setInstitution(String institution) {
        this.institution = institution;
        return this;
    }

    public String getRole() {
        return role;
    }
    public PublicationAuthor setRole(String role) {
        this.role = role;
        return this;
    }

    public String getOrcidID() {
        String orcidID = identifiers.getOrDefault("orcid", null);
        if (StringUtils.isBlank(orcidID) && researcherProfileAuthority != null) {
            orcidID = itemService.getMetadataFirstValue(
                researcherProfileAuthority,
                new MetadataFieldName(
                    configService.getProperty("uclouvain.global.metadata.person.orcidID.field", "dc.identifier.orcid")
                ),
                null
            );
            identifiers.put("orcid", orcidID);
        }
        return orcidID;
    }
    public PublicationAuthor setOrcidID(String orcidID) {
        this.identifiers.put("orcid", orcidID);
        return this;
    }

    public String getFgs() {
        String fgs = identifiers.getOrDefault("fgs", null);
        if (StringUtils.isBlank(fgs) && researcherProfileAuthority != null) {
            fgs = itemService.getMetadataFirstValue(
                researcherProfileAuthority,
                new MetadataFieldName(configService.getProperty(
                    "uclouvain.global.metadata.person.institutionalID.field",
                    "person.identifier.fgs")
                ),
                null
            );
            identifiers.put("fgs", fgs);
        }
        return fgs;
    }

    public Item getAuthority() {
        return researcherProfileAuthority;
    }
    public PublicationAuthor setAuthority(UUID authorityID) {
        if (authorityID == null) {
            return this;
        }
        try {
            Item authorityItem = itemService.find(context, authorityID);
            if (authorityItem == null) {
                throw new IllegalArgumentException("Unable to load linked authority [" + authorityID + "]");
            }
            String entityType = itemService.getEntityType(authorityItem);
            String researchProfileEntityType = configService.getProperty("researcher-profile.entity-type", "Person");
            if (entityType != null && !entityType.equals(researchProfileEntityType)) {
                throw new IllegalArgumentException("Item[" + authorityID + "] isn't a valid researcher profile");
            }
            this.researcherProfileAuthority = authorityItem;
            return this;
        } catch (SQLException e) {
            throw new RuntimeException();
        }
    }

    // FUNCTION ========================================================================================================
    public String toString() {
        return "<%s name=[%s]>".formatted(PublicationAuthor.class.getName(), getName());
    }

    public boolean hasAuthorRole() {
        return AUTHOR_ROLES.contains(this.role);
    }

}
