package com.internal.timeoverlay;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Device local time, current time zone, 24-hour clock. Millis always 3 digits.
 */
public final class TimeFormatter {

    private final Calendar calendar = Calendar.getInstance();

    public String format(long epochMillis, boolean showDate, boolean showMillis) {
        calendar.setTimeZone(TimeZone.getDefault());
        calendar.setTimeInMillis(epochMillis);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millis = (int) (epochMillis % 1000);

        if (showDate && showMillis) {
            return String.format(Locale.US,
                    "%04d-%02d-%02d %02d:%02d:%02d.%03d",
                    year, month, day, hour, minute, second, millis);
        }
        if (showDate) {
            return String.format(Locale.US,
                    "%04d-%02d-%02d %02d:%02d:%02d",
                    year, month, day, hour, minute, second);
        }
        if (showMillis) {
            return String.format(Locale.US,
                    "%02d:%02d:%02d.%03d",
                    hour, minute, second, millis);
        }
        return String.format(Locale.US,
                "%02d:%02d:%02d",
                hour, minute, second);
    }
}
