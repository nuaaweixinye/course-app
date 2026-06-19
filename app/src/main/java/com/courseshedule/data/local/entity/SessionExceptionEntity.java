package com.courseshedule.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

/**
 * A per-week override applied on top of a session's base weekPattern. CANCEL
 * hides the session in the given week; MOVED relocates it to moveToDayOfWeek
 * (same periods). The base session itself is never mutated by an exception.
 */
@Entity(
        tableName = "session_exceptions",
        foreignKeys = @ForeignKey(
                entity = CourseSessionEntity.class,
                parentColumns = "id",
                childColumns = "sessionId",
                onDelete = CASCADE
        ),
        indices = {@Index("sessionId")}
)
public class SessionExceptionEntity {

    public static final int TYPE_CANCEL = 0;
    public static final int TYPE_MOVED = 1;

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long sessionId;

    /** Week number the exception applies to (1-based). */
    public int weekNo;

    /** TYPE_CANCEL or TYPE_MOVED. */
    public int type;

    /** Target weekday when type == TYPE_MOVED (1..7); null otherwise. */
    public Integer moveToDayOfWeek;
}
