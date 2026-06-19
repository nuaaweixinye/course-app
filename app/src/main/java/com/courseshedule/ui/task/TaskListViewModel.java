package com.courseshedule.ui.task;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.courseshedule.App;
import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.entity.TaskEntity;
import com.courseshedule.data.repository.TaskRepository;

import java.util.List;

/** Access surface for the task list screen. */
public class TaskListViewModel extends AndroidViewModel {

    private final TaskRepository repository;

    public TaskListViewModel(@NonNull Application application, AppDatabase db) {
        super(application);
        repository = new TaskRepository(db);
    }

    public LiveData<List<TaskEntity>> getTasks() {
        return repository.observeAll();
    }

    public void save(TaskEntity task) {
        if (task.id == 0) repository.insert(task);
        else repository.update(task);
    }

    public void toggleDone(TaskEntity task) {
        repository.setDone(task.id, !task.done);
    }

    public void delete(TaskEntity task) {
        repository.delete(task.id);
    }

    public LiveData<List<com.courseshedule.data.local.entity.CourseEntity>> getCourses() {
        return new com.courseshedule.data.repository.CourseRepository(
                ((App) getApplication()).getDatabase()).observeCourses();
    }

    public static class Factory extends androidx.lifecycle.ViewModelProvider.NewInstanceFactory {
        private final Application app;
        private final AppDatabase db;

        public Factory(Application app) {
            this.app = app;
            this.db = ((App) app).getDatabase();
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            //noinspection unchecked
            return (T) new TaskListViewModel(app, db);
        }
    }
}
