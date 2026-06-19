package com.courseshedule.data.imports;

import org.junit.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IcsScheduleParserTest {

    private static final String DEFAULTS = com.courseshedule.data.model.PeriodUtils.DEFAULT_PERIOD_TIMES_JSON;

    @Test
    public void parsesWeeklyRepeatingEvent() throws ImportException {
        String ics = "BEGIN:VCALENDAR\n"
                + "BEGIN:VEVENT\n"
                + "SUMMARY:高等数学\n"
                + "DTSTART:20260907T080000\n"  // a Monday at 08:00
                + "RRULE:FREQ=WEEKLY;COUNT=16\n"
                + "LOCATION:教三301\n"
                + "END:VEVENT\n"
                + "END:VCALENDAR\n";
        List<ParsedCourse> courses = new IcsScheduleParser(new StringReader(ics), DEFAULTS).fetch();
        assertEquals(1, courses.size());
        ParsedCourse math = courses.get(0);
        assertEquals("高等数学", math.name);
        assertEquals(1, math.sessions.size());
        // 08:00 maps to period 1 in the default table.
        assertEquals(1, math.sessions.get(0).startPeriod);
        assertEquals("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16",
                math.sessions.get(0).weekPattern);
        assertEquals("教三301", math.sessions.get(0).location);
    }

    @Test
    public void noRruleDefaultsToAllWeeks() throws ImportException {
        String ics = "BEGIN:VCALENDAR\n"
                + "BEGIN:VEVENT\n"
                + "SUMMARY:单次课\n"
                + "DTSTART:20260908T100000\n" // Tuesday 10:00
                + "END:VEVENT\n"
                + "END:VCALENDAR\n";
        List<ParsedCourse> courses = new IcsScheduleParser(new StringReader(ics), DEFAULTS).fetch();
        assertEquals("1-16", courses.get(0).sessions.get(0).weekPattern);
        assertEquals(2, courses.get(0).sessions.get(0).dayOfWeek); // Tuesday
    }

    @Test
    public void malformedLinesIgnoredGracefully() throws ImportException {
        String ics = "BEGIN:VCALENDAR\n"
                + "GARBAGE\n"
                + "BEGIN:VEVENT\n"
                + "SUMMARY:测试\n"
                + "DTSTART:20260909T140000\n"
                + "END:VEVENT\n"
                + "END:VCALENDAR\n";
        List<ParsedCourse> courses = new IcsScheduleParser(new StringReader(ics), DEFAULTS).fetch();
        assertEquals(1, courses.size());
        assertTrue(courses.get(0).sessions.size() >= 1);
    }
}
