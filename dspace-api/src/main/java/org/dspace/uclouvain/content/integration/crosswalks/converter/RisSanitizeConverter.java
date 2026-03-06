/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.integration.crosswalks.converter;

import java.util.Optional;

import org.springframework.core.convert.converter.Converter;

/**
 * Converter to escape the miscellaneous character for a RIS export.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class RisSanitizeConverter implements Converter<String, String> {

    @Override
    public String convert(String value) {
        return Optional.ofNullable(value)
            .map(this::removeHtml)
            .map(this::replaceNewlines)
            .map(this::normalizeSpaces)
            .orElse("");
    }

    private String removeHtml(String input) {
        // Remove all HTML tags using regex
        return input.replaceAll("<[^>]*>", "");
    }

    private String replaceNewlines(String input) {
        // Replace CR/LF/N with a single space to avoid breaking RIS line structure
        return input.replaceAll("\\r\\n|\\r|\\n", " ");
    }

    private String normalizeSpaces(String input) {
        // Replace multiple spaces with one and trim
        return input.replaceAll("\\s+", " ").trim();
    }

}
