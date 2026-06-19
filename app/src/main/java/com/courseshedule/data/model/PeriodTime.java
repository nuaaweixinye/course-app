package com.courseshedule.data.model;

/** A single class period's time window, parsed from SemesterConfig.periodTimesJson. */
public class PeriodTime {
    public final int index;       // 1-based period number
    public final int startMinutes; // minutes since midnight
    public final int endMinutes;   // minutes since midnight

    public PeriodTime(int index, int startMinutes, int endMinutes) {
        this.index = index;
        this.startMinutes = startMinutes;
        this.endMinutes = endMinutes;
    }

    public static int toMinutes(String hhmm) {
        // "HH:MM" -> minutes since midnight. Tolerant of leading zeros.
        String[] parts = hhmm.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }
}
