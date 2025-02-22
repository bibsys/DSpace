/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mail;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service to parse some metadata of an item into something that can be used in a mail.
 * This service is configured using instances of {@link MailMetadataParser} in 'mail-metadata-parser.xml'.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class MailMetadataParserServiceImpl implements MailMetadataParserService {

    private final Logger logger = LogManager.getLogger(MailMetadataParserServiceImpl.class);

    private List<MailMetadataParser> parsers;

    @Autowired
    ChoiceAuthorityService choiceAuthorityService;
    @Autowired
    ItemService itemService;

    /**
     * Parse a given metadata for the given item into a pair.
     * The pair contains the label of the field and the actual field value(s).
     * 
     * @param context The current DSpace context.
     * @param item The item to extract metadata from.
     * @param metadataField The metadata field to extract from the item.
     * @param lang The language used to generate the label.
     * @return A {@link Pair} object containing both the label and the value (Pair<label, value>).
     */
    public Pair<String, String> parseMetadata(Context context, Item item, String metadataField, String lang) {
        // Retrieve the corresponding parser for the given metadata field.
        MailMetadataParser parser = getParserForMetadataField(metadataField);
        if (parser == null) {
            logger.warn("Could not retrieve metadata parser for metadata field: [" + metadataField + "]");
            return null;
        }

        // Return a pair of label and value using the provided language.
        return Pair.of(
            Optional.ofNullable(parser.getLabel(lang)).orElse(metadataField),
            getValue(context, item, parser, metadataField)
        );
    }

    /**
     * Parse a list of given metadata for the given item into a map of labels and values.
     * 
     * @param context The current DSpace context.
     * @param item The item to extract values from.
     * @param metadataFields The fields to extract from the item.
     * @param lang The language used to generate the labels.
     * @return A map containing all the labels and values for the given metadata fields.
     * The map is of type {@link LinkedHashMap} which allows to keep the order of the metadataFields list.
     */
    public LinkedHashMap<String, String> parseMetadata(
        Context context, Item item, List<String> metadataFields, String lang
    ) {
        return metadataFields.stream()
            .map(field -> parseMetadata(context, item, field, lang))
            .filter(Objects::nonNull)
            // Convert from List<Pair<String, String>> to LinkedHashMap<String, String>
            .collect(Collectors.toMap(
                Pair::getLeft,
                Pair::getRight,
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ));
    }

    /**
     * Retrieve the correct parser for the given metadata field name.
     * @param metadataField The metadata field name of form '<schema>.<element>.<qualifier>'.
     * @return The parser configured to handle the metadata field or null if not found.
     */
    private MailMetadataParser getParserForMetadataField(String metadataField) {
        return parsers.stream()
            .filter(parser -> parser.getMetadataField().equals(metadataField))
            .findFirst()
            .orElse(null);
    }

    /**
     * Get the values for the given item, metadata field and parser.
     * 
     * @param context The current DSpace context.
     * @param item The item to extract initial values from.
     * @param parser The parser used to parse the values.
     * @param metadataField The metadata field to extract values from the item.
     * @return Returns a parsed value. Can return an empty string if no values were found for the given metadata field.
     */
    private String getValue(Context context, Item item, MailMetadataParser parser, String metadataField) {
        List<String> values = itemService.getMetadataByMetadataString(item, metadataField).stream()
            .map(MetadataValue::getValue).collect(Collectors.toList());
        if (values.isEmpty()) {
            // Return an empty string if no values were found.
            return "";
        }

        // If multipleValues flag is false, just get the first metadata value.
        if (!parser.getMultipleValues()) {
            values = Arrays.asList(values.get(0));
        }
        // If we have a configured vocabulary name, we need to try to convert the values.
        if (parser.getVocabularyName() != null) {
            values = convertVocabularyValues(context, values, parser);
        }
        return String.join(parser.getSeparator(), values);
    }

    /**
     * If the parser has a configured controlled vocabulary name, use it to convert the values extracted from the item.
     * 
     * @param context The current DSpace context.
     * @param values The values extracted from the item.
     * @param parser The parser that holds the name of the controlled vocabulary.
     * @return A list of transformed values. NOTE: If a value has no equivalent for the provided controlled vocabulary,
     * it as is in the final list.
     */
    private List<String> convertVocabularyValues(Context context, List<String> values, MailMetadataParser parser) {
        ChoiceAuthority choiceAuthority =
            choiceAuthorityService.getChoiceAuthorityByAuthorityName(parser.getVocabularyName());
        if (choiceAuthority == null) {
            logger.warn(
                "Could not retrieve vocabulary from parser configuration: [" + parser.getVocabularyName() + "]"
            );
            return values;
        }

        // Try to transform to the vocabulary label. If transformation failed, keep the initial value.
        return values.stream()
            .map(value -> Optional.ofNullable(extractVocabularyLabel(context, value, choiceAuthority)).orElse(value))
            .collect(Collectors.toList());
    }

    /**
     * Get the label for the given value using the choice authority of the controlled vocabulary.
     * 
     * @param context The current DSpace context.
     * @param value The value to convert.
     * @param ca The {@link ChoiceAuthority} of the controlled vocabulary used to get the label from the value.
     */
    private String extractVocabularyLabel(Context context, String value, ChoiceAuthority ca) {
        Choice choice = Arrays.stream(ca.getBestMatch(value, context.getCurrentLocale().toString()).values)
            .findFirst()
            .orElse(null);
        return (choice != null) ? choice.label : null;
    }

    // SETTERS && GETTERS.
    public List<MailMetadataParser> getParsers() {
        return parsers;
    }

    public void setParsers(List<MailMetadataParser> parsers) {
        this.parsers = parsers;
    }
}
