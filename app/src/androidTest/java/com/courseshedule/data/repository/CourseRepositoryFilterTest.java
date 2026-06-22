package com.courseshedule.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.entity.CourseEntity;
import com.courseshedule.data.local.entity.CourseSessionEntity;
import com.courseshedule.data.local.entity.SemesterEntity;
import com.courseshedule.data.local.entity.SessionExceptionEntity;
import com.courseshedule.data.local.entity.TimetableEntity;
import com.courseshedule.data.model.DisplaySession;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CourseRepositoryFilterTest {

    private AppDatabase db;
    private CourseRepository repository;
    private long semId;
    private long ttA;
    private long ttB;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        repository = new CourseRepository(db);

        semId = insertSemester("大一上");
        ttA = insertTimetable("课表A", semId);
        ttB = insertTimetable("课表B", semId);

        runOnMain(() -> repository.setActiveSemester(semId));
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void observeWeekSessionsFiltersByTimetable() throws Exception {
        saveCourse("高数", ttA, 1, 1, 2, "1-16");
        saveCourse("英语", ttB, 1, 1, 2, "1-16");
        Thread.sleep(200);

        runOnMain(() -> repository.setActiveTimetableId(ttA));
        Thread.sleep(200);

        List<DisplaySession> result = awaitWeekSessions(3);

        assertEquals("Only timetable A courses should appear", 1, result.size());
        assertEquals("高数", result.get(0).courseName);
    }

    @Test
    public void observeWeekSessionsWeekPatternFiltering() throws Exception {
        saveCourse("高数", ttA, 1, 1, 2, "1-8");
        Thread.sleep(200);

        runOnMain(() -> repository.setActiveTimetableId(ttA));
        Thread.sleep(200);

        List<DisplaySession> result = awaitWeekSessions(10);
        assertTrue("Session with pattern 1-8 should not appear in week 10", result.isEmpty());
    }

    @Test
    public void cancelExceptionExcludesSession() throws Exception {
        long courseId = saveCourse("高数", ttA, 1, 1, 2, "1-16");
        Thread.sleep(200);
        long sessionId = db.courseSessionDao().listByCourse(courseId).get(0).id;

        SessionExceptionEntity ex = new SessionExceptionEntity();
        ex.sessionId = sessionId;
        ex.weekNo = 3;
        ex.type = SessionExceptionEntity.TYPE_CANCEL;
        repository.saveException(ex);
        Thread.sleep(200);

        runOnMain(() -> repository.setActiveTimetableId(ttA));
        Thread.sleep(200);

        List<DisplaySession> result = awaitWeekSessions(3);
        assertTrue("Cancelled session should not appear", result.isEmpty());
    }

    @Test
    public void movedExceptionChangesDay() throws Exception {
        long courseId = saveCourse("高数", ttA, 2, 1, 2, "1-16");
        Thread.sleep(200);
        long sessionId = db.courseSessionDao().listByCourse(courseId).get(0).id;

        SessionExceptionEntity ex = new SessionExceptionEntity();
        ex.sessionId = sessionId;
        ex.weekNo = 1;
        ex.type = SessionExceptionEntity.TYPE_MOVED;
        ex.moveToDayOfWeek = 4;
        repository.saveException(ex);
        Thread.sleep(200);

        runOnMain(() -> repository.setActiveTimetableId(ttA));
        Thread.sleep(200);

        List<DisplaySession> result = awaitWeekSessions(1);
        assertEquals(1, result.size());
        assertEquals("Day should be moved from 2 to 4", 4, result.get(0).dayOfWeek);
    }

    private long saveCourse(String name, long ttId, int day, int start, int end, String pattern) throws Exception {
        CourseEntity course = new CourseEntity();
        course.name = name;
        course.semesterId = semId;
        course.timetableId = ttId;
        course.colorTag = 0;
        CourseSessionEntity s = new CourseSessionEntity();
        s.dayOfWeek = day;
        s.startPeriod = start;
        s.endPeriod = end;
        s.weekPattern = pattern;
        s.location = "";
        final long[] idHolder = new long[1];
        repository.saveCourse(course, java.util.Collections.singletonList(s), id -> idHolder[0] = id);
        Thread.sleep(200);
        return idHolder[0];
    }

    private List<DisplaySession> awaitWeekSessions(int weekNo) throws Exception {
        final LiveData<List<DisplaySession>> liveData = repository.observeWeekSessions(weekNo);
        final List<DisplaySession>[] result = new List[1];
        final CountDownLatch latch = new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                liveData.observeForever(value -> {
                    result[0] = value;
                    latch.countDown();
                }));
        latch.await(5, TimeUnit.SECONDS);
        Thread.sleep(300);
        return result[0];
    }

    private void runOnMain(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }

    private long insertSemester(String name) {
        SemesterEntity s = new SemesterEntity();
        s.name = name;
        s.startDate = System.currentTimeMillis();
        s.totalWeeks = 16;
        s.isActive = true;
        return db.semesterDao().insert(s);
    }

    private long insertTimetable(String name, long semesterId) {
        TimetableEntity t = new TimetableEntity();
        t.name = name;
        t.semesterId = semesterId;
        t.isActive = false;
        return db.timetableDao().insert(t);
    }
}
