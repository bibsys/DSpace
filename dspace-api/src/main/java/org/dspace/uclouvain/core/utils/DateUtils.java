/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.exceptions.DateConversionException;

/** 
 * A set of utils method to work with dates in DSpace.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class DateUtils {

    public static final String DSPACE_FORMAT = "yyyy-MM-dd";

    private static final List<DateTimeFormatter> INPUT_FORMATS = List.of(
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MM-dd-yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy MMMM dd", Locale.ENGLISH)
    );

    protected DateUtils() {
        throw new UnsupportedOperationException();  // required by "(design) HideUtilityClassConstructor" code checker
    }

    /**
     * Returns the current date and time in the local timezone, formatted with the given pattern.
     * The timezone is defined by the "uclouvain.local.timezone" property in dspace.cfg
     * (defaults to Europe/Brussels: UTC+1 in winter, UTC+2 in summer).
     *
     * @param format the pattern used to format the date (e.g. "dd/MM/yyyy - HH:mm:ss").
     * @return the formatted current date and time.
     */
    public static String getLocaleDateString(String format) {
        String zoneId = DSpaceServicesFactory
            .getInstance()
            .getConfigurationService()
            .getProperty("uclouvain.local.timezone", "Europe/Brussels");
        return ZonedDateTime.now(ZoneId.of(zoneId)).format(DateTimeFormatter.ofPattern(format));
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

    /**
     * Converts a date of the given format into a well-formed dspace date.
     * @param date The date to convert to DSpace format.
     * @param inputFormat The format of the given date.
     * @return The converted date into a DSpace friendly format.
     * @throws ParseException
     */
    public static String toDSpaceDate(String date, String inputFormat) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(inputFormat);
        Date inputDate = sdf.parse(date);
        sdf.applyPattern(DSPACE_FORMAT);
        return sdf.format(inputDate);
    }

    /**
     * Try to convert a given date string into a given format.
     * We try to guess the format of the given date string in order to convert it.
     * If no format could be guessed, return null.
     * 
     * @param date The date string to convert.
     * @param outputFormat The format to convert the date string into.
     * @return The converted date string or null if no matching format found for the given date string.
     */
    public static String convertDateString(String date, String outputFormat) {
        if (date == null) {
            return null;
        }
        for (DateTimeFormatter formatter : INPUT_FORMATS) {
            try {
                LocalDate localDate = LocalDate.parse(date, formatter);
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(outputFormat);
                return localDate.format(outputFormatter);
            } catch (DateTimeParseException ex) {
                continue;
            }
        }
        return null;
    }
}
