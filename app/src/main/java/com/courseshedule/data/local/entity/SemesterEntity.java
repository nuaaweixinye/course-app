package com.courseshedule.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "semesters")
public class SemesterEntity {

    public static final int DEFAULT_TOTAL_WEEKS = 16;

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;

    public long startDate;

    public int totalWeeks;

    public String periodTimesJson;

    public boolean isActive;
}
