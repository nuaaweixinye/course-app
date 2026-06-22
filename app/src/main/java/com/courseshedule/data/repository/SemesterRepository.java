package com.courseshedule.data.repository;

import androidx.lifecycle.LiveData;

import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.dao.SemesterDao;
import com.courseshedule.data.local.entity.SemesterEntity;
import com.courseshedule.data.local.entity.TimetableEntity;
import com.courseshedule.data.model.PeriodUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SemesterRepository {

    private final SemesterDao dao;
    private final AppDatabase db;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private static volatile SemesterEntity cachedActive;
    private volatile java.util.List<SemesterEntity> cachedAll;

    public SemesterRepository(AppDatabase db) {
        this.db = db;
        this.dao = db.semesterDao();
    }

    public static boolean isCached() {
        return cachedActive != null;
    }

    public SemesterEntity getCachedOrDefault() {
        SemesterEntity c = cachedActive;
        if (c != null) return c;
        SemesterEntity def = new SemesterEntity();
        def.name = "默认学期";
        def.startDate = PeriodUtils.mondayOfDay(System.currentTimeMillis());
        def.totalWeeks = 16;
        def.periodTimesJson = PeriodUtils.DEFAULT_PERIOD_TIMES_JSON;
        def.isActive = true;
        return def;
    }

    public LiveData<SemesterEntity> observeActive() {
        return dao.observeActive();
    }

    public LiveData<List<SemesterEntity>> observeAll() {
        return dao.observeAll();
    }

    public List<SemesterEntity> listAll() {
        return dao.listAll();
    }

    public SemesterEntity getSeedingDefault() {
        final SemesterEntity[] result = new SemesterEntity[1];
        db.runInTransaction(() -> {
            cachedActive = dao.getActive();
            result[0] = cachedActive;
        });
        return result[0];
    }

    public void switchTo(long semesterId) {
        io.execute(() -> db.runInTransaction(() -> {
            dao.clearActive();
            dao.setActive(semesterId);
            cachedActive = dao.getActive();
        }));
    }

    public interface CreateCallback {
        void onCreated(long id);
    }

    public void create(final String name, final long startDate, final int totalWeeks, final CreateCallback callback) {
        io.execute(() -> {
            SemesterEntity s = new SemesterEntity();
            s.name = name;
            s.startDate = startDate;
            s.totalWeeks = totalWeeks;
            s.periodTimesJson = PeriodUtils.DEFAULT_PERIOD_TIMES_JSON;
            s.isActive = false;
            final long id = dao.insert(s);
            if (callback != null) {
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onCreated(id));
            }
        });
    }

    public void delete(final SemesterEntity semester) {
        io.execute(() -> dao.delete(semester));
    }

    public void update(final SemesterEntity semester) {
        io.execute(() -> dao.update(semester));
    }

    public void repairDuplicateActive() {
        io.execute(() -> db.runInTransaction(() -> {
            com.courseshedule.data.local.dao.TimetableDao ttDao = db.timetableDao();
            List<SemesterEntity> actives = dao.getAllActive();
            if (actives.size() > 1) {
                dao.deactivateAllExcept(actives.get(0).id);
                cachedActive = actives.get(0);
            } else if (actives.size() == 1) {
                cachedActive = actives.get(0);
            }
            TimetableEntity activeTt = ttDao.getActive(actives.isEmpty() ? -1 : actives.get(0).id);
            if (activeTt == null && !actives.isEmpty()) {
                long semId = actives.get(0).id;
                java.util.List<TimetableEntity> tts = ttDao.listBySemester(semId);
                if (!tts.isEmpty()) {
                    ttDao.clearAllActive();
                    ttDao.setActive(tts.get(0).id);
                }
            }
        }));
    }
}
