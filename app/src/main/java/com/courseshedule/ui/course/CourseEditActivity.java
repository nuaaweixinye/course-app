package com.courseshedule.ui.course;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.courseshedule.App;
import com.courseshedule.R;
import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.entity.CourseEntity;
import com.courseshedule.data.local.entity.CourseSessionEntity;
import com.courseshedule.data.local.entity.TimetableEntity;
import com.courseshedule.data.model.WeekUtils;
import com.courseshedule.data.local.entity.SemesterEntity;
import com.courseshedule.data.repository.SemesterRepository;
import com.courseshedule.data.repository.TimetableRepository;
import com.courseshedule.databinding.ActivityCourseEditBinding;
import com.courseshedule.ui.common.ColorPalette;

import java.util.ArrayList;
import java.util.List;

public class CourseEditActivity extends AppCompatActivity {

    public static final String EXTRA_COURSE_ID = "course_id";

    private ActivityCourseEditBinding binding;
    private CourseEditViewModel viewModel;

    private final List<View> sessionRows = new ArrayList<>();
    private int selectedColorTag = 0;
    private int totalWeeks = SemesterEntity.DEFAULT_TOTAL_WEEKS;
    private final String[] weekPresets;

    private TimetableRepository timetableRepo;
    private List<TimetableEntity> timetableList = new ArrayList<>();

