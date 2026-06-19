package com.courseshedule.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.courseshedule.data.local.entity.SemesterEntity;

import java.util.List;

@Dao
public interface SemesterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SemesterEntity semester);

    @Update
    void update(SemesterEntity semester);

    @Delete
    void delete(SemesterEntity semester);

    @Query("SELECT * FROM semesters ORDER BY startDate DESC")
    LiveData<List<SemesterEntity>> observeAll();

    @Query("SELECT * FROM semesters WHERE isActive = 1 LIMIT 1")
    LiveData<SemesterEntity> observeActive();

    @Query("SELECT * FROM semesters WHERE isActive = 1 LIMIT 1")
    SemesterEntity getActive();

    @Query("SELECT * FROM semesters WHERE id = :id")
    SemesterEntity getById(long id);

    @Query("SELECT COUNT(*) FROM semesters")
    int count();

    @Query("UPDATE semesters SET isActive = 0")
    void clearActive();

    @Query("UPDATE semesters SET isActive = 1 WHERE id = :id")
    void setActive(long id);

    @Query("SELECT * FROM semesters ORDER BY startDate DESC")
    java.util.List<SemesterEntity> listAll();
}
