/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.journals;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * This model represents a journal object.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class Journal {

    public static final String JOURNAL_ENTITY_TYPE = "Journal";
    public static final String JOURNAL_ACTIVE_ACCESS_TYPE = "Active";
    public static final String JOURNAL_CEASED_ACCESS_TYPE = "Ceased";

    private static final ConfigurationService configService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    private Item item;

    public static final String FIELD_PREFIX = "uclouvain.global.metadata.";
    public static final String titleField = configService.getProperty(FIELD_PREFIX + "title.field");
    public static final String issnField = configService.getProperty(FIELD_PREFIX + "journalissn.field");
    public static final String eissnField = configService.getProperty(FIELD_PREFIX + "journaleissn.field");
    public static final String publisherField = configService.getProperty(FIELD_PREFIX + "journalpublisher.field");
    public static final String publisherLocationField =
        configService.getProperty(FIELD_PREFIX + "journalpublisherlocation.field");
    public static final String peerReviewedField =
        configService.getProperty(FIELD_PREFIX + "journalpeerreviewed.field");
    public static final String statusCodeField = configService.getProperty(FIELD_PREFIX + "journalstatuscode.field");

    public Journal(Item item) {
        this.item = item;
    }

    /**
     * Get a list of metadata values corresponding to the metadata field.
     * @param mdField the metadata field name with '.' as separator.
     * @return the list of corresponding values (returning at least an empty list)
     */
    public List<String> getMetadataValues(String mdField) {
        return item.getMetadata().stream()
            .filter(m -> m.getMetadataField().toString('.').equals(mdField))
            .map(MetadataValue::getValue)
            .collect(Collectors.toList());
    }

    /**
     * Get the first found metadata value corresponding to the metadata field.
     * @param mdField the metadata field name with '.' as separator.
     * @return the corresponding metadata value if exists; null otherwise
     */
    private String getFirstMetadataValue(String mdField) {
        return item.getMetadata().stream()
            .filter(m -> m.getMetadataField().toString('.').equals(mdField))
            .map(MetadataValue::getValue)
            .findFirst()
            .orElse(null);
    }

    public Item getItem() {
        return this.item;
    }

    public String getTitle() {
        return getFirstMetadataValue(titleField);
    }

    public String getIssn() {
        return getFirstMetadataValue(issnField);
    }

    public String getEissn() {
        return getFirstMetadataValue(eissnField);
    }

    public String getPublisher() {
        return getFirstMetadataValue(publisherField);
    }

    public String getPublisherLocation() {
        return getFirstMetadataValue(publisherLocationField);
    }

    public boolean isPeerReviewed() {
        return Boolean.valueOf(getFirstMetadataValue(peerReviewedField));
    }

    public String getPeerReviewed() {
        return getFirstMetadataValue(peerReviewedField);
    }

    public String getStatusCode() {
        return getFirstMetadataValue(statusCodeField);
    }
}