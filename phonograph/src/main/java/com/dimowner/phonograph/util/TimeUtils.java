package com.dimowner.phonograph.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

public class TimeUtils {

    /** Date format: 2019.09.22 11:30 */
    @NonNull
    private static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy.MM.dd HH.mm.ss", Locale.getDefault());

    @NonNull
    static String formatDateForName(long time) {
        return dateTimeFormat.format(new Date(time));
    }

    @NonNull
    public static String formatTimeIntervalMinSec(long length) {
        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        long numMinutes = timeUnit.toMinutes(length);
        long numSeconds = timeUnit.toSeconds(length);
        return String.format(Locale.getDefault(), "%02d:%02d", numMinutes, numSeconds % 60);
    }


    /** Convert time in milliseconds to hh:mm:ss in case of > 1 hour, mm:ss otherwise
     *
     * @param length the amount of time in milliseconds
     * @return hh:mm:ss in case of > 1 hour, mm:ss otherwise
     */
    @NonNull
    public static String formatTimeIntervalHourMinSec2(long length) {
        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        long numHour = timeUnit.toHours(length);
        long numMinutes = timeUnit.toMinutes(length);
        long numSeconds = timeUnit.toSeconds(length);
        if (numHour == 0) {
            return String.format(Locale.getDefault(), "%02d:%02d", numMinutes, numSeconds % 60);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", numHour, numMinutes % 60, numSeconds % 60);
        }
    }
}
