/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model.publication;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.access.status.AccessStatusHelper;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.eperson.dto.RegistrationDataChanges;
import org.dspace.uclouvain.core.model.ItemModel;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;
import org.dspace.uclouvain.core.utils.DateUtils;
import org.dspace.uclouvain.exceptions.DateConversionException;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.plugins.UCLouvainAccessStatusHelper;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;

/**
 * Object representing a Publication object.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class Publication extends ItemModel implements FWBValidation {

    // CLASS CONSTANTS =================================================================================================
    public static final String ENTITY_TYPE = "Publication";

    public static final String STATUS_SUBMITTED = "submitted";
    public static final String STATUS_INPRESS = "accepted/in-press";
    public static final String STATUS_PUBLISHED = "published";

    // METADATA FIELDS DEFINITIONS =====================================================================================
    public static final String MAIN_TYPE_FIELD =
        getField("mainType", "dc.type.maintype");
    public static final String SUB_TYPE_FIELD =
        getField("subType", "dc.type.subtype");

    public static final String AUTHOR_NAME_FIELD =
        getField("authorName", "dc.contributor.author");
    public static final String AUTHOR_EMAIL_FIELD =
        getField("authorEmail", "authors.email");
    public static final String AUTHOR_ORCID_FIELD =
        getField("authorOrcid", "authors.identifier.orcid");
    public static final String AUTHOR_INSTITUTION_FIELD =
        getField("authorInstitution", "authors.institution.code");
    public static final String AUTHOR_ROLE_FIELD =
        getField("authorRole", "authors.role");
    public static final String AUTHOR_FGS_FIELD =
        getField("authorFgs", "authors.identifier.fgs");
    public static final String AUTHOR_ETAL_FIELD =
        getField("additionalAuthors", "dc.contributor.etal");
    public static final String ADVISOR_EMAIL_FIELD =
        getField("advisorEmail", "advisors.email");

    public static final String ENTITY_DEPARTMENT_FIELD =
        getField("entityDepartmentName", "oairecerif.affiliation.orgunitDepartment");
    public static final String ENTITY_INSTITUTION_FIELD =
            getField("entityInstitutionName", "oairecerif.affiliation.orgunit");

    public static final String TITLE_FIELD =
        getField("title", "dc.title");
    public static final String ABSTRACT_FIELD =
        getField("abstract", "dc.description.abstract");
    public static final String DATE_ISSUED_FIELD =
        getField("dateIssued", "dc.date.issued");
    public static final String LANGUAGE_FIELD =
        getField("language", "dc.language.iso");
    public static final String KEYWORD_FIELD =
        getField("keyword", "dc.subject");
    public static final String MESH_KEYWORD_FIELD =
        getField("meshKeyword", "dc.subject.mesh");
    public static final String PUBLICATION_STATUS_FIELD =
        getField("publication-status", "publication.publicationStatus");

    public static final String CONFERENCE_NAME_FIELD =
        getField("conferenceName", "publication.conference.name");
    public static final String CONFERENCE_LOCATION_FIELD =
        getField("conferenceLocation", "publication.conference.location");
    public static final String CONFERENCE_START_DATE_FIELD =
        getField("conferenceStartDate", "publication.conference.startDate");
    public static final String CONFERENCE_END_DATE_FIELD =
        getField("conferenceEndDate", "publication.conference.endDate");
    public static final String CONFERENCE_IS_ABSTRACT_FIELD =
        getField("conferenceIsAbstract", "publication.isAbstract");

    public static final String JOURNAL_TITLE_FIELD =
        getField("journalTitle", "dc.relation.journal");
    public static final String JOURNAL_ISSN_FIELD =
        getField("journalIssn", "publication.serial.issn");
    public static final String JOURNAL_EISSN_FIELD =
        getField("journalEissn", "publication.serial.eissn");
    public static final String JOURNAL_PEER_REVIEWED_FIELD =
        getField("journalPeerReviewed", "publication.serial.peerReviewed");
    public static final String JOURNAL_VOLUME_FIELD =
        getField("journalVolume", "publication.serial.volume");
    public static final String JOURNAL_ISSUE_FIELD =
        getField("journalIssue", "publication.serial.issue");
    public static final String JOURNAL_PAGES_FIELD =
        getField("journalPages", "publication.serial.pages");
    public static final String JOURNAL_DATE_ISSUED_FIELD =
        getField("journalDateIssued", "publication.serial.dateIssued");

    public static final String EDITOR_NAME_FIELD =
        getField("editorName", "publication.editor.name");
    public static final String EDITOR_LOCATION_FIELD =
        getField("editorLocation", "publication.editor.location");

    public static final String HOST_BOOK_TITLE_FIELD =
        getField("hostTitle", "publication.host.title");
    public static final String HOST_DOCUMENT_TYPE_FIELD =
        getField("hostType", "publication.host.type");
    public static final String HOST_DOCUMENT_ISBN_FIELD =
        getField("hostIsbn", "publication.host.isbn");

    public static final String DEFENSE_DATE_FIELD =
        getField("dissertationDefenseDate", "dissertation.defenseDate");

    // CLASS ATTRIBUTES ================================================================================================
    AccessStatusHelper helper = (AccessStatusHelper) CoreServiceFactory
        .getInstance()
        .getPluginService()
        .getSinglePlugin(AccessStatusHelper.class);
    UCLouvainResourcePolicyService uclouvainResourcePolicyService = UCLouvainServiceFactory
        .getInstance().
        getResourcePolicyService();

    // CONSTRUCTOR =====================================================================================================
    protected Publication(Item item) throws InvalidModelEntityTypeException {
        super(item);
        if (!Objects.equals(itemService.getEntityType(item), ENTITY_TYPE)) {
            throw new InvalidModelEntityTypeException(item, ENTITY_TYPE);
        }
    }

    // PUBLIC METHODS ==================================================================================================
    /**
     * Allows retrieving all authors of the publication.
     *
     * @return The list of {@link PublicationAuthor} of the publication.
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
     *
     * @param authorRoles author roles to filter (See {@link PublicationAuthor} constants)
     * @return The list of {@link PublicationAuthor} of the publication matching roles
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
     * Methods to know if the publication is made with additional author not encoded into the repository (aka 'et al.')
     *
     * @return true if the publication has extra additional authors, false, otherwise
     */
    public boolean hasExtraAuthors() {
        return Objects.equals(this.getFirstMetadataValue(AUTHOR_ETAL_FIELD), "true");
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

    /**
     * Allows retrieving all entities related to the publication.
     * These entity could be linked (or not !) to an {@link org.dspace.uclouvain.core.model.OrgUnit} object
     *
     * @return The list of {@link PublicationEntity} of the publication.
     */
    public List<PublicationEntity> getEntities() {
        return itemService
            .getMetadataByMetadataString(item, ENTITY_DEPARTMENT_FIELD)
            .stream()
            .map(mv -> new PublicationEntity()
                .setName(mv.getValue())
                .setAuthority((mv.getAuthority() != null) ? UUID.fromString(mv.getAuthority()) : null)
                .setInstitution(itemService.getMetadata(item, ENTITY_INSTITUTION_FIELD, mv.getPlace()))
                .setPlace(mv.getPlace())
            )
            .toList();
    }

    /** Get the document type of the publication */
    public String getMainType() {
        return this.getFirstMetadataValue(MAIN_TYPE_FIELD);
    }

    /** Get the document subtype of the publication */
    public String getSubType() {
        return this.getFirstMetadataValue(SUB_TYPE_FIELD);
    }

    /**
     * Get the issued year of the publication
     *
     * @return the issued year, or -1 if no valid issued year could be found
     */
    public int getIssuedYear() {
        try {
            String dateIssued = this.getFirstMetadataValue(DATE_ISSUED_FIELD);
            String yearPart = dateIssued.substring(0, Math.min(dateIssued.length(), 4));
            return Integer.parseInt(yearPart);
        } catch (Exception e) {  // NullPointerException, ParsingException ...
            return -1;
        }
    }

    /**
     * Retrieve the access type associated to the publication.
     * If possible, prefer the `accessType(context)` method to be more consistent.
     *
     * @return the access type of the publication. If the publication doesn't have any attached file or the access type
     *         cannot be determined, return `UCLouvainAccessStatusHelper.UNKNOWN`
     */
    public String accessType() {
        return accessType(this.context);
    }
    public String accessType(Context context) {
        try {
            return helper.getAccessStatusFromItem(context, item, null);
        } catch (SQLException e) {
            return UCLouvainAccessStatusHelper.UNKNOWN;
        }
    }

    /** Determine if a publication could be retired (aka withdraw) */
    public boolean isWithdrawable() {
        return true;
    }

    // PROTECTED METHODS ===============================================================================================
    /** Get the issued date of the publication; `null` if it cannot be determined */
    protected LocalDate getPublicationDateIssued() {
        String dateString = this.getFirstMetadataValue(DATE_ISSUED_FIELD);
        try {
            return DateUtils.convertDSpaceDate(dateString);
        } catch (DateConversionException dce) {
            return null;
        }
    }

    /**
     * Try to validate the publication checking the attached files regarding rules of FWB decree.
     * Only OpenAccess and small embargo date are possible for FWB decree
     *
     * @param context The DSpace application context
     * @return either a SUCCESS validation, either a specific FAILURE validation (with reason) if validation failed.
     * @throws SQLException if any database exception occurred
     */
    protected Pair<Boolean, String> validateFWBFileAccess(Context context) throws SQLException {
        LocalDate publicationDate = getPublicationDateIssued();
        String accessType = accessType(context);
        if (Objects.equals(accessType, UCLouvainAccessStatusHelper.OPEN_ACCESS)) {
            return VALIDATION_SUCCESS;
        } else if (Objects.equals(accessType, UCLouvainAccessStatusHelper.EMBARGO)) {
            // If publDate + 1 is before embargoDate, return error (the embargo end date is too high)
            boolean hasTooLongEmbargo = retrieveEmbargoPolicies(context)
                .stream()
                .map(rp -> new java.sql.Date(rp.getStartDate().getTime()).toLocalDate())
                .anyMatch(embargoDate -> publicationDate.plusYears(1).isBefore(embargoDate));
            return (hasTooLongEmbargo)
                ? VALIDATION_FAILURE_EMBARGO_DATE
                : VALIDATION_SUCCESS;
        }
        return VALIDATION_FAILURE_ACCESS_TYPE;
    }

    // PRIVATE METHODS =================================================================================================
    /**
     * Retrieve all the embargo policies form a given item's bitstreams.
     *
     * @param context The current DSpace context.
     * @return The list of all policies that have an 'embargo' type and a startDate for a given item's bitstreams.
     */
    private List<ResourcePolicy> retrieveEmbargoPolicies(Context context) {
        List<Bundle> bundles = item.getBundles(Constants.DEFAULT_BUNDLE_NAME);
        return bundles.stream()
            .map(Bundle::getBitstreams)
            .flatMap(List::stream)
            .map(bs -> getBsResourcePolicies(context, bs))
            .flatMap(List::stream)
            .filter(policy -> policy.getRpName().equals(UCLouvainAccessStatusHelper.EMBARGO))
            .filter(policy -> policy.getStartDate() != null)
            .collect(Collectors.toList());
    }
    /**
     * Get all UCLouvain resource policies from a bitstream.
     *
     * @param context The current DSpace context.
     * @param bs The bitstream to extract ResourcePolicy from.
     */
    private List<ResourcePolicy> getBsResourcePolicies(Context context, Bitstream bs) {
        try {
            return uclouvainResourcePolicyService.find(context, bs);
        } catch (SQLException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Get the metadata field string from a configuration key.
     * If no config is found for the given key, use given default value.
     *
     * @param fieldName The key of the metadata field configuration to find.
     * @param defaultValue The default value to use in case the config is not found.
     * @return The value of the config key or default value if not found.
     */
    private static String getField(String fieldName, String defaultValue) {
        return configService.getProperty("%spublication.%s.field".formatted(FIELD_PREFIX, fieldName), defaultValue);
    }
}
