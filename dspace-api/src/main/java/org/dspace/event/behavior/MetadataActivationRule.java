/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.event.behavior;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;

/**
 * Class to represent an activation rule for a consumer based on DSpace object metadata.
 *   Rules will be loaded from the configuration properties.
 *   For each rule:
 *     * We must define a metadata field on which this rule should be validated
 *     * We can define a regular expression to validate the metadata value.
 *     * To separate metadata field and regular expression, we can use the "::" glue string.
 *
 *  Example:
 *     event.consumer.consumerKey.rule.enable = dc.title
 *       --> the DSpace object must define at least one `dc.title`
 *     event.consumer.consumerKey.rule.enable = dcterms.provenance::cataretro
 *       --> the Dspace object must define a `dterms.provenance` metadata field matching "cataretro" value
 *     event.consumer.consumerKey.rule.disable = custom.field::^[0-9]+$
 *       --> the Dspace object must not define a `custom.field` metadata field only composed by digits.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class MetadataActivationRule implements ConsumerActivationRule {

    String metadataField;
    Pattern metadataValidationPattern;

    public MetadataActivationRule(String rule) throws IllegalArgumentException {
        if (StringUtils.isBlank(rule)) {
            throw new IllegalArgumentException("rule cannot be empty");
        }
        // Split the rule to get metadata field and metadata regexp value
        // Next validate extracted parameters
        //   * field  :: must be a valid and existing metadata field.
        //   * regexp :: must be a valid regexp.
        metadataField = rule;
        String mdRegxp = ".*";
        if (rule.contains("::")) {
            String[] parts = rule.split("::", 2);
            metadataField = parts[0];
            mdRegxp = StringUtils.isBlank(parts[1]) ? ".*" : parts[1];
        }
        metadataValidationPattern = Pattern.compile(mdRegxp);
    }

    public boolean isValid(Context context, DSpaceObject dso) throws SQLException {
        MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
        MetadataField mdField = metadataFieldService.findByString(context, metadataField, '.');
        if (mdField == null) {
            throw new IllegalArgumentException("Cannot find '" + metadataField + "' metadata field");
        }
        DSpaceObjectService<DSpaceObject> dsoService = ContentServiceFactory.getInstance().getDSpaceObjectService(dso);
        List<MetadataValue> mdValues = dsoService.getMetadataByMetadataString(dso, mdField.toString('.'));
        return mdValues.stream().anyMatch(md -> metadataValidationPattern.matcher(md.getValue()).matches());
    }
}