    public CourseEditActivity() {
        weekPresets = new String[]{
                "全周", "单周", "双周"
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this, new CourseEditViewModel.Factory(getApplication()))
                .get(CourseEditViewModel.class);

        App app = (App) getApplication();
        timetableRepo = new TimetableRepository(app.getDatabase());

        SemesterEntity cfg = new SemesterRepository(app.getDatabase())
                .getCachedOrDefault();
        totalWeeks = cfg.totalWeeks;

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_save) {
                onSave();
                return true;
            }
            return false;
        });

        binding.btnAddSession.setOnClickListener(v -> addSessionRow(null));
        binding.toolbar.setTitle(R.string.fab_add_course);

        buildColorSwatches();

        viewModel.getLoaded().observe(this, this::onLoaded);

        long courseId = getIntent().getLongExtra(EXTRA_COURSE_ID, -1L);
        if (courseId >= 0) {
            viewModel.initEdit(courseId);
            binding.toolbar.setTitle(R.string.action_edit);
        } else {
            viewModel.initCreate(ColorPalette.defaultTag(0));
        }
    }

    private void buildColorSwatches() {
        LinearLayout row = binding.colorRow;
        for (int i = 0; i < ColorPalette.SIZE; i++) {
            final int tag = i;
            View swatch = new View(this);
            int size = (int) (36 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
            swatch.setLayoutParams(lp);
            swatch.setBackgroundResource(R.drawable.bg_color_swatch);
            swatch.getBackground().setTint(
                    androidx.core.content.ContextCompat.getColor(this, ColorPalette.colorRes(i)));
            swatch.setOnClickListener(v -> selectColor(tag));
            row.addView(swatch);
        }
    }

    private void selectColor(int tag) {
        selectedColorTag = tag;
    }

    private void onLoaded(CourseEditViewModel.Loaded loaded) {
        if (loaded == null) return;
        binding.sessionsContainer.removeAllViews();
        sessionRows.clear();
        selectedColorTag = loaded.colorTag;
        if (loaded.course != null) {
            binding.etName.setText(loaded.course.name);
            binding.etTeacher.setText(loaded.course.teacher);
            binding.etNote.setText(loaded.course.note);
        }
        if (loaded.sessions.isEmpty()) {
            addSessionRow(null);
        } else {
            for (CourseSessionEntity s : loaded.sessions) addSessionRow(s);
        }

        long semId = loaded.course != null ? loaded.course.semesterId
                : new SemesterRepository(((App) getApplication()).getDatabase())
                        .getCachedOrDefault().id;
        final Long currentTtId = loaded.course != null ? loaded.course.timetableId : null;
        new Thread(() -> {
            AppDatabase db = ((App) getApplication()).getDatabase();
            java.util.List<TimetableEntity> list = db.timetableDao().listBySemester(semId);
            runOnUiThread(() -> {
                timetableList = list;
                String[] timetableNames = new String[timetableList.size() + 1];
                timetableNames[0] = getString(R.string.timetable_none);
                for (int i = 0; i < timetableList.size(); i++) {
                    timetableNames[i + 1] = timetableList.get(i).name;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, timetableNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.spinTimetable.setAdapter(adapter);
                if (currentTtId != null) {
                    for (int i = 0; i < timetableList.size(); i++) {
                        if (timetableList.get(i).id == currentTtId) {
                            binding.spinTimetable.setSelection(i + 1);
                            break;
                        }
                    }
                }
            });
        }).start();
    }

    private void addSessionRow(CourseSessionEntity existing) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_session_editor,
                binding.sessionsContainer, false);

        Spinner weekSpin = row.findViewById(R.id.spinWeek);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, weekPresets);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weekSpin.setAdapter(adapter);

        if (existing != null) {
            ((Spinner) row.findViewById(R.id.spinDay)).setSelection(existing.dayOfWeek - 1);
            ((EditText) row.findViewById(R.id.etStart)).setText(String.valueOf(existing.startPeriod));
            ((EditText) row.findViewById(R.id.etEnd)).setText(String.valueOf(existing.endPeriod));
            ((EditText) row.findViewById(R.id.etLocation)).setText(existing.location);
            weekSpin.setSelection(presetIndexFor(existing.weekPattern));
        } else {
            ((EditText) row.findViewById(R.id.etEnd)).setText(String.valueOf(
                    Math.min(totalWeeks, 2)));
        }

        row.findViewById(R.id.btnRemove).setOnClickListener(v -> {
            binding.sessionsContainer.removeView(row);
            sessionRows.remove(row);
        });

        binding.sessionsContainer.addView(row);
        sessionRows.add(row);
    }

    private int presetIndexFor(String pattern) {
        if (pattern == null) return 0;
        if (pattern.equals(WeekUtils.allWeeks(totalWeeks))) return 0;
        if (pattern.equals(WeekUtils.oddWeeks(totalWeeks))) return 1;
        if (pattern.equals(WeekUtils.evenWeeks(totalWeeks))) return 2;
        return 0;
    }

    private String patternForPreset(int index) {
        switch (index) {
            case 1: return WeekUtils.oddWeeks(totalWeeks);
            case 2: return WeekUtils.evenWeeks(totalWeeks);
            default: return WeekUtils.allWeeks(totalWeeks);
        }
    }

    private void onSave() {
        CourseEntity course = new CourseEntity();
        course.name = textOf(binding.etName);
        course.teacher = textOf(binding.etTeacher);
        course.note = textOf(binding.etNote);
        course.colorTag = selectedColorTag;
        course.semesterId = new SemesterRepository(((App) getApplication()).getDatabase())
                .getCachedOrDefault().id;

        int ttPos = binding.spinTimetable.getSelectedItemPosition();
        course.timetableId = ttPos > 0 ? timetableList.get(ttPos - 1).id : null;

        List<CourseSessionEntity> sessions = new ArrayList<>();
        for (View row : sessionRows) {
            CourseSessionEntity s = new CourseSessionEntity();
            s.dayOfWeek = ((Spinner) row.findViewById(R.id.spinDay)).getSelectedItemPosition() + 1;
            s.startPeriod = parseInt(((EditText) row.findViewById(R.id.etStart)).getText(), 1);
            s.endPeriod = parseInt(((EditText) row.findViewById(R.id.etEnd)).getText(), 1);
            s.location = textOf((EditText) row.findViewById(R.id.etLocation));
            s.weekPattern = patternForPreset(((Spinner) row.findViewById(R.id.spinWeek))
                    .getSelectedItemPosition());
            sessions.add(s);
        }

        Integer error = viewModel.save(course, sessions);
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            return;
        }
        com.courseshedule.widget.TodayWidgetProvider.refresh(this);
        Toast.makeText(this, R.string.toast_course_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private static String textOf(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private static int parseInt(CharSequence cs, int fallback) {
        try {
            return Integer.parseInt(cs.toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
