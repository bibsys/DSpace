/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.CrisConstants;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Class to represent a MasterThesis item extending DSpace `Item`
 * NOTE: This class should extend `org.content.content.Item` class; but casting an Item to MasterThesis will cause a
 *       strange `java.lang.ClassCastException`. Should be fixed in future code iteration.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class MasterThesis {

    private static final Logger log = LogManager.getLogger(MasterThesis.class);
    private final ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private Item item;

    private static final String FIELD_PREFIX = "uclouvain.global.metadata.";
    private final String authorMdField = configService.getProperty(FIELD_PREFIX + "authorname.field");
    private final String authorEmailMdField = configService.getProperty(FIELD_PREFIX + "authoremail.field");
    private final String authorInstitutionMdField = configService.getProperty(FIELD_PREFIX + "authorinstitution.field");
    private final String authorFgsMdField = configService.getProperty(FIELD_PREFIX + "id_fgs.field");
    private final String authorNomaMdField = configService.getProperty(FIELD_PREFIX + "id_noma.field");


    public MasterThesis(Item item) {
        this.item = item;
    }

    /**
     * Get authors metadata of the master thesis object.
     * @return a list of `MasterThesisAuthor`, each element corresponding to one author.
     */
    public List<MasterThesisAuthor> getAuthors() {
        if (authorMdField == null) {
            log.warn("Unable to determine `author's name` metadata field");
            return Collections.EMPTY_LIST;
        }
        return item.getMetadata()
            .stream()
            .filter(m -> m.getMetadataField().toString('.').equals(authorMdField))
            .map(this::buildAuthor)
            .collect(Collectors.toList());
    }

    private MasterThesisAuthor buildAuthor(MetadataValue mdValue) {
        MasterThesisAuthor author = new MasterThesisAuthor();
        author.name = mdValue.getValue();
        author.email = getMetadataValue(authorEmailMdField, mdValue.getPlace());
        author.institution = getMetadataValue(authorInstitutionMdField, mdValue.getPlace());
        String tmpIdentifier = getMetadataValue(authorFgsMdField, mdValue.getPlace());
        if (tmpIdentifier != null) {
            author.addIdentifier("fgs", tmpIdentifier);
        }
        tmpIdentifier = getMetadataValue(authorNomaMdField, mdValue.getPlace());
        if (tmpIdentifier != null) {
            author.addIdentifier("noma", tmpIdentifier);
        }
        return author;
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
    public String getMetadataValue(String mdField) {
        return getMetadataValue(mdField, 0);
    }

    /**
     * Get the metadata value corresponding to the metadata field with a specific place
     * @param mdField the metadata field name with '.' as separator.
     * @return the corresponding metadata value if exists; null otherwise
     */
    public String getMetadataValue(String mdField, int place) {
        return item.getMetadata().stream()
            .filter(m -> m.getMetadataField().toString('.').equals(mdField))
            .filter(m -> place <= 0 || m.getPlace() == place)
            .map(MetadataValue::getValue)
            .filter(v -> !v.equals(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE))
            .findFirst().orElse(null);
    }


}
