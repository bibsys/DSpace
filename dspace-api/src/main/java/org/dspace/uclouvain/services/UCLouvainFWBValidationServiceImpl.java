/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.access.status.AccessStatusHelper;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.model.FWBValidation;
import org.dspace.uclouvain.core.utils.DateUtils;
import org.dspace.uclouvain.exceptions.DateConversionException;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.plugins.UCLouvainAccessStatusHelper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Main service to evaluate the FWB eligibility of an item and its compliance.
 * See {@link https://gallilex.cfwb.be/sites/default/files/imports/45142_000.pdf} for the full FWB decree.
 * 
 * IMPORTANT: The `isFWBEligible()` method should be executed first to check for the eligibility
 * of the item before executing `isFWBCompliant()`.
 * 
 * @author: Michaël Pourbaix <michael.pourbaix@ucouvain.be>
 */
public class UCLouvainFWBValidationServiceImpl implements UCLouvainFWBValidationService {
    // ERROR CODES
    public static final String ERROR_VALIDATION_FWB_WRONG_EMBARGO_DATE = "error.validation.fwb.wrongembargodate";
    public static final String ERROR_VALIDATION_FWB_NO_FILE = "error.validation.fwb.nofile";
    public static final String ERROR_VALIDATION_FWB_ACCESS_TYPE = "error.validation.fwb.accesstype";

    // The year from which the decree is applicable.
    public static final Integer DECREE_YEAR = 2018;
    public static final List<String> ACCEPTED_ENTITY_TYPES = Arrays.asList("Publication");

    // METADATA CONFIG
    protected ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private String dateIssuedField = configService.getProperty(
        "uclouvain.global.metadata.dateissued.field"
    );
    private String mainTypeField = configService.getProperty(
        "uclouvain.global.metadata.maintype.field"
    );
    private String subTypeField = configService.getProperty(
        "uclouvain.global.metadata.subtype.field"
    );
    private String publicationStatusField = configService.getProperty(
        "uclouvain.global.metadata.publicationstatus.field"
    );
    private String hostTitleField = configService.getProperty(
        "uclouvain.global.metadata.hosttitle.field"
    );
    private String journalField = configService.getProperty(
        "uclouvain.global.metadata.journal.field"
    );

    // ACCEPTED CONDITIONS
    private final String journalArticleType = "text::journal-article";
    private final String conferenceSpeechType = "text::conference-speech";
    // Valid publication status to be eligible.
    private final List<String> validPublicationStatus = Arrays.asList(
        "accepted/in-press",
        "published"
    );
    // If one of those subtype is present for an article, it is not eligible.
    private final List<String> invalidJournalArticleSubtype = Arrays.asList(
        "popularising-article",
        "full-issue"
    );
    // Those are the fields that needs to be present for an conference speech to be eligible.
    private final List<String> validConferenceField = Arrays.asList(
        hostTitleField,
        journalField
    );

    @Autowired
    private ItemService itemService;
    // With out this autowired, the factory is instantiated after this class which causes a null pointer exception.
    @Autowired
    private UCLouvainServiceFactory uclouvainServiceFactory;

    private UCLouvainResourcePolicyService uclouvainResourcePolicyService;
    private AccessStatusHelper helper;
    private final Logger logger = LogManager.getLogger(UCLouvainFWBValidationServiceImpl.class);

    /**
     * We need to use a post construct here to guarantee that the UCLouvainFactory is initialized when using it.
     */
    @PostConstruct
    private void init() {
        uclouvainResourcePolicyService = uclouvainServiceFactory.getResourcePolicyService();
        helper = (AccessStatusHelper) CoreServiceFactory
            .getInstance()
            .getPluginService()
            .getSinglePlugin(AccessStatusHelper.class);
    }

    /**
     * Check if the document is eligible for FWB check.
     * The document is eligible if:
     * 
     * - It is of one of the types defined in ${ACCEPTED_ENTITY_TYPES}
     * 
     * - The issued date year is ${DECREE_YEAR} or more
     * 
     * - The document is a journal article and:
     *      -> the status is 'accepted' or 'published'
     *      -> the subtype is NOT 'Popularising article' or 'Full issue'
     * 
     * - The document is a conference speech and:
     *      -> it is published in a book or a periodical article (see document host info)
     * 
     * @param context The current DSpace context.
     * @param item The item to check the eligibility of.
     */
    @Override
    public boolean isFWBEligible(Context context, Item item) {
        LocalDate pubDate;

        String entityType = itemService.getEntityType(item);
        if (!ACCEPTED_ENTITY_TYPES.contains(entityType)) {
            logger.debug("Not eligible: Entity type not handled.");
            return false;
        }

        String dateString = getFirstMetadataValue(context, item, dateIssuedField);
        try {
            pubDate = DateUtils.convertDSpaceDate(
                dateString
            );
        } catch (DateConversionException dce) {
            logger.warn("Could not convert date for given item: " + item.getID() + ". Date string was: " + dateString);
            return false;
        }

        Integer year = pubDate.getYear();
        if (year == null || year < DECREE_YEAR) {
            logger.debug("Not eligible: Date was null or before decree year.");
            return false;
        }

        String type = getFirstMetadataValue(context, item, mainTypeField);
        if (type == null || type.isEmpty()) {
            logger.debug("Not eligible: Publication type was null or empty.");
            return false;
        }

        // Check the publication type.
        switch (type) {
            case journalArticleType:
                return isJournalArticleEligible(context, item);
            case conferenceSpeechType:
                return isConferenceSpeechEligible(context, item);
            default:
                logger.debug("Not eligible: Publication type is not handled.");
                return false;
        }
    }

    /**
     * Check the eligibility of the given journal article item.
     * @param context The current DSpace context.
     * @param item The journal article item to check.
     * @return true if eligible, else false.
     */
    private boolean isJournalArticleEligible(Context context, Item item) {
        String publicationStatus = getFirstMetadataValue(context, item, publicationStatusField);
        String subType = getFirstMetadataValue(context, item, subTypeField);
        if (subType == null || subType.isEmpty()) {
            logger.debug("Not eligible: Publication type was journal-article but sub type was null or empty.");
            return false;
        }
        return !invalidJournalArticleSubtype.contains(subType)
            && validPublicationStatus.contains(publicationStatus);
    }

    /**
     * Check the eligibility of the given conference speech item.
     * @param context The current DSpace context.
     * @param item The conference speech item to check.
     * @return true if the item is linked to either a host document or a journal, else false.
     */
    private boolean isConferenceSpeechEligible(Context context, Item item) {
        return hasValueForGivenFields(context, item, validConferenceField);
    }

    /** 
     * Check that the document is compliant to the FWB conditions.
     * 
     * The document is compliant if:
     * - It has at least one file which is:
     *  - in open access or
     *  - in embargo with a date until maximum 1 year after the published date.
     * 
     * @param context The current DSpace context.
     * @param item The item to validate.
     * @return A {@link FWBValidation} object that gives the validation stage and a possible error message.
     */
    @Override
    public FWBValidation isFWBCompliant(Context context, Item item) {
        try {
            if (!itemService.hasUploadedFiles(item)) {
                return validationError(ERROR_VALIDATION_FWB_NO_FILE);
            }
            String accessType = helper.getAccessStatusFromItem(context, item, null);
            if (accessType.equals(UCLouvainAccessStatusHelper.OPEN_ACCESS)) {
                return validationSuccess();
            } else if (accessType.equals(UCLouvainAccessStatusHelper.EMBARGO)) {
                LocalDate pubDate;
                String dateString = getFirstMetadataValue(context, item, dateIssuedField);
                try {
                    pubDate = DateUtils.convertDSpaceDate(
                        dateString
                    );
                } catch (DateConversionException dce) {
                    logger.warn(
                        "Could not validate FWB compliance: Could not convert the date of the item: " + item.getID()
                        + ". Given date string was: " + dateString
                    );
                    return validationSuccess();
                }

                // Loop over all valid embargo policies.
                for (ResourcePolicy rp: retrieveEmbargoPolicies(context, item)) {
                    // We need to use SQL Date since it is returned by the database.
                    Date startDate = new Date(rp.getStartDate().getTime());
                    LocalDate embargoDate = startDate.toLocalDate();
                    // If publDate + 1 is before embargoDate, return error.
                    if (pubDate.plusYears(1).isBefore(embargoDate)) {
                        // In this case the embargo end date is too high.
                        return validationError(ERROR_VALIDATION_FWB_WRONG_EMBARGO_DATE);
                    }
                }
                return validationSuccess();
            }
            return validationError(ERROR_VALIDATION_FWB_ACCESS_TYPE);
        } catch (Exception e) {
            // If an error occurres while checking for compliance we cannot block the user.
            // We have to log the error for investigation and return a success state.
            logger.warn("Could not check for FWB compliance of item: " + item.getID(), e);
            return validationSuccess();
        }
    }

    /** 
     * Check that the document is compliant to the FWB conditions and return a boolean.
     * 
     * @param context The current DSpace context.
     * @param item The item to check the compliance of.
     */
    @Override
    public boolean isFWBCompliantAsBoolean(Context context, Item item) {
        return isFWBCompliant(context, item).isValid;
    }

    /**
     * Return a FWBValidation object with 'isValid' property to false and the given error message.
     * 
     * @param errorMessage The path to the desired custom message to send to the user.
     */
    private FWBValidation validationError(String errorMessage) {
        return new FWBValidation(false, errorMessage);
    }

    /** Return a FWBValidation object with 'isValid' property to true. */
    private FWBValidation validationSuccess() {
        return new FWBValidation(true);
    }

    /** 
     * Checks that the given item has a metadata value for at least one of the given metadata fields.
     * 
     * @param context The current DSpace context.
     * @param item The item to check.
     * @param fields The metadata fields used for the check. The item must have a a value for at
     * least one of those fields.
     */
    private boolean hasValueForGivenFields(Context context, Item item, List<String> fields) {
        return fields.stream()
            .map(field -> itemService.getMetadataByMetadataString(item, field))
            .anyMatch(mvs -> mvs != null && !mvs.isEmpty());
    }

    /**
     * Retrieve all the embargo policies form a given item's bitstreams.
     * 
     * @param context The current DSpace context.
     * @param item The item to extract policies from.
     * @return The list of all policies that have an 'embargo' type and a startDate for a given item's bitstreams.
     */
    private List<ResourcePolicy> retrieveEmbargoPolicies(Context context, Item item) {
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
            logger.warn(
                "An error occurred while retrieving the valid policies for given bitstream with id: " + bs.getID(),
                e
            );
            return Collections.emptyList();
        }
    }

    /** 
     * Util method to extract the first metadata value from a metadata field.
     *
     * @param context The current DSpace context.
     * @param item The item to extract the metadata value from.
     * @param metadataField The metadata field to extract the value of.
     * @return The first metadata value for the given field or null if nothing found.
     */
    private String getFirstMetadataValue(Context context, Item item, String metadataField) {
        try {
            return itemService.getMetadataByMetadataString(item, metadataField)
                .stream()
                .findFirst()
                .map(MetadataValue::getValue)
                .orElse(null);
        } catch (Exception e) {
            logger.error(
                "Could not extract first metadata value from item: "
                + item.getID() + " for metadata field: " + metadataField,
                e
            );
            return null;
        }
    }
}
