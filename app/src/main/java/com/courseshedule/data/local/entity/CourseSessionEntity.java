package com.courseshedule.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

/**
 * One weekly meeting slot of a course. A course typically has several sessions
 * (e.g. 高数 Mon 1-2 + Wed 3-4). startPeriod/endPeriod are 1-based period
 * indices (1..12). weekPattern compresses the active weeks into a string such
 * as "1-16" or "1,3,5,7,9,11,13,15" (odd weeks); see WeekUtils for parsing.
 */
@Entity(
        tableName = "course_sessions",
        foreignKeys = @ForeignKey(
                entity = CourseEntity.class,
                parentColumns = "id",
                childColumns = "courseId",
                onDelete = CASCADE
        ),
        indices = {@Index("courseId")}
)
public class CourseSessionEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long courseId;

    /** Weekday: 1 = Monday … 7 = Sunday. */
    public int dayOfWeek;

    /** Inclusive start period (1-based). */
    public int startPeriod;

    /** Inclusive end period (>= startPeriod). */
    public int endPeriod;

    /** Room/location, e.g. "教三301". Optional. */
    public String location;

    /** Compressed week pattern, e.g. "1-16" or "1,3,5,7". */
    public String weekPattern;
}
