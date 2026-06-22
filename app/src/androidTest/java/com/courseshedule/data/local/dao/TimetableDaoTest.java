package com.courseshedule.data.local.dao;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.entity.SemesterEntity;
import com.courseshedule.data.local.entity.TimetableEntity;
import com.courseshedule.data.model.TimetableWithSemester;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TimetableDaoTest {

    private AppDatabase db;
    private TimetableDao dao;
    private SemesterDao semesterDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = db.timetableDao();
        semesterDao = db.semesterDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void observeAllWithSemesterReturnsJoinWithSemesterName() throws Exception {
        long semId = insertSemester("大一上", true);
        dao.insert(newTimetable("课表A", semId, false));

        List<TimetableWithSemester> result = awaitValue(dao.observeAllWithSemester());

        assertEquals(1, result.size());
        assertEquals("课表A", result.get(0).name);
        assertEquals("大一上", result.get(0).semesterName);
    }

    @Test
    public void observeAllWithSemesterActiveSemesterFirst() throws Exception {
        long sem1 = insertSemester("大一上", false);
        long sem2 = insertSemester("大一下", true);

        dao.insert(newTimetable("课表B1", sem1, false));
        dao.insert(newTimetable("课表A1", sem2, false));

        List<TimetableWithSemester> result = awaitValue(dao.observeAllWithSemester());

        assertEquals(2, result.size());
        assertEquals("大一下", result.get(0).semesterName);
        assertEquals("大一上", result.get(1).semesterName);
    }

    @Test
    public void clearAllActiveClearsAllTimetables() {
        long sem1 = insertSemester("大一上", true);
        long sem2 = insertSemester("大一下", false);
        long tt1 = dao.insert(newTimetable("课表A", sem1, false));
        long tt2 = dao.insert(newTimetable("课表B", sem2, false));

        dao.setActive(tt1);
        dao.setActive(tt2);

        dao.clearAllActive();

        assertNull(dao.getActive(sem1));
        assertNull(dao.getActive(sem2));
    }

    @Test
    public void setActiveSetsOnlyOneActive() {
        long semId = insertSemester("大一上", true);
        long tt1 = dao.insert(newTimetable("课表A", semId, false));
        long tt2 = dao.insert(newTimetable("课表B", semId, false));

        dao.setActive(tt1);
        assertEquals(tt1, dao.getActive(semId).id);

        dao.clearActive(semId);
        dao.setActive(tt2);
        assertEquals(tt2, dao.getActive(semId).id);
    }

    @Test
    public void observeActiveReturnsCorrectTimetable() throws Exception {
        long semId = insertSemester("大一上", true);
        long ttId = dao.insert(newTimetable("课表A", semId, false));
        dao.setActive(ttId);

        TimetableEntity active = awaitValue(dao.observeActive(semId));
        assertNotNull(active);
        assertEquals(ttId, active.id);
    }

    private long insertSemester(String name, boolean active) {
        SemesterEntity s = new SemesterEntity();
        s.name = name;
        s.startDate = System.currentTimeMillis();
        s.totalWeeks = 16;
        s.isActive = active;
        if (active) semesterDao.clearActive();
        return semesterDao.insert(s);
    }

    private TimetableEntity newTimetable(String name, long semesterId, boolean isActive) {
        TimetableEntity t = new TimetableEntity();
        t.name = name;
        t.semesterId = semesterId;
        t.isActive = isActive;
        return t;
    }

    private <T> T awaitValue(LiveData<T> liveData) throws Exception {
        final Object[] holder = new Object[1];
        final CountDownLatch latch = new CountDownLatch(1);
        androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation().runOnMainSync(() ->
                liveData.observeForever(value -> {
                    if (value != null) {
                        holder[0] = value;
                        latch.countDown();
                    }
                }));
        latch.await(5, TimeUnit.SECONDS);
        Thread.sleep(200);
        return (T) holder[0];
    }
}
