package com.courseshedule.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.courseshedule.data.local.entity.TimetableEntity;
import com.courseshedule.data.model.TimetableWithSemester;

import java.util.List;

@Dao
public interface TimetableDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TimetableEntity timetable);

    @Update
    void update(TimetableEntity timetable);

    @Delete
    void delete(TimetableEntity timetable);

    @Query("SELECT * FROM timetables WHERE semesterId = :semesterId ORDER BY name")
    LiveData<List<TimetableEntity>> observeBySemester(long semesterId);

    @Query("SELECT * FROM timetables WHERE semesterId = :semesterId ORDER BY name")
    List<TimetableEntity> listBySemester(long semesterId);

    @Query("SELECT * FROM timetables WHERE id = :id")
    TimetableEntity getById(long id);

    @Query("SELECT COUNT(*) FROM timetables WHERE semesterId = :semesterId")
    int countBySemester(long semesterId);

    @Query("UPDATE timetables SET isActive = 0 WHERE semesterId = :semesterId")
    void clearActive(long semesterId);

    @Query("UPDATE timetables SET isActive = 0")
    void clearAllActive();

    @Query("UPDATE timetables SET isActive = 1 WHERE id = :id")
    void setActive(long id);

    @Query("SELECT * FROM timetables WHERE semesterId = :semesterId AND isActive = 1 LIMIT 1")
    LiveData<TimetableEntity> observeActive(long semesterId);

    @Query("SELECT * FROM timetables WHERE isActive = 1 LIMIT 1")
    LiveData<TimetableEntity> observeActiveGlobal();

    @Query("SELECT * FROM timetables WHERE isActive = 1 LIMIT 1")
    TimetableEntity getActiveGlobal();

    @Query("SELECT * FROM timetables WHERE semesterId = :semesterId AND isActive = 1 LIMIT 1")
    TimetableEntity getActive(long semesterId);

    @Query("SELECT t.id AS id, t.name AS name, t.semesterId AS semesterId, " +
            "t.isActive AS isActive, s.name AS semesterName " +
            "FROM timetables t INNER JOIN semesters s ON t.semesterId = s.id " +
            "ORDER BY CASE WHEN s.isActive = 1 THEN 0 ELSE 1 END, s.startDate DESC, t.name")
    LiveData<List<TimetableWithSemester>> observeAllWithSemester();

    @Query("SELECT t.id AS id, t.name AS name, t.semesterId AS semesterId, " +
            "t.isActive AS isActive, s.name AS semesterName " +
            "FROM timetables t INNER JOIN semesters s ON t.semesterId = s.id " +
            "ORDER BY CASE WHEN s.isActive = 1 THEN 0 ELSE 1 END, s.startDate DESC, t.name")
    List<TimetableWithSemester> listAllWithSemester();
}
