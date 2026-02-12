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
import java.util.stream.Stream;

import org.dspace.content.Item;
import org.dspace.core.CrisConstants;
import org.dspace.eperson.dto.RegistrationDataChanges;
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

    public static final String STATUS_SUBMITTED = "submitted";
    public static final String STATUS_INPRESS = "accepted/in-press";
    public static final String STATUS_PUBLISHED = "published";

    // METADATA FIELDS DEFINITIONS =====================================================================================
    public static final String AUTHOR_NAME_FIELD =
            configService.getProperty(FIELD_PREFIX + "publication.authorName.field", "dc.contributor.author");
    public static final String AUTHOR_EMAIL_FIELD =
            configService.getProperty(FIELD_PREFIX + "publication.authorEmail.field", "authors.email");
    public static final String AUTHOR_ORCID_FIELD =
            configService.getProperty(FIELD_PREFIX + "publication.authorOrcid.field", "authors.identifier.orcid");
    public static final String AUTHOR_INSTITUTION_FIELD =
            configService.getProperty(FIELD_PREFIX + "publication.authorInstitution.field", "authors.institution.code");
    public static final String AUTHOR_ROLE_FIELD =
            configService.getProperty(FIELD_PREFIX + "publication.authorRole.field", "authors.role");
    public static final String AUTHOR_FGS_FIELD =
            configService.getProperty(FIELD_PREFIX + "publication.authorFgs.field", "authors.identifier.fgs");
    public static final String MAIN_TYPE_FIELD =
            configService.getProperty(FIELD_PREFIX + "maintype.field", "dc.type.maintype");
    public static final String SUB_TYPE_FIELD =
            configService.getProperty(FIELD_PREFIX + "subtype.field", "dc.type.subtype");

    // CONSTRUCTOR =====================================================================================================
    protected Publication(Item item) throws InvalidModelEntityTypeException {
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
                .setPlace(mv.getPlace())
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

    /**
     * Find an author based on its place in the authors of the publication.
     * 
     * @param place The place of the author to find.
     * @return The author of the publication at the given place.
     */
    public PublicationAuthor getAuthor(int place) {
        return getAuthors().stream()
            .filter(author -> Objects.equals(author.getPlace(), place))
            .findFirst()
            .orElse(null);
    }

    /**
     * Get emails of the authors of the publication.
     * @param publicationEmail Include (or not) the email stored as a publication metadata (default=authors.email)
     * @param privateEmail Include (or not) private/specific email of author link to a ResearchProfile authority.
     * @return the list of emails for the publication. This list can be empty ! (especially if both params are `false`)
     */
    public List<String> getAuthorsEmails(boolean publicationEmail, boolean privateEmail) {
        return getAuthors().stream()
            .flatMap(author -> Stream.of(
                publicationEmail ? author.getEmail() : null,
                privateEmail ? author.getPrivateEmail() : null
            ))
            .filter(Objects::nonNull)
            .filter(email -> !CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE.equals(email))
            .filter(email -> email.matches(RegistrationDataChanges.EMAIL_PATTERN))
            .distinct()
            .toList();
    }

    public String getMainType() {
        return this.getFirstMetadataValue(MAIN_TYPE_FIELD);
    }

    public String getSubType() {
        return this.getFirstMetadataValue(SUB_TYPE_FIELD);
    }
}
