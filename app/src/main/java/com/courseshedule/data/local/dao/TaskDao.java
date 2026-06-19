package com.courseshedule.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.courseshedule.data.local.entity.TaskEntity;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TaskEntity task);

    @Update
    void update(TaskEntity task);

    @Query("DELETE FROM tasks WHERE id = :id")
    void deleteById(long id);

    @Query("UPDATE tasks SET done = :done WHERE id = :id")
    void setDone(long id, boolean done);

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    LiveData<List<TaskEntity>> observeAllOrdered();

    @Query("SELECT * FROM tasks WHERE id = :id")
    TaskEntity getById(long id);
}
