package com.courseshedule.data.model;

/** Join result of one session with its parent course's display fields. */
public class SessionWithCourse {
    public long sessionId;
    public long courseId;
    public int dayOfWeek;
    public int startPeriod;
    public int endPeriod;
    public String location;
    public String weekPattern;
    public String courseName;
    public String teacher;
    public int colorTag;
    public long semesterId;
    public Long timetableId;
}
