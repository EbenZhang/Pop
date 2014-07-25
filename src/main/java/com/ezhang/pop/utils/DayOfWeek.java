package com.ezhang.pop.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by eben on 13-6-9.
 */
public class DayOfWeek {
    public static String Get(Date date) {
        return new SimpleDateFormat("EE").format(date);
    }
}
