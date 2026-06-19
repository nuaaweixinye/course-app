package com.courseshedule.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.dao.CourseDao;
import com.courseshedule.data.local.dao.CourseSessionDao;
import com.courseshedule.data.local.dao.SessionExceptionDao;
import com.courseshedule.data.local.entity.CourseEntity;
import com.courseshedule.data.local.entity.CourseSessionEntity;
import com.courseshedule.data.local.entity.SessionExceptionEntity;
import com.courseshedule.data.model.DisplaySession;
import com.courseshedule.data.model.SessionWithCourse;
import com.courseshedule.data.model.WeekUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CourseRepository {

    private final AppDatabase db;
    private final CourseDao courseDao;
    private final CourseSessionDao sessionDao;
    private final SessionExceptionDao exceptionDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final MediatorLiveData<List<CourseEntity>> observableCourses = new MediatorLiveData<>();
    private LiveData<List<CourseEntity>> currentCourseSource;
    private volatile long activeSemesterId = 1;
    private volatile Long activeTimetableId = null;

    public CourseRepository(AppDatabase db) {
        this.db = db;
        this.courseDao = db.courseDao();
        this.sessionDao = db.courseSessionDao();
        this.exceptionDao = db.sessionExceptionDao();
        currentCourseSource = courseDao.observeUnassigned(activeSemesterId);
        observableCourses.addSource(currentCourseSource, observableCourses::setValue);
    }

    public void setActiveSemester(long semesterId) {
        this.activeSemesterId = semesterId;
        refreshCourseSource();
    }

    public void setActiveTimetableId(Long timetableId) {
        this.activeTimetableId = timetableId;
        refreshCourseSource();
    }

    public Long getActiveTimetableId() {
        return activeTimetableId;
    }

    private void refreshCourseSource() {
        if (currentCourseSource != null) {
            observableCourses.removeSource(currentCourseSource);
        }
        currentCourseSource = (activeTimetableId != null)
                ? courseDao.observeByTimetable(activeTimetableId)
                : courseDao.observeUnassigned(activeSemesterId);
        observableCourses.addSource(currentCourseSource, observableCourses::setValue);
    }

    public LiveData<List<CourseEntity>> observeCourses() {
        return observableCourses;
    }

    public LiveData<CourseEntity> observeCourse(long id) {
        return courseDao.observeById(id);
    }

    public LiveData<List<CourseSessionEntity>> observeSessions(long courseId) {
        return sessionDao.observeByCourse(courseId);
    }

    public LiveData<List<DisplaySession>> observeWeekSessions(int weekNo) {
        MediatorLiveData<List<DisplaySession>> result = new MediatorLiveData<>();
        final LiveData<List<SessionWithCourse>> sessionsSource = sessionDao.observeAllWithCourse();
        final LiveData<List<SessionExceptionEntity>> exceptionsSource = exceptionDao.observeAll();

        result.addSource(sessionsSource, sessions ->
                result.setValue(combine(sessions, exceptionsSource.getValue(), weekNo)));
        result.addSource(exceptionsSource, exceptions ->
                result.setValue(combine(sessionsSource.getValue(), exceptions, weekNo)));
        return result;
    }

    private List<DisplaySession> combine(List<SessionWithCourse> sessions,
                                         List<SessionExceptionEntity> exceptions,
                                         int weekNo) {
        List<DisplaySession> out = new ArrayList<>();
        if (sessions == null) return out;
        Map<Long, SessionExceptionEntity> exMap = new HashMap<>();
        if (exceptions != null) {
            for (SessionExceptionEntity ex : exceptions) {
                if (ex.weekNo == weekNo) exMap.put(ex.sessionId, ex);
            }
        }
        for (SessionWithCourse s : sessions) {
            if (s.semesterId != activeSemesterId) continue;
            if (!WeekUtils.matchesWeek(s.weekPattern, weekNo)) continue;
            if (activeTimetableId != null) {
                if (s.timetableId == null || s.timetableId != activeTimetableId) continue;
            } else {
                if (s.timetableId != null) continue;
            }
            SessionExceptionEntity ex = exMap.get(s.sessionId);
            if (DisplaySession.isCancelled(ex)) continue;
            int day = s.dayOfWeek;
            if (ex != null && ex.type == SessionExceptionEntity.TYPE_MOVED && ex.moveToDayOfWeek != null) {
                day = ex.moveToDayOfWeek;
            }
            out.add(new DisplaySession(
                    s.sessionId, s.courseId, s.courseName, s.teacher, s.location,
                    s.colorTag, day, s.startPeriod, s.endPeriod, s.weekPattern));
        }
        return out;
    }

    public void saveCourse(final CourseEntity course, final List<CourseSessionEntity> sessions,
                           final OnCourseSaved callback) {
        io.execute(() -> db.runInTransaction(() -> {
            long newId = courseDao.insert(course);
            for (CourseSessionEntity s : sessions) {
                s.courseId = newId;
                sessionDao.insert(s);
            }
            if (callback != null) callback.onSaved(newId);
        }));
    }

    public void updateCourse(final CourseEntity course, final List<CourseSessionEntity> sessions) {
        io.execute(() -> db.runInTransaction(() -> {
            courseDao.update(course);
            sessionDao.deleteByCourse(course.id);
            for (CourseSessionEntity s : sessions) {
                s.courseId = course.id;
                sessionDao.insert(s);
            }
        }));
    }

    public void deleteCourse(final CourseEntity course) {
        io.execute(() -> courseDao.delete(course));
    }

    public void batchDeleteCourses(final java.util.List<Long> courseIds) {
        io.execute(() -> db.runInTransaction(() -> {
            List<CourseSessionEntity> sessions = sessionDao.listByCourseIds(courseIds);
            List<Long> sessionIds = new ArrayList<>();
            for (CourseSessionEntity s : sessions) sessionIds.add(s.id);
            if (!sessionIds.isEmpty()) exceptionDao.deleteBySessionIds(sessionIds);
            sessionDao.deleteByCourseIds(courseIds);
            courseDao.deleteByIds(courseIds);
        }));
    }

    public void batchMoveCourses(final java.util.List<Long> courseIds, long targetTimetableId) {
        io.execute(() -> courseDao.updateTimetableForCourses(courseIds, targetTimetableId));
    }

    public void saveException(final SessionExceptionEntity exception) {
        io.execute(() -> exceptionDao.insert(exception));
    }

    public void deleteException(final long exceptionId) {
        io.execute(() -> exceptionDao.deleteById(exceptionId));
    }

    public List<SessionExceptionEntity> listExceptions(long sessionId) {
        return exceptionDao.listBySession(sessionId);
    }

    public List<CourseSessionEntity> listSessions(long courseId) {
        return sessionDao.listByCourse(courseId);
    }

    public interface OnCourseSaved {
        void onSaved(long courseId);
    }
}
