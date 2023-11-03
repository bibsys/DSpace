/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.utils;

import java.time.LocalDateTime;


public class DateUtils {

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
