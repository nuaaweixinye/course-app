package com.courseshedule.data.repository;

import androidx.lifecycle.LiveData;

import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.dao.TimetableDao;
import com.courseshedule.data.local.entity.TimetableEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TimetableRepository {

    private final TimetableDao dao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public TimetableRepository(AppDatabase db) {
        this.dao = db.timetableDao();
    }

    public LiveData<List<TimetableEntity>> observeBySemester(long semesterId) {
        return dao.observeBySemester(semesterId);
    }

    public LiveData<TimetableEntity> observeActive(long semesterId) {
        return dao.observeActive(semesterId);
    }

    public void switchTo(long id, long semesterId) {
        io.execute(() -> {
            dao.clearActive(semesterId);
            dao.setActive(id);
        });
    }

    public List<TimetableEntity> listBySemester(long semesterId) {
        return dao.listBySemester(semesterId);
    }

    public interface CreateCallback {
        void onCreated(long id);
    }

    public void create(String name, long semesterId, final CreateCallback callback) {
        io.execute(() -> {
            TimetableEntity t = new TimetableEntity();
            t.name = name;
            t.semesterId = semesterId;
            final long id = dao.insert(t);
            if (callback != null) {
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onCreated(id));
            }
        });
    }

    public void update(TimetableEntity timetable) {
        io.execute(() -> dao.update(timetable));
    }

    public void delete(TimetableEntity timetable) {
        io.execute(() -> dao.delete(timetable));
    }
}
