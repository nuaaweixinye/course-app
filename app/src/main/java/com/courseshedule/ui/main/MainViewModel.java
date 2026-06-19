package com.courseshedule.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.courseshedule.App;
import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.entity.SemesterEntity;
import com.courseshedule.data.local.entity.TimetableEntity;
import com.courseshedule.data.model.DisplaySession;
import com.courseshedule.data.model.WeekUtils;
import com.courseshedule.data.repository.CourseRepository;
import com.courseshedule.data.repository.SemesterRepository;
import com.courseshedule.data.repository.TimetableRepository;

import java.util.List;

public class MainViewModel extends AndroidViewModel {

    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final TimetableRepository timetableRepository;

    private final MutableLiveData<Integer> selectedWeek = new MutableLiveData<>();
    private final LiveData<List<DisplaySession>> sessions;
    private final MutableLiveData<SemesterEntity> activeSemester = new MutableLiveData<>();
    private final MediatorLiveData<TimetableEntity> activeTimetable = new MediatorLiveData<>();
    private final LiveData<String> activeTimetableName;
    private LiveData<TimetableEntity> activeTtSource;
    private int currentWeek = 1;
    private final java.util.List<com.courseshedule.data.model.PeriodTime> periodTimes = new java.util.ArrayList<>();

    public MainViewModel(@NonNull Application application, AppDatabase db) {
        super(application);
        courseRepository = new CourseRepository(db);
        semesterRepository = new SemesterRepository(db);
        timetableRepository = new TimetableRepository(db);

        SemesterEntity cfg = semesterRepository.getCachedOrDefault();
        applyConfig(cfg);
        activeSemester.setValue(cfg);
        selectedWeek.setValue(currentWeek);
        sessions = Transformations.switchMap(selectedWeek, week ->
                (week == null) ? new MutableLiveData<>(java.util.Collections.emptyList())
                        : courseRepository.observeWeekSessions(week));

        activeTimetableName = Transformations.map(activeTimetable, tt ->
                tt != null ? tt.name : null);

        observeActiveTimetableForSemester(cfg.id);

        if (!SemesterRepository.isCached()) {
            semesterRepository.loadAsync(() -> {
                SemesterEntity real = semesterRepository.getCachedOrDefault();
                onSemesterChanged(real);
            });
        }
    }

    private void observeActiveTimetableForSemester(long semesterId) {
        if (activeTtSource != null) {
            activeTimetable.removeSource(activeTtSource);
        }
        activeTtSource = timetableRepository.observeActive(semesterId);
        activeTimetable.addSource(activeTtSource, tt -> {
            activeTimetable.setValue(tt);
            courseRepository.setActiveTimetableId(tt != null ? tt.id : null);
        });
    }

    private void applyConfig(SemesterEntity cfg) {
        courseRepository.setActiveSemester(cfg.id);
        currentWeek = WeekUtils.currentWeek(cfg.startDate, cfg.totalWeeks,
                System.currentTimeMillis());
        periodTimes.clear();
        periodTimes.addAll(com.courseshedule.data.model.PeriodUtils.parse(cfg.periodTimesJson));
    }

    public void onSemesterChanged(SemesterEntity semester) {
        applyConfig(semester);
        activeSemester.postValue(semester);
        selectedWeek.postValue(currentWeek);
        observeActiveTimetableForSemester(semester.id);
    }

    public LiveData<SemesterEntity> getActiveSemester() {
        return activeSemester;
    }

    public LiveData<String> getActiveTimetableName() {
        return activeTimetableName;
    }

    public void setActiveTimetable(Long timetableId) {
        courseRepository.setActiveTimetableId(timetableId);
        Integer week = selectedWeek.getValue();
        if (week != null) selectedWeek.setValue(week);
    }

    public void clearActiveTimetable() {
        setActiveTimetable(null);
    }

    public LiveData<List<DisplaySession>> getSessions() {
        return sessions;
    }

    public void shiftWeek(int delta) {
        Integer current = selectedWeek.getValue();
        if (current == null) return;
        setSelectedWeek(current + delta);
    }

    public void setSelectedWeek(int week) {
        SemesterEntity sem = activeSemester.getValue();
        int total = sem != null ? sem.totalWeeks : 16;
        if (week < 1) week = 1;
        if (week > total) week = total;
        selectedWeek.setValue(week);
    }

    public LiveData<Integer> getSelectedWeek() {
        return selectedWeek;
    }

    public void jumpToCurrentWeek() {
        selectedWeek.setValue(currentWeek);
    }

    public boolean isCurrentWeekSelected() {
        Integer sel = selectedWeek.getValue();
        return sel != null && sel == currentWeek;
    }

    public int currentPeriodNow() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        int nowMinutes = c.get(java.util.Calendar.HOUR_OF_DAY) * 60
                + c.get(java.util.Calendar.MINUTE);
        return com.courseshedule.data.model.PeriodUtils.findCurrentPeriod(periodTimes, nowMinutes);
    }

    public void batchDeleteCourses(java.util.List<Long> courseIds) {
        courseRepository.batchDeleteCourses(courseIds);
    }

    public void batchMoveCourses(java.util.List<Long> courseIds, long targetTimetableId) {
        courseRepository.batchMoveCourses(courseIds, targetTimetableId);
    }

    public static class Factory extends androidx.lifecycle.ViewModelProvider.NewInstanceFactory {
        private final Application app;
        private final AppDatabase db;

        public Factory(Application app) {
            this.app = app;
            this.db = ((App) app).getDatabase();
        }

        @NonNull
        @Override
        public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
            //noinspection unchecked
            return (T) new MainViewModel(app, db);
        }
    }
}
