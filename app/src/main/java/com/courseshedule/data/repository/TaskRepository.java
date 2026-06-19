package com.courseshedule.data.repository;

import androidx.lifecycle.LiveData;

import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.dao.TaskDao;
import com.courseshedule.data.local.entity.TaskEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Access surface for homework/exam tasks. */
public class TaskRepository {

    private final TaskDao taskDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public TaskRepository(AppDatabase db) {
        this.taskDao = db.taskDao();
    }

    public LiveData<List<TaskEntity>> observeAll() {
        return taskDao.observeAllOrdered();
    }

    public void insert(final TaskEntity task) {
        io.execute(() -> taskDao.insert(task));
    }

    public void update(final TaskEntity task) {
        io.execute(() -> taskDao.update(task));
    }

    public void delete(final long taskId) {
        io.execute(() -> taskDao.deleteById(taskId));
    }

    public void setDone(final long taskId, final boolean done) {
        io.execute(() -> taskDao.setDone(taskId, done));
    }
}
