package com.courseshedule.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        tableName = "timetables",
        foreignKeys = @ForeignKey(
                entity = SemesterEntity.class,
                parentColumns = "id",
                childColumns = "semesterId",
                onDelete = CASCADE
        ),
        indices = {@Index("semesterId")}
)
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
