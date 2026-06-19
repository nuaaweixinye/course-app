package com.courseshedule.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Update;

import com.courseshedule.data.local.entity.CourseEntity;

import java.util.List;

@Dao
public interface CourseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CourseEntity course);

    @Update
    void update(CourseEntity course);

    @Delete
    void delete(CourseEntity course);

    @androidx.room.Query("SELECT * FROM courses ORDER BY name COLLATE NOCASE")
    LiveData<List<CourseEntity>> observeAll();

    @androidx.room.Query("SELECT * FROM courses WHERE id = :id")
    LiveData<CourseEntity> observeById(long id);

    @androidx.room.Query("SELECT * FROM courses WHERE id = :id")
    CourseEntity getById(long id);

    @androidx.room.Query("SELECT * FROM courses ORDER BY name COLLATE NOCASE")
    List<CourseEntity> listAll();

    @androidx.room.Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY name COLLATE NOCASE")
    LiveData<List<CourseEntity>> observeBySemester(long semesterId);

    @androidx.room.Query("SELECT * FROM courses WHERE timetableId = :timetableId ORDER BY name COLLATE NOCASE")
    LiveData<List<CourseEntity>> observeByTimetable(long timetableId);

    @androidx.room.Query("SELECT * FROM courses WHERE timetableId IS NULL AND semesterId = :semesterId ORDER BY name COLLATE NOCASE")
    LiveData<List<CourseEntity>> observeUnassigned(long semesterId);

    @androidx.room.Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY name COLLATE NOCASE")
    List<CourseEntity> listBySemester(long semesterId);

    @androidx.room.Query("DELETE FROM courses WHERE id IN (:ids)")
    void deleteByIds(java.util.List<Long> ids);

    @androidx.room.Query("UPDATE courses SET semesterId = :semesterId WHERE id IN (:ids)")
    void updateSemesterForCourses(java.util.List<Long> ids, long semesterId);

    @androidx.room.Query("UPDATE courses SET timetableId = :timetableId WHERE id IN (:ids)")
    void updateTimetableForCourses(java.util.List<Long> ids, long timetableId);

    @androidx.room.Query("SELECT COUNT(*) FROM courses WHERE timetableId = :timetableId")
    int countByTimetable(long timetableId);

    @androidx.room.Query("SELECT COUNT(*) FROM courses WHERE timetableId IS NULL AND semesterId = :semesterId")
    int countUnassigned(long semesterId);
}
