package com.courseshedule.data.repository;

import androidx.lifecycle.LiveData;

import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.dao.SemesterDao;
import com.courseshedule.data.local.dao.TimetableDao;
import com.courseshedule.data.local.entity.TimetableEntity;
import com.courseshedule.data.model.TimetableWithSemester;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TimetableRepository {

    private final TimetableDao dao;
    private final SemesterDao semesterDao;
    private final AppDatabase db;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public TimetableRepository(AppDatabase db) {
        this.db = db;
        this.dao = db.timetableDao();
        this.semesterDao = db.semesterDao();
    }

    public LiveData<List<TimetableEntity>> observeBySemester(long semesterId) {
        return dao.observeBySemester(semesterId);
    }

    public LiveData<TimetableEntity> observeActive(long semesterId) {
        return dao.observeActive(semesterId);
    }

    public LiveData<TimetableEntity> observeActiveGlobal() {
        return dao.observeActiveGlobal();
    }

    public TimetableEntity getActiveGlobal() {
        return dao.getActiveGlobal();
    }

    public void switchTo(long id, long semesterId) {
        io.execute(() -> db.runInTransaction(() -> {
            dao.clearAllActive();
            dao.setActive(id);
            semesterDao.clearActive();
            semesterDao.setActive(semesterId);
        }));
    }

    public List<TimetableEntity> listBySemester(long semesterId) {
        return dao.listBySemester(semesterId);
    }

    public LiveData<List<TimetableWithSemester>> observeAllWithSemester() {
        return dao.observeAllWithSemester();
    }

    public List<TimetableWithSemester> listAllWithSemester() {
        return dao.listAllWithSemester();
    }

    public interface CreateCallback {
        void onCreated(long id);
    }

    public void create(String name, long semesterId, final CreateCallback callback) {
        io.execute(() -> db.runInTransaction(() -> {
            TimetableEntity t = new TimetableEntity();
            t.name = name;
            t.semesterId = semesterId;
            final long id = dao.insert(t);
            if (callback != null) {
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onCreated(id));
            }
        }));
    }

    public void update(TimetableEntity timetable) {
        io.execute(() -> dao.update(timetable));
    }

    public void delete(TimetableEntity timetable) {
        io.execute(() -> dao.delete(timetable));
    }
}
