package com.courseshedule.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

/**
 * A course (e.g. 高等数学). A course owns one or more CourseSession rows that
 * describe when/where it meets. colorTag indexes the fixed 8-color palette and
 * drives the card's left border across all screens.
 */
@Entity(
        tableName = "courses",
        foreignKeys = @ForeignKey(
                entity = TimetableEntity.class,
                parentColumns = "id",
                childColumns = "timetableId",
                onDelete = CASCADE
        ),
        indices = {@Index("timetableId")}
)
public class CourseEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Course name, e.g. "高等数学". Required (validated in UI). */
    public String name;

    /** Teacher name. Optional. */
    public String teacher;

    /** Index into the fixed 8-color palette (0..7). */
    public int colorTag;

    /** Free-form note. Optional. */
    public String note;

    /** Which semester this course belongs to. */
    public long semesterId;

    /** Which timetable this course belongs to. */
    public Long timetableId;
}
