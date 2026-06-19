package com.courseshedule.data.imports;

/** A parsed session (one weekly time slot) for a ParsedCourse. */
public class ParsedSession {
    public int dayOfWeek;   // 1..7
    public int startPeriod; // 1-based
    public int endPeriod;   // >= startPeriod
    public String location;
    public String weekPattern;

    public ParsedSession() {}

    public ParsedSession(int dayOfWeek, int startPeriod, int endPeriod,
                         String location, String weekPattern) {
        this.dayOfWeek = dayOfWeek;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
        this.location = location;
        this.weekPattern = weekPattern;
    }
}
