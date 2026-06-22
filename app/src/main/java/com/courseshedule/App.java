package com.courseshedule;

import android.app.Application;

import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.repository.SemesterRepository;
import com.courseshedule.ui.common.ThemePrefs;

/**
 * Application entry point. Owns the singleton database instance so it survives
 * configuration changes and is shared across Activities/ViewModels. Seeds the
 * semester defaults on first launch.
 */
public class App extends Application {

    private AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        database = AppDatabase.get(this);
        new ThemePrefs(this).applyCurrent();
        new SemesterRepository(database).repairDuplicateActive();
    }

    public AppDatabase getDatabase() {
        return database;
    }
}
