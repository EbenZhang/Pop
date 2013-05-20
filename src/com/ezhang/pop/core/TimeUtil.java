package com.ezhang.pop.core;

import java.util.Calendar;
import java.util.Date;

public class TimeUtil {

    public static int GetCurDay() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        return cal.get(Calendar.DAY_OF_MONTH);
    }
}