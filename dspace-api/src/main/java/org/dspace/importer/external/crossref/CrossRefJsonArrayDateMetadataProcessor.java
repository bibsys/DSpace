/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.crossref;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Processor extending {@link CrossRefJsonArrayMetadataProcessor} that can be used to extract dates.
 * Given an inputFormat and outputFormat, the processor will search for the desired date and convert
 * it to the given output format.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class CrossRefJsonArrayDateMetadataProcessor extends CrossRefJsonArrayMetadataProcessor {
    protected String inputFormat;
    protected String outputFormat;

    /**
     * Keep the parent behavior but convert the results using the convertDates method.
     * processValues(node) -> Collection of values -> convertDate(values) -> Collection of converted values
    */
    @Override
    protected Collection<String> processValues(JsonNode node) {
        Collection<String> dates = super.processValues(node);
        // Retrieve the dates as strings from the node
        return convertDates(dates, inputFormat, outputFormat);
    }

    /**
     * Converts the dates from given inputFormat to outputFormat.
     * 
     * @param dates A collection of date strings.
     * @param inputFormat The format of the given dates.
     * @param outputFormat The format to convert the dates into.
     * @param locale The language used in the given date strings.
     * @return The converted form of the dates following the given output format.
     */
    protected static Collection<String> convertDates(
            Collection<String> dates, String inputFormat, String outputFormat
    ) {
        // Define formatters based on the given formats.
        // We need to use the english local in order to decode whats coming from crossref
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(inputFormat, Locale.ENGLISH);
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(outputFormat, Locale.ENGLISH);
        // Convert the date strings.
        return dates.stream().map((String date) -> {
            LocalDate parseDate = LocalDate.parse(date, inputFormatter);
            return parseDate.format(outputFormatter);
        }).collect(Collectors.toList());
    }

    // SETTERS FOR ATTRIBUTES
    public void setInputFormat(String format) {
        this.inputFormat = format;
    }

    public void setOutputFormat(String format) {
        this.outputFormat = format;
    }
}
