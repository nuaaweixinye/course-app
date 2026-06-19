package com.courseshedule.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "timetables")
public class TimetableEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "semesterId")
    public long semesterId;

    @ColumnInfo(name = "isActive")
    public boolean isActive;
}
