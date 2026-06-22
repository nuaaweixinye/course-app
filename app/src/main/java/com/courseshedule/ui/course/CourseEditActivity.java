package com.courseshedule.ui.course;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
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
    public static final String EXTRA_TIMETABLE_ID = "timetable_id";
    private static final int WEEK_CUSTOM = 3;

    private ActivityCourseEditBinding binding;
    private CourseEditViewModel viewModel;

    private final List<View> sessionRows = new ArrayList<>();
    private int selectedColorTag = 0;
    private int totalWeeks = SemesterEntity.DEFAULT_TOTAL_WEEKS;

    private TimetableRepository timetableRepo;
    private List<TimetableEntity> timetableList = new ArrayList<>();
    private Long extraTimetableId;
    private boolean timetableNoticeAdded;

    private static class SessionRowData {
        long sessionId = 0;
        String customWeekPattern = null;
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

        extraTimetableId = getIntent().hasExtra(EXTRA_TIMETABLE_ID)
                ? getIntent().getLongExtra(EXTRA_TIMETABLE_ID, -1L) : null;

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
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < ColorPalette.SIZE; i++) {
            final int tag = i;
            View swatch = new View(this);
            int size = (int) (36 * density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginEnd((int) (8 * density));
            swatch.setLayoutParams(lp);
            swatch.setBackgroundResource(R.drawable.bg_color_swatch);
            swatch.getBackground().mutate();
            swatch.getBackground().setTint(
                    androidx.core.content.ContextCompat.getColor(this, ColorPalette.colorRes(i)));
            swatch.setOnClickListener(v -> selectColor(tag));
            row.addView(swatch);
        }
    }

    private void selectColor(int tag) {
        selectedColorTag = tag;
        LinearLayout row = binding.colorRow;
        int onSurface = com.google.android.material.color.MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOnSurface, 0xFF000000);
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < row.getChildCount(); i++) {
            View swatch = row.getChildAt(i);
            android.graphics.drawable.GradientDrawable gd =
                    (android.graphics.drawable.GradientDrawable) swatch.getBackground();
            if (i == tag) {
                gd.setStroke((int) (3 * density), onSurface);
            } else {
                gd.setStroke((int) (2 * density), android.graphics.Color.WHITE);
            }
        }
    }

    private void onLoaded(CourseEditViewModel.Loaded loaded) {
        if (loaded == null) return;
        binding.sessionsContainer.removeAllViews();
        sessionRows.clear();
        selectColor(loaded.colorTag);
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

        final boolean isEdit = (loaded.course != null);
        final Long currentTtId = loaded.course != null ? loaded.course.timetableId : null;

        new Thread(() -> {
            AppDatabase db = ((App) getApplication()).getDatabase();
            TimetableEntity globalActive = db.timetableDao().getActiveGlobal();

            long semId;
            if (extraTimetableId != null) {
                TimetableEntity extraTt = db.timetableDao().getById(extraTimetableId);
                if (extraTt != null) {
                    semId = extraTt.semesterId;
                } else {
                    semId = globalActive != null ? globalActive.semesterId : 0;
                }
            } else if (isEdit) {
                semId = loaded.course.semesterId;
            } else if (globalActive != null) {
                semId = globalActive.semesterId;
            } else {
                semId = new SemesterRepository(db).getCachedOrDefault().id;
            }

            java.util.List<TimetableEntity> list = db.timetableDao().listBySemester(semId);

            final Long finalSemId = semId;
            runOnUiThread(() -> {
                timetableList = list;
                if (timetableList.isEmpty()) {
                    binding.spinTimetable.setVisibility(View.GONE);
                    if (timetableNoticeAdded) return;
                    timetableNoticeAdded = true;
                    TextView notice = new TextView(this);
                    notice.setText(R.string.hint_create_timetable_first);
                    notice.setPadding(0, 8, 0, 0);
                    notice.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
                    int hintColor = com.google.android.material.color.MaterialColors.getColor(
                            this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF666666);
                    notice.setTextColor(hintColor);
                    ((ViewGroup) binding.spinTimetable.getParent()).addView(notice);
                    return;
                }

                String[] timetableNames = new String[timetableList.size()];
                for (int i = 0; i < timetableList.size(); i++) {
                    timetableNames[i] = timetableList.get(i).name;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, timetableNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.spinTimetable.setAdapter(adapter);

                Long targetTtId;
                if (currentTtId != null) {
                    targetTtId = currentTtId;
                } else if (extraTimetableId != null) {
                    targetTtId = extraTimetableId;
                } else if (globalActive != null) {
                    targetTtId = globalActive.id;
                } else {
                    targetTtId = null;
                }

                if (targetTtId != null) {
                    for (int i = 0; i < timetableList.size(); i++) {
                        if (timetableList.get(i).id == targetTtId) {
                            binding.spinTimetable.setSelection(i);
                            break;
                        }
                    }
                }
            });
        }).start();
    }

    private String[] buildWeekPresets() {
        return new String[]{
                String.format(getString(R.string.preset_all_weeks), totalWeeks),
                getString(R.string.preset_odd_weeks),
                getString(R.string.preset_even_weeks),
                getString(R.string.preset_custom_weeks)
        };
    }

    private void addSessionRow(CourseSessionEntity existing) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_session_editor,
                binding.sessionsContainer, false);

        SessionRowData data = new SessionRowData();
        row.setTag(data);

        Spinner weekSpin = row.findViewById(R.id.spinWeek);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, buildWeekPresets());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weekSpin.setAdapter(adapter);

        int initialPreset;
        if (existing != null) {
            ((Spinner) row.findViewById(R.id.spinDay)).setSelection(existing.dayOfWeek - 1);
            ((EditText) row.findViewById(R.id.etStart)).setText(String.valueOf(existing.startPeriod));
            ((EditText) row.findViewById(R.id.etEnd)).setText(String.valueOf(existing.endPeriod));
            ((EditText) row.findViewById(R.id.etLocation)).setText(existing.location);
            data.sessionId = existing.id;
            initialPreset = presetIndexFor(existing.weekPattern);
            if (initialPreset == WEEK_CUSTOM) {
                data.customWeekPattern = existing.weekPattern;
            }
        } else {
            ((EditText) row.findViewById(R.id.etEnd)).setText(String.valueOf(
                    Math.min(totalWeeks, 2)));
            initialPreset = 0;
        }

        final int finalInitialPreset = initialPreset;
        weekSpin.setSelection(initialPreset);

        weekSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            int lastPos = finalInitialPreset;
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == WEEK_CUSTOM && lastPos != WEEK_CUSTOM) {
                    SessionRowData d = (SessionRowData) row.getTag();
                    String currentPattern = d.customWeekPattern;
                    if (currentPattern == null) {
                        currentPattern = patternForPreset(lastPos);
                    }
                    final int revertPos = lastPos;
                    WeekPickerDialog.show(CourseEditActivity.this, totalWeeks, currentPattern,
                            pattern -> d.customWeekPattern = pattern,
                            () -> weekSpin.setSelection(revertPos));
                }
                lastPos = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

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
        return WEEK_CUSTOM;
    }

    private String patternForPreset(int index) {
        switch (index) {
            case 1: return WeekUtils.oddWeeks(totalWeeks);
            case 2: return WeekUtils.evenWeeks(totalWeeks);
            default: return WeekUtils.allWeeks(totalWeeks);
        }
    }

    private void onSave() {
        int ttPos = binding.spinTimetable.getSelectedItemPosition();
        if (ttPos < 0 || timetableList.isEmpty()) {
            Toast.makeText(this, R.string.err_no_timetable, Toast.LENGTH_SHORT).show();
            return;
        }

        CourseEntity course = new CourseEntity();
        course.name = textOf(binding.etName);
        course.teacher = textOf(binding.etTeacher);
        course.note = textOf(binding.etNote);
        course.colorTag = selectedColorTag;
        course.timetableId = timetableList.get(ttPos).id;
        course.semesterId = timetableList.get(ttPos).semesterId;

        List<CourseSessionEntity> sessions = new ArrayList<>();
        for (View row : sessionRows) {
            CourseSessionEntity s = new CourseSessionEntity();
            SessionRowData data = (SessionRowData) row.getTag();
            s.id = data.sessionId;
            s.dayOfWeek = ((Spinner) row.findViewById(R.id.spinDay)).getSelectedItemPosition() + 1;
            s.startPeriod = parseInt(((EditText) row.findViewById(R.id.etStart)).getText(), 1);
            s.endPeriod = parseInt(((EditText) row.findViewById(R.id.etEnd)).getText(), 1);
            s.location = textOf((EditText) row.findViewById(R.id.etLocation));
            int weekPos = ((Spinner) row.findViewById(R.id.spinWeek)).getSelectedItemPosition();
            if (weekPos == WEEK_CUSTOM && data.customWeekPattern != null) {
                s.weekPattern = data.customWeekPattern;
            } else {
                s.weekPattern = patternForPreset(weekPos);
            }
            sessions.add(s);
        }

        Integer error = viewModel.save(course, sessions, () -> {
            com.courseshedule.widget.TodayWidgetProvider.refresh(this);
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.toast_course_saved, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        }
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
