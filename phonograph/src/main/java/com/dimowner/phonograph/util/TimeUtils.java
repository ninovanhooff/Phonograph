package com.dimowner.phonograph.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    /** Date format: 2019.09.22 11:30 */
    private static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy.MM.dd HH.mm.ss", Locale.getDefault());

    static String formatDateForName(long time) {
        return dateTimeFormat.format(new Date(time));
    }


}
