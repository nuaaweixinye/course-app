package com.courseshedule.data.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WeekUtilsTest {

    @Test
    public void parseRange() {
        Set<Integer> weeks = WeekUtils.parse("1-16");
        assertEquals(16, weeks.size());
        for (int w = 1; w <= 16; w++) assertTrue(weeks.contains(w));
    }

    @Test
    public void parseEnumeration() {
        Set<Integer> weeks = WeekUtils.parse("1,3,5,7");
        assertEquals(4, weeks.size());
        assertTrue(weeks.containsAll(Arrays.asList(1, 3, 5, 7)));
    }

    @Test
    public void parseMixed() {
        Set<Integer> weeks = WeekUtils.parse("1-5,8,10-12");
        assertTrue(weeks.containsAll(Arrays.asList(1, 2, 3, 4, 5, 8, 10, 11, 12)));
        assertEquals(9, weeks.size());
    }

    @Test
    public void parseWhitespaceTolerant() {
        Set<Integer> weeks = WeekUtils.parse(" 1 , 2 , 3 ");
        assertEquals(3, weeks.size());
    }

    @Test
    public void parseEmptyAndNull() {
        assertTrue(WeekUtils.parse(null).isEmpty());
        assertTrue(WeekUtils.parse("").isEmpty());
        assertTrue(WeekUtils.parse("   ").isEmpty());
    }

    @Test
    public void parseSkipsGarbage() {
        // Bad tokens are skipped, not fatal. "3-" is a malformed range (no end).
        Set<Integer> weeks = WeekUtils.parse("1,x,3-");
        assertTrue(weeks.contains(1));
        assertEquals(1, weeks.size());
    }

    @Test
    public void matchesWeekRange() {
        assertTrue(WeekUtils.matchesWeek("1-16", 1));
        assertTrue(WeekUtils.matchesWeek("1-16", 16));
        assertFalse(WeekUtils.matchesWeek("1-16", 17));
    }

    @Test
    public void matchesWeekOddOnly() {
        assertTrue(WeekUtils.matchesWeek("1,3,5,7,9", 5));
        assertFalse(WeekUtils.matchesWeek("1,3,5,7,9", 6));
    }

    @Test
    public void currentWeekWithinSemester() {
        long start = 1_700_000_000_000L; // arbitrary Monday baseline
        // Exactly 3 weeks later
        int week = WeekUtils.currentWeek(start, 16, start + 3L * WeekUtils.WEEK_MILLIS);
        assertEquals(4, week); // start = week 1, +3 weeks -> week 4
    }

    @Test
    public void currentWeekClampsLow() {
        long start = 1_700_000_000_000L;
        assertEquals(1, WeekUtils.currentWeek(start, 16, start - 1000L));
    }

    @Test
    public void currentWeekClampsHigh() {
        long start = 1_700_000_000_000L;
        long far = start + 100L * WeekUtils.WEEK_MILLIS;
        assertEquals(16, WeekUtils.currentWeek(start, 16, far));
    }

    @Test
    public void oddAndEvenPresets() {
        assertEquals("1,3,5,7,9,11,13,15", WeekUtils.oddWeeks(16));
        assertEquals("2,4,6,8,10,12,14,16", WeekUtils.evenWeeks(16));
    }

    @Test
    public void fromSelectionRoundTrip() {
        List<Integer> sel = Arrays.asList(5, 1, 3, 2);
        assertEquals("1,2,3,5", WeekUtils.fromSelection(sel));
    }

    @Test
    public void expandIsSorted() {
        List<Integer> exp = WeekUtils.expand("3,1,2-4");
        assertEquals(Arrays.asList(1, 2, 3, 4), exp);
    }
}
