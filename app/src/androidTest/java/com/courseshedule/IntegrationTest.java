package com.courseshedule;

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
import com.courseshedule.data.local.entity.TimetableEntity;
import com.courseshedule.data.model.DisplaySession;
import com.courseshedule.data.repository.CourseRepository;
import com.courseshedule.data.repository.TimetableRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class IntegrationTest {

    private AppDatabase db;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void fullFlowCreateSwitchFilter() throws Exception {
        long sem1 = insertSemester("大一上", true);
        long sem2 = insertSemester("大一下", false);

        long tt1 = insertTimetable("课表A", sem1);
        long tt2 = insertTimetable("课表B", sem2);

        CourseRepository courseRepo = new CourseRepository(db);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                courseRepo.setActiveSemester(sem1));

        saveCourse(courseRepo, "高数", sem1, tt1, 1, 1, 2, "1-16");
        saveCourse(courseRepo, "英语", sem1, tt1, 2, 3, 4, "1-16");
        saveCourse(courseRepo, "物理", sem2, tt2, 3, 5, 6, "1-16");
        Thread.sleep(300);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                courseRepo.setActiveTimetableId(tt1));
        Thread.sleep(300);

        List<DisplaySession> week1 = awaitWeekSessions(courseRepo, 1);
        assertEquals("Should see 2 courses from timetable A", 2, week1.size());

        TimetableRepository ttRepo = new TimetableRepository(db);
        ttRepo.switchTo(tt2, sem2);
        Thread.sleep(500);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            courseRepo.setActiveSemester(sem2);
            courseRepo.setActiveTimetableId(tt2);
        });        Thread.sleep(300);

        List<DisplaySession> afterSwitch = awaitWeekSessions(courseRepo, 1);
        assertEquals("Should see 1 course from timetable B after switch", 1, afterSwitch.size());
        assertEquals("物理", afterSwitch.get(0).courseName);
    }

    private long insertSemester(String name, boolean active) {
        if (active) db.semesterDao().clearActive();
        SemesterEntity s = new SemesterEntity();
        s.name = name;
        s.startDate = System.currentTimeMillis();
        s.totalWeeks = 16;
        s.isActive = active;
        return db.semesterDao().insert(s);
    }

    private long insertTimetable(String name, long semesterId) {
        TimetableEntity t = new TimetableEntity();
        t.name = name;
        t.semesterId = semesterId;
        t.isActive = false;
        return db.timetableDao().insert(t);
    }

    private void saveCourse(CourseRepository repo, String name, long semId, long ttId,
                            int day, int start, int end, String pattern) throws Exception {
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
        repo.saveCourse(course, Collections.singletonList(s), null);
    }

    private List<DisplaySession> awaitWeekSessions(CourseRepository repo, int weekNo) throws Exception {
        final LiveData<List<DisplaySession>> liveData = repo.observeWeekSessions(weekNo);
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
}
