package com.courseshedule.data.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PeriodUtilsTest {

    private static final String JSON =
            "[{\"start\":\"08:00\",\"end\":\"08:45\"}," +
            " {\"start\":\"08:55\",\"end\":\"09:40\"}," +
            " {\"start\":\"10:00\",\"end\":\"10:45\"}]";

    @Test
    public void parseReturnsOrderedPeriods() {
        List<PeriodTime> periods = PeriodUtils.parse(JSON);
        assertEquals(3, periods.size());
        assertEquals(1, periods.get(0).index);
        assertEquals(480, periods.get(0).startMinutes); // 08:00
        assertEquals(525, periods.get(0).endMinutes);   // 08:45
        assertEquals(3, periods.get(2).index);
    }

    @Test
    public void parseEmptyAndMalformed() {
        assertTrue(PeriodUtils.parse(null).isEmpty());
        assertTrue(PeriodUtils.parse("").isEmpty());
        assertTrue(PeriodUtils.parse("not json").isEmpty());
    }

    @Test
    public void findCurrentPeriodInProgress() {
        List<PeriodTime> periods = PeriodUtils.parse(JSON);
        // 08:20 is inside period 1
        assertEquals(1, PeriodUtils.findCurrentPeriod(periods, 8 * 60 + 20));
    }

    @Test
    public void findCurrentPeriodBetweenReturnsNext() {
        List<PeriodTime> periods = PeriodUtils.parse(JSON);
        // 09:00 is between p1 and p2 -> next is p2
        assertEquals(2, PeriodUtils.findCurrentPeriod(periods, 9 * 60));
    }

    @Test
    public void findCurrentPeriodAfterAll() {
        List<PeriodTime> periods = PeriodUtils.parse(JSON);
        assertEquals(-1, PeriodUtils.findCurrentPeriod(periods, 23 * 60));
    }

    @Test
    public void findCurrentPeriodBeforeAll() {
        List<PeriodTime> periods = PeriodUtils.parse(JSON);
        // 07:00 before first -> next is p1
        assertEquals(1, PeriodUtils.findCurrentPeriod(periods, 7 * 60));
    }

    @Test
    public void toJsonRoundTrip() {
        List<PeriodTime> periods = Arrays.asList(
                new PeriodTime(1, 480, 525),
                new PeriodTime(2, 535, 580));
        String json = PeriodUtils.toJson(periods);
        List<PeriodTime> back = PeriodUtils.parse(json);
        assertEquals(2, back.size());
        assertEquals(480, back.get(0).startMinutes);
        assertEquals(580, back.get(1).endMinutes);
    }

    @Test
    public void defaultTimesHasTwelvePeriods() {
        List<PeriodTime> periods = PeriodUtils.parse(PeriodUtils.DEFAULT_PERIOD_TIMES_JSON);
        assertEquals(12, periods.size());
    }
}
