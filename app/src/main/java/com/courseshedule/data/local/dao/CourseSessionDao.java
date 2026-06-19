package com.courseshedule.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.courseshedule.data.local.entity.CourseSessionEntity;
import com.courseshedule.data.model.SessionWithCourse;

import java.util.List;

@Dao
public interface CourseSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CourseSessionEntity session);

    @Update
    void update(CourseSessionEntity session);

    @Delete
    void delete(CourseSessionEntity session);

    @Query("SELECT * FROM course_sessions WHERE courseId = :courseId ORDER BY dayOfWeek, startPeriod")
    LiveData<List<CourseSessionEntity>> observeByCourse(long courseId);

    @Query("SELECT * FROM course_sessions WHERE dayOfWeek = :dayOfWeek")
    LiveData<List<CourseSessionEntity>> observeByDay(int dayOfWeek);

    @Query("SELECT * FROM course_sessions")
    LiveData<List<CourseSessionEntity>> observeAll();

    @Query("SELECT s.id AS sessionId, s.courseId AS courseId, s.dayOfWeek AS dayOfWeek, " +
            "s.startPeriod AS startPeriod, s.endPeriod AS endPeriod, s.location AS location, " +
            "s.weekPattern AS weekPattern, c.name AS courseName, c.teacher AS teacher, " +
            "c.colorTag AS colorTag, c.semesterId AS semesterId, " +
            "c.timetableId AS timetableId " +
            "FROM course_sessions s INNER JOIN courses c ON s.courseId = c.id " +
            "ORDER BY s.dayOfWeek, s.startPeriod")
    LiveData<List<SessionWithCourse>> observeAllWithCourse();

    @Query("SELECT s.id AS sessionId, s.courseId AS courseId, s.dayOfWeek AS dayOfWeek, " +
            "s.startPeriod AS startPeriod, s.endPeriod AS endPeriod, s.location AS location, " +
            "s.weekPattern AS weekPattern, c.name AS courseName, c.teacher AS teacher, " +
            "c.colorTag AS colorTag, c.semesterId AS semesterId, " +
            "c.timetableId AS timetableId " +
            "FROM course_sessions s INNER JOIN courses c ON s.courseId = c.id")
    List<SessionWithCourse> listAllWithCourse();

    @Query("SELECT * FROM course_sessions WHERE courseId = :courseId ORDER BY dayOfWeek, startPeriod")
    List<CourseSessionEntity> listByCourse(long courseId);

    @Query("SELECT * FROM course_sessions WHERE courseId IN (:courseIds)")
    List<CourseSessionEntity> listByCourseIds(java.util.List<Long> courseIds);

    @Query("DELETE FROM course_sessions WHERE courseId = :courseId")
    void deleteByCourse(long courseId);

    @Query("DELETE FROM course_sessions WHERE courseId IN (:courseIds)")
    void deleteByCourseIds(java.util.List<Long> courseIds);
}
