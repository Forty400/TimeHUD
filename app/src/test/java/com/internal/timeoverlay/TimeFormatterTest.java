package com.internal.timeoverlay;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TimeFormatterTest {

    private final TimeFormatter formatter = new TimeFormatter();

    @Test
    public void fourFormats() {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.set(2024, Calendar.MARCH, 5, 9, 8, 7);
        cal.set(Calendar.MILLISECOND, 45);
        long t = cal.getTimeInMillis();

        String dateMillis = formatter.format(t, true, true);
        String dateSecond = formatter.format(t, true, false);
        String timeMillis = formatter.format(t, false, true);
        String timeSecond = formatter.format(t, false, false);

        assertTrue(Pattern.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}", dateMillis));
        assertTrue(Pattern.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", dateSecond));
        assertTrue(Pattern.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}", timeMillis));
        assertTrue(Pattern.matches("\\d{2}:\\d{2}:\\d{2}", timeSecond));

        assertTrue(dateMillis.endsWith(".045"));
        assertEquals(3, timeMillis.substring(timeMillis.indexOf('.') + 1).length());
    }

    @Test
    public void uses24HourClock() {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        String text = formatter.format(cal.getTimeInMillis(), false, false);
        assertTrue(text.startsWith("21:"));
    }
}
