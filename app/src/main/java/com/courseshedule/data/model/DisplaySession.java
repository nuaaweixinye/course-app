package com.courseshedule.data.model;

import com.courseshedule.data.local.entity.SessionExceptionEntity;

/**
 * A session prepared for grid rendering: the base session fields plus its
 * parent course's display attributes and any week-exception resolution applied
 * by the repository/render layer.
 */
public class DisplaySession {
    public final long sessionId;
    public final long courseId;
    public final String courseName;
    public final String teacher;
    public final String location;
    public final int colorTag;
    public final int dayOfWeek;        // possibly overridden by a MOVED exception
    public final int startPeriod;
    public final int endPeriod;
    public final String weekPattern;

    public DisplaySession(long sessionId, long courseId, String courseName, String teacher,
                          String location, int colorTag, int dayOfWeek,
                          int startPeriod, int endPeriod, String weekPattern) {
        this.sessionId = sessionId;
        this.courseId = courseId;
        this.courseName = courseName;
        this.teacher = teacher;
        this.location = location;
        this.colorTag = colorTag;
        this.dayOfWeek = dayOfWeek;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
        this.weekPattern = weekPattern;
    }

    /** Whether this session is cancelled for the given week (TYPE_CANCEL exception). */
    public static boolean isCancelled(SessionExceptionEntity ex) {
        return ex != null && ex.type == SessionExceptionEntity.TYPE_CANCEL;
    }
}
