package org.dspace.core.utils;

import java.time.LocalDateTime;


public class DateUtils {
    
    public int getCurrentAcademicYear(){
        int monthNumber = this.getCurrentMonthNumber();
        int currentYear = this.getCurrentYear();
        return (monthNumber < 12) ? currentYear - 1 : currentYear;
    }

    public int getCurrentMonthNumber(){
        LocalDateTime currentDate = LocalDateTime.now();
        return currentDate.getMonthValue();
    }

    public int getCurrentYear(){
        LocalDateTime currentDate = LocalDateTime.now();
        return currentDate.getYear();
    }
}
