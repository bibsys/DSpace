package org.dspace.uclouvain.core.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.dspace.services.factory.DSpaceServicesFactory;

/** 
 * A set of utils method to work with dates in DSpace.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class DateUtils {

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

    public int getCurrentAcademicYear() {
        int monthNumber = this.getCurrentMonthNumber();
        int currentYear = this.getCurrentYear();
        return (monthNumber < 12) ? currentYear - 1 : currentYear;
    }

    public int getCurrentMonthNumber() {
        LocalDateTime currentDate = LocalDateTime.now();
        return currentDate.getMonthValue();
    }

    public int getCurrentYear() {
        LocalDateTime currentDate = LocalDateTime.now();
        return currentDate.getYear();
    }
}
