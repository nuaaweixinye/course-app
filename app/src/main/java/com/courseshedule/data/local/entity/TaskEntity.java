package com.courseshedule.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * A homework or exam reminder, independent of the timetable. courseId is
 * nullable so a task may exist without being linked to a course.
 */
@Entity(
        tableName = "tasks",
        foreignKeys = @ForeignKey(
                entity = CourseEntity.class,
                parentColumns = "id",
                childColumns = "courseId",
                onDelete = ForeignKey.SET_NULL
        ),
        indices = {@Index("courseId")}
)
public class TaskEntity {

    public static final int TYPE_HOMEWORK = 0;
    public static final int TYPE_EXAM = 1;

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;

    /** TYPE_HOMEWORK or TYPE_EXAM. */
    public int type;

    /** Optional link to a course; null when unlinked. */
    public Long courseId;

    /** Due time (epoch millis). */
    public long dueDate;

    public boolean done;

    public String note;
}
