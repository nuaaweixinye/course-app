package com.courseshedule.data.repository;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.dao.SemesterDao;
import com.courseshedule.data.local.dao.TimetableDao;
import com.courseshedule.data.local.entity.SemesterEntity;
import com.courseshedule.data.local.entity.TimetableEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TimetableRepositoryTest {

    private AppDatabase db;
    private TimetableRepository repository;
    private SemesterDao semesterDao;
    private TimetableDao timetableDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        repository = new TimetableRepository(db);
        semesterDao = db.semesterDao();
        timetableDao = db.timetableDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void switchToActivatesTargetSemester() throws Exception {
        long sem1 = insertSemester("大一上", true);
        long sem2 = insertSemester("大一下", false);
        long tt2 = insertTimetable("课表B", sem2, false);

        repository.switchTo(tt2, sem2);
        Thread.sleep(300);

        SemesterEntity active = semesterDao.getActive();
        assertNotNull(active);
        assertEquals(sem2, active.id);
    }

    @Test
    public void switchToClearsPreviouslyActiveTimetableInDifferentSemester() throws Exception {
        long sem1 = insertSemester("大一上", true);
        long sem2 = insertSemester("大一下", false);
        long tt1 = insertTimetable("课表A", sem1, false);
        long tt2 = insertTimetable("课表B", sem2, false);

        timetableDao.setActive(tt1);
        Thread.sleep(100);

        repository.switchTo(tt2, sem2);
        Thread.sleep(300);

        TimetableEntity active1 = timetableDao.getActive(sem1);
        TimetableEntity active2 = timetableDao.getActive(sem2);

        assertNull("Old semester's timetable should be inactive", active1);
        assertNotNull("New semester's timetable should be active", active2);
        assertEquals(tt2, active2.id);
    }

    @Test
    public void createLinksToCorrectSemester() throws Exception {
        long semId = insertSemester("大一上", true);

        AtomicReference<Long> createdId = new AtomicReference<>();
        repository.create("新课表", semId, id -> createdId.set(id));
        Thread.sleep(300);

        Long id = createdId.get();
        assertNotNull("Create callback should fire", id);
        TimetableEntity tt = timetableDao.getById(id);
        assertNotNull(tt);
        assertEquals("新课表", tt.name);
        assertEquals(semId, tt.semesterId);
        assertFalse("New timetable should not be active", tt.isActive);
    }

    private long insertSemester(String name, boolean active) {
        if (active) semesterDao.clearActive();
        SemesterEntity s = new SemesterEntity();
        s.name = name;
        s.startDate = System.currentTimeMillis();
        s.totalWeeks = 16;
        s.isActive = active;
        return semesterDao.insert(s);
    }

    private long insertTimetable(String name, long semesterId, boolean isActive) {
        TimetableEntity t = new TimetableEntity();
        t.name = name;
        t.semesterId = semesterId;
        t.isActive = isActive;
        return timetableDao.insert(t);
    }
}
