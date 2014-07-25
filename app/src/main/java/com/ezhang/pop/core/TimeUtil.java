package com.ezhang.pop.core;

import java.util.Calendar;
import java.util.Date;

public class TimeUtil {

    public static int GetCurDay() {
        return GetDayFromDate(new Date());
    }
    public static int GetDayFromDate(Date date){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    public static float DaysBetween(Date date1, Date date2) {
        long mSec = date2.getTime() - date1.getTime();
        long millsPerDay = 1000 * 60 * 60 * 24;
        return (float)mSec / (float)millsPerDay;
    }

    public static Date Add(Date base, int days){
        Calendar cal = Calendar.getInstance();
        cal.setTime(base);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

}