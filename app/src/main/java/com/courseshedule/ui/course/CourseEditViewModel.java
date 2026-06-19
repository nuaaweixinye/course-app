package com.courseshedule.ui.course;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.courseshedule.App;
import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.entity.CourseEntity;
import com.courseshedule.data.local.entity.CourseSessionEntity;
import com.courseshedule.data.repository.CourseRepository;

import java.util.ArrayList;
import java.util.List;

/** Loads an existing course+sessions for edit mode; saves create/update. */
public class CourseEditViewModel extends AndroidViewModel {

    public static final int MODE_CREATE = 0;
    public static final int MODE_EDIT = 1;

    private final CourseRepository repository;
    private final MutableLiveData<Loaded> loaded = new MutableLiveData<>();
    private int mode = MODE_CREATE;
    private long courseId = -1;
    private int defaultColorTag = 0;

    public CourseEditViewModel(@NonNull Application application, AppDatabase db) {
        super(application);
        repository = new CourseRepository(db);
    }

    public void initCreate(int defaultColorTag) {
        this.mode = MODE_CREATE;
        this.defaultColorTag = defaultColorTag;
        loaded.setValue(new Loaded(null, new ArrayList<>(), defaultColorTag));
    }

    public void initEdit(long courseId) {
        this.mode = MODE_EDIT;
        this.courseId = courseId;
        new Thread(() -> {
            AppDatabase db = ((App) getApplication()).getDatabase();
            CourseEntity course = db.courseDao().getById(courseId);
            List<CourseSessionEntity> sessions = db.courseSessionDao().listByCourse(courseId);
            int color = course != null ? course.colorTag : 0;
            Loaded l = new Loaded(course, sessions, color);
            getApplication().getMainExecutor().execute(() -> loaded.setValue(l));
        }).start();
    }

    public LiveData<Loaded> getLoaded() {
        return loaded;
    }

    public int getMode() { return mode; }

    /** Returns null on success, or an error string resource id on validation failure. */
    public Integer save(CourseEntity course, List<CourseSessionEntity> sessions) {
        if (course.name == null || course.name.trim().isEmpty()) {
            return com.courseshedule.R.string.err_name_required;
        }
        if (sessions == null || sessions.isEmpty()) {
            return com.courseshedule.R.string.err_no_sessions;
        }
        for (CourseSessionEntity s : sessions) {
            if (s.endPeriod < s.startPeriod) {
                return com.courseshedule.R.string.err_periods_invalid;
            }
        }
        if (mode == MODE_CREATE) {
            repository.saveCourse(course, sessions, null);
        } else {
            course.id = courseId;
            repository.updateCourse(course, sessions);
        }
        return null;
    }

    static class Loaded {
        final CourseEntity course;
        final List<CourseSessionEntity> sessions;
        final int colorTag;

        Loaded(CourseEntity course, List<CourseSessionEntity> sessions, int colorTag) {
            this.course = course;
            this.sessions = sessions;
            this.colorTag = colorTag;
        }
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
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            //noinspection unchecked
            return (T) new CourseEditViewModel(app, db);
        }
    }
}
