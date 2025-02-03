/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;

import org.dspace.uclouvain.exceptions.DateConversionException;

/** 
 * A set of utils method to work with dates in DSpace.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class DateUtils {

    protected DateUtils() {
        throw new UnsupportedOperationException();  // required by "(design) HideUtilityClassConstructor" code checker
    }

    /**
     * Convert a date string coming from an item to a LocalDate object.
     * The value comes from a metadata field using the 'date' input type form field.
     * It has 3 parts:
     * - The year (4 digits): 'uuuu',
     * - The month (2 digits) which is OPTIONAL: 'MM',
     * - The day (2 digits) which is OPTIONAL: 'dd'
     * 
     * Those 3 parts are linked by a '-' character.
     * So to summarize the format can be: 'uuuu', 'uuuu-MM' or 'uuuu-MM-dd'.
     * 
     * @param date The string representing the date coming form a date field.
     * @return The date converted in a "LocalDate" type. Can be null if the conversion failed.
     */
    public static LocalDate convertDSpaceDate(String date) throws DateConversionException {
        try {
            // Define all the available formats here as a global formatter.
            DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                .appendPattern("uuuu")
                .optionalStart()
                .appendPattern("-MM")
                .optionalStart()
                .appendPattern("-dd")
                .optionalEnd()
                .optionalEnd()
                .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .toFormatter(Locale.ENGLISH)
                .withResolverStyle(ResolverStyle.STRICT);
            // Use the formatter to parse a LocalDate object from the string.
            return LocalDate.parse(date, dateTimeFormatter);
        } catch (Exception e) {
            throw new DateConversionException(e.getMessage());
        }
    }
}
