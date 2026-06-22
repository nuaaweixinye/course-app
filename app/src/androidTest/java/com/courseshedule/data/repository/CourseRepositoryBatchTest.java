package com.courseshedule.data.repository;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.entity.CourseEntity;
import com.courseshedule.data.local.entity.CourseSessionEntity;
import com.courseshedule.data.local.entity.SemesterEntity;
import com.courseshedule.data.local.entity.SessionExceptionEntity;
import com.courseshedule.data.local.entity.TimetableEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CourseRepositoryBatchTest {

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

        SemesterEntity sem = new SemesterEntity();
        sem.name = "大一上";
        sem.startDate = System.currentTimeMillis();
        sem.totalWeeks = 16;
        sem.isActive = true;
        semId = db.semesterDao().insert(sem);

        TimetableEntity tA = new TimetableEntity();
        tA.name = "课表A";
        tA.semesterId = semId;
        ttA = db.timetableDao().insert(tA);

        TimetableEntity tB = new TimetableEntity();
        tB.name = "课表B";
        tB.semesterId = semId;
        ttB = db.timetableDao().insert(tB);
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void batchDeleteRemovesCoursesAndCascades() throws Exception {
        long c1 = saveCourse("高数", ttA);
        long c2 = saveCourse("英语", ttA);
        long c3 = saveCourse("物理", ttA);
        Thread.sleep(200);

        repository.batchDeleteCourses(Arrays.asList(c1, c2, c3));
        Thread.sleep(300);

        assertNull(db.courseDao().getById(c1));
        assertNull(db.courseDao().getById(c2));
        assertNull(db.courseDao().getById(c3));
        assertEquals(0, db.courseSessionDao().listByCourse(c1).size());
    }

    @Test
    public void batchDeleteCascadesExceptions() throws Exception {
        long courseId = saveCourse("高数", ttA);
        Thread.sleep(200);
        long sessionId = db.courseSessionDao().listByCourse(courseId).get(0).id;

        SessionExceptionEntity ex = new SessionExceptionEntity();
        ex.sessionId = sessionId;
        ex.weekNo = 1;
        ex.type = SessionExceptionEntity.TYPE_CANCEL;
        repository.saveException(ex);
        Thread.sleep(200);

        assertEquals(1, db.sessionExceptionDao().listBySession(sessionId).size());

        repository.batchDeleteCourses(Arrays.asList(courseId));
        Thread.sleep(300);

        assertEquals(0, db.sessionExceptionDao().listBySession(sessionId).size());
    }

    @Test
    public void batchMoveTransfersCoursesToTargetTimetable() throws Exception {
        long c1 = saveCourse("高数", ttA);
        long c2 = saveCourse("英语", ttA);
        Thread.sleep(200);

        repository.batchMoveCourses(Arrays.asList(c1, c2), ttB);
        Thread.sleep(300);

        assertEquals(ttB, (long) db.courseDao().getById(c1).timetableId);
        assertEquals(ttB, (long) db.courseDao().getById(c2).timetableId);
    }

    private long saveCourse(String name, long ttId) throws Exception {
        CourseEntity course = new CourseEntity();
        course.name = name;
        course.semesterId = semId;
        course.timetableId = ttId;
        course.colorTag = 0;
        CourseSessionEntity s = new CourseSessionEntity();
        s.dayOfWeek = 1;
        s.startPeriod = 1;
        s.endPeriod = 2;
        s.weekPattern = "1-16";
        s.location = "";
        final long[] idHolder = new long[1];
        repository.saveCourse(course, java.util.Collections.singletonList(s), id -> idHolder[0] = id);
        Thread.sleep(200);
        return idHolder[0];
    }
}
