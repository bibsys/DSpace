/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model.publication;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.uclouvain.core.model.ItemModel;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;

/**
 * Object representing a Publication object.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class Publication extends ItemModel {

    // CLASS CONSTANTS =================================================================================================
    public static final String ENTITY_TYPE = "Publication";

    // METADATA FIELDS DEFINITIONS =====================================================================================
    public static final String AUTHOR_NAME_FIELD =
            configService.getProperty(FIELD_PREFIX + "publication.authorName.field", "dc.contributor.author");
    public static final String AUTHOR_EMAIL_FIELD =
            configService.getProperty(FIELD_PREFIX + "publication.authorEmail.field", "authors.email");
    public static final String AUTHOR_INSTITUTION_FIELD =
            configService.getProperty(FIELD_PREFIX + "publication.authorInstitution.field", "authors.institution.code");
    public static final String AUTHOR_ROLE_FIELD =
            configService.getProperty(FIELD_PREFIX + "publication.authorRole.field", "authors.role");

    // CONSTRUCTOR =====================================================================================================
    public Publication(Item item) throws InvalidModelEntityTypeException {
        super(item);
        if (!Objects.equals(itemService.getEntityType(item), ENTITY_TYPE)) {
            throw new InvalidModelEntityTypeException(item, ENTITY_TYPE);
        }
    }

    // FUNCTIONS =======================================================================================================

    /**
     * Allows retrieving all authors of the publication.
     * @return The list of {@class PublicationAuthor} of the publication.
     */
    public List<PublicationAuthor> getAuthors() {
        return itemService
            .getMetadataByMetadataString(item, AUTHOR_NAME_FIELD)
            .stream()
            .map(mv -> new PublicationAuthor()
                .setName(mv.getValue())
                .setAuthority((mv.getAuthority() != null) ? UUID.fromString(mv.getAuthority()) : null)
                .setEmail(itemService.getMetadata(item, AUTHOR_EMAIL_FIELD, mv.getPlace()))
                .setInstitution(itemService.getMetadata(item, AUTHOR_INSTITUTION_FIELD, mv.getPlace()))
                .setRole(itemService.getMetadata(item, AUTHOR_ROLE_FIELD, mv.getPlace()))
            )
            .toList();
    }

    /**
     * Allows retrieving authors of the publication for specific roles
     * @param authorRoles author roles to filter (See {@class PublicationAuthor} constants)
     * @return The list of {@class PublicationAuthor} of the publication matching roles
     */
    public List<PublicationAuthor> getAuthors(String... authorRoles) {
        List<String> roles = Arrays.asList(authorRoles);
        return getAuthors()
            .stream()
            .filter(author -> roles.contains(author.getRole()))
            .toList();
    }
}
