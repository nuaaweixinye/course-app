package com.courseshedule.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.courseshedule.data.local.entity.SemesterConfigEntity;

@Dao
public interface SemesterConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(SemesterConfigEntity config);

    @Update
    void update(SemesterConfigEntity config);

    @Query("SELECT * FROM semester_config WHERE id = 1")
    SemesterConfigEntity get();

    @Query("SELECT COUNT(*) FROM semester_config WHERE id = 1")
    int count();
}
