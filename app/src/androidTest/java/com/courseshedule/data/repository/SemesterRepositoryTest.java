package com.courseshedule.data.repository;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.dao.SemesterDao;
import com.courseshedule.data.local.entity.SemesterEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SemesterRepositoryTest {

    private AppDatabase db;
    private SemesterRepository repository;
    private SemesterDao semesterDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        repository = new SemesterRepository(db);
        semesterDao = db.semesterDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void createDoesNotAutoActivate() throws Exception {
        insertSemester("大一上", true);

        AtomicReference<Long> createdId = new AtomicReference<>();
        repository.create("大一下", System.currentTimeMillis(), 16, id -> createdId.set(id));
        Thread.sleep(300);

        Long id = createdId.get();
        assertNotNull(id);
        SemesterEntity created = semesterDao.getById(id);
        assertFalse("New semester should not be auto-activated", created.isActive);
    }

    @Test
    public void switchToSetsTargetAsSoleActive() throws Exception {
        long sem1 = insertSemester("大一上", true);
        long sem2 = insertSemester("大一下", false);

        repository.switchTo(sem2);
        Thread.sleep(300);

        SemesterEntity active = semesterDao.getActive();
        assertNotNull(active);
        assertEquals(sem2, active.id);

        SemesterEntity old = semesterDao.getById(sem1);
        assertFalse("Old semester should be deactivated", old.isActive);
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
}
