package com.courseshedule.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Application-level semester config, stored as a single row with a fixed id=1.
 * startDate is the Monday of week 1 (epoch millis, midnight local).
 * periodTimesJson encodes each period's start/end time (see PeriodUtils).
 */
@Entity(tableName = "semester_config")
public class SemesterConfigEntity {

    public static final long SINGLETON_ID = 1L;
    public static final int DEFAULT_TOTAL_WEEKS = 16;

    @PrimaryKey
    public long id = SINGLETON_ID;

    /** Monday of week 1, midnight local (epoch millis). */
    public long startDate;

    public int totalWeeks;

    public String periodTimesJson;
}
