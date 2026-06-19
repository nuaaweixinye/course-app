package com.courseshedule.data.repository;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.entity.CourseEntity;
import com.courseshedule.data.local.entity.CourseSessionEntity;
import com.courseshedule.data.local.entity.SessionExceptionEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented tests for CourseRepository using an in-memory Room database.
 * Run with a connected device/emulator: ./gradlew :app:connectedCheck
 */
@RunWith(AndroidJUnit4.class)
public class CourseRepositoryTest {

    private AppDatabase db;
    private CourseRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        repository = new CourseRepository(db);
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void saveCourseWithSessionsPersistsAll() {
        CourseEntity course = newCourse("高等数学", 0);
        CourseSessionEntity s1 = newSession(1, 1, 2, "教三301", "1-16");
        CourseSessionEntity s2 = newSession(3, 3, 4, "教三301", "1-16");

        final long[] idHolder = new long[1];
        repository.saveCourse(course, java.util.Arrays.asList(s1, s2),
                courseId -> idHolder[0] = courseId);

        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        long courseId = idHolder[0];
        assertTrue(courseId > 0);
        assertEquals(2, db.courseSessionDao().listByCourse(courseId).size());
        CourseEntity saved = db.courseDao().getById(courseId);
        assertNotNull(saved);
        assertEquals("高等数学", saved.name);
    }

    @Test
    public void deleteCourseCascadesToSessionsAndExceptions() {
        CourseEntity course = newCourse("英语", 1);
        CourseSessionEntity s = newSession(2, 1, 2, "外院2", "1-16");

        final long[] idHolder = new long[1];
        repository.saveCourse(course, java.util.Arrays.asList(s),
                courseId -> idHolder[0] = courseId);
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        long courseId = idHolder[0];
        long sessionId = db.courseSessionDao().listByCourse(courseId).get(0).id;

        SessionExceptionEntity ex = new SessionExceptionEntity();
        ex.sessionId = sessionId;
        ex.weekNo = 5;
        ex.type = SessionExceptionEntity.TYPE_CANCEL;
        repository.saveException(ex);
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        assertEquals(1, db.sessionExceptionDao().listBySession(sessionId).size());

        // Deleting the course should cascade to sessions AND exceptions.
        repository.deleteCourse(db.courseDao().getById(courseId));
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        assertEquals(0, db.courseSessionDao().listByCourse(courseId).size());
        assertEquals(0, db.sessionExceptionDao().listBySession(sessionId).size());
    }

    private CourseEntity newCourse(String name, int colorTag) {
        CourseEntity c = new CourseEntity();
        c.name = name;
        c.colorTag = colorTag;
        return c;
    }

    private CourseSessionEntity newSession(int day, int start, int end, String loc, String pattern) {
        CourseSessionEntity s = new CourseSessionEntity();
        s.dayOfWeek = day;
        s.startPeriod = start;
        s.endPeriod = end;
        s.location = loc;
        s.weekPattern = pattern;
        return s;
    }
}
