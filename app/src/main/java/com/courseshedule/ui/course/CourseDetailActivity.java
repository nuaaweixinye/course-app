package com.courseshedule.ui.course;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.courseshedule.App;
import com.courseshedule.R;
import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.entity.CourseEntity;
import com.courseshedule.data.local.entity.CourseSessionEntity;
import com.courseshedule.data.local.entity.SessionExceptionEntity;
import com.courseshedule.data.repository.CourseRepository;
import com.courseshedule.databinding.ActivityCourseDetailBinding;
import com.courseshedule.ui.common.ColorPalette;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourseDetailActivity extends AppCompatActivity {

    public static final String EXTRA_COURSE_ID = "course_id";

    private ActivityCourseDetailBinding binding;
    private CourseRepository repository;
    private long courseId;
    private CourseEntity currentCourse;
    private List<CourseSessionEntity> currentSessions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        courseId = getIntent().getLongExtra(EXTRA_COURSE_ID, -1L);
        if (courseId < 0) { finish(); return; }

        AppDatabase db = ((App) getApplication()).getDatabase();
        repository = new CourseRepository(db);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit) {
                Intent intent = new Intent(this, CourseEditActivity.class);
                intent.putExtra(CourseEditActivity.EXTRA_COURSE_ID, courseId);
                startActivity(intent);
                return true;
            } else if (id == R.id.action_delete) {
                confirmDelete();
                return true;
            } else if (id == R.id.action_exceptions) {
                openExceptions();
                return true;
            }
            return false;
        });

        observeCourse();
        observeSessions();
    }

    private void observeCourse() {
        LiveData<CourseEntity> live = repository.observeCourse(courseId);
        live.observe(this, new Observer<CourseEntity>() {
            @Override
            public void onChanged(CourseEntity course) {
                if (course == null) { finish(); return; }
                currentCourse = course;
                binding.tvCourseName.setText(course.name);
                binding.tvTeacher.setText(course.teacher);
                int color = ContextCompat.getColor(CourseDetailActivity.this,
                        ColorPalette.colorRes(course.colorTag));
                binding.colorBar.setBackgroundColor(color);
                binding.tvNote.setText(course.note);
                binding.tvNote.setVisibility(
                        course.note == null || course.note.isEmpty() ? View.GONE : View.VISIBLE);
                if (course.timetableId != null) {
                    final long ttId = course.timetableId;
                    new Thread(() -> {
                        com.courseshedule.data.local.entity.TimetableEntity tt =
                                ((com.courseshedule.data.repository.TimetableRepository)
                                        new com.courseshedule.data.repository.TimetableRepository(
                                                ((App) getApplication()).getDatabase()))
                                        .listBySemester(course.semesterId)
                                        .stream()
                                        .filter(t -> t.id == ttId)
                                        .findFirst()
                                        .orElse(null);
                        final String ttName = tt != null ? tt.name : null;
                        runOnUiThread(() -> {
                            if (ttName != null) {
                                binding.tvProfiles.setVisibility(View.VISIBLE);
                                binding.tvProfiles.setText(getString(R.string.timetable_label, ttName));
                            } else {
                                binding.tvProfiles.setVisibility(View.GONE);
                            }
                        });
                    }).start();
                } else {
                    binding.tvProfiles.setVisibility(View.GONE);
                }
            }
        });
    }

    private void observeSessions() {
        repository.observeSessions(courseId).observe(this, this::renderSessions);
    }

    private void renderSessions(@NonNull List<CourseSessionEntity> sessions) {
        currentSessions = sessions;
        LinearLayout container = binding.sessionsContainer;
        container.removeAllViews();
        if (sessions.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.empty_sessions);
            empty.setTextColor(com.google.android.material.color.MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorOnSurfaceVariant,
                    android.graphics.Color.GRAY));
            container.addView(empty);
            return;
        }
        String[] dayNames = getResources().getStringArray(R.array.weekday_full);
        LayoutInflater inflater = LayoutInflater.from(this);
        List<Long> sessionIds = new ArrayList<>();
        for (CourseSessionEntity s : sessions) sessionIds.add(s.id);
        new Thread(() -> {
            AppDatabase db = ((App) getApplication()).getDatabase();
            List<SessionExceptionEntity> allExceptions = db.sessionExceptionDao().listBySessionIds(sessionIds);
            Map<Long, List<SessionExceptionEntity>> exMap = new HashMap<>();
            for (SessionExceptionEntity e : allExceptions) {
                List<SessionExceptionEntity> list = exMap.get(e.sessionId);
                if (list == null) { list = new ArrayList<>(); exMap.put(e.sessionId, list); }
                list.add(e);
            }
            runOnUiThread(() -> renderSessionRows(sessions, exMap, dayNames, inflater));
        }).start();
    }

    private void renderSessionRows(List<CourseSessionEntity> sessions,
                                   Map<Long, List<SessionExceptionEntity>> exMap,
                                   String[] dayNames, LayoutInflater inflater) {
        LinearLayout container = binding.sessionsContainer;
        for (CourseSessionEntity s : sessions) {
            View row = inflater.inflate(R.layout.item_session_row, container, false);
            String day = (s.dayOfWeek >= 1 && s.dayOfWeek <= 7) ? dayNames[s.dayOfWeek - 1] : "";
            ((TextView) row.findViewById(R.id.tvSessionPrimary)).setText(
                    getString(R.string.session_primary, day, s.startPeriod, s.endPeriod));
            ((TextView) row.findViewById(R.id.tvSessionSecondary)).setText(
                    getString(R.string.session_secondary,
                            s.location == null ? "" : s.location,
                            s.weekPattern == null ? "" : s.weekPattern));
            buildWeekBar(row, s.weekPattern);
            loadExceptionInfo(row, exMap.get(s.id));
            row.setOnClickListener(v -> {
                WeekExceptionsDialog.show(CourseDetailActivity.this, s.id, null);
            });
            container.addView(row);
        }
    }

    private void buildWeekBar(View row, String weekPattern) {
        LinearLayout bar = row.findViewById(R.id.weekBar);
        bar.removeAllViews();
        if (weekPattern == null || weekPattern.length() < 4) return;
        int total = Math.min(weekPattern.length(), 25);
        int activeColor = com.google.android.material.color.MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorPrimaryContainer,
                android.graphics.Color.GRAY);
        int inactiveColor = com.google.android.material.color.MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorSurfaceVariant, android.graphics.Color.LTGRAY);
        int dotSize = (int) (8 * getResources().getDisplayMetrics().density);
        int margin = (int) (2 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < total; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dotSize, dotSize);
            lp.setMarginEnd(margin);
            dot.setLayoutParams(lp);
            boolean active = i < weekPattern.length() && weekPattern.charAt(i) == '1';
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(active ? activeColor : inactiveColor);
            dot.setBackground(gd);
            bar.addView(dot);
        }
    }

    private void loadExceptionInfo(View row, List<SessionExceptionEntity> exceptions) {
        TextView tv = row.findViewById(R.id.tvExceptionInfo);
        if (exceptions == null || exceptions.isEmpty()) {
            tv.setVisibility(View.GONE);
            return;
        }
        int cancelCount = 0, moveCount = 0;
        for (SessionExceptionEntity e : exceptions) {
            if (e.type == SessionExceptionEntity.TYPE_CANCEL) cancelCount++;
            else if (e.type == SessionExceptionEntity.TYPE_MOVED) moveCount++;
        }
        StringBuilder sb = new StringBuilder();
        if (cancelCount > 0) sb.append(getString(R.string.exception_cancel_count, cancelCount));
        if (moveCount > 0) {
            if (sb.length() > 0) sb.append("，");
            sb.append(getString(R.string.exception_move_count, moveCount));
        }
        tv.setText(sb.toString());
        tv.setVisibility(View.VISIBLE);
    }

    private void confirmDelete() {
        if (currentCourse == null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_message, currentCourse.name))
                .setPositiveButton(R.string.action_delete,
                        (d, w) -> {
                            repository.deleteCourse(currentCourse);
                            com.courseshedule.widget.TodayWidgetProvider.refresh(this);
                            Toast.makeText(this, R.string.toast_course_deleted,
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openExceptions() {
        if (currentSessions.isEmpty()) {
            Toast.makeText(this, R.string.empty_sessions, Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentSessions.size() == 1) {
            WeekExceptionsDialog.show(this, currentSessions.get(0).id, null);
            return;
        }
        String[] labels = new String[currentSessions.size()];
        String[] dayNames = getResources().getStringArray(R.array.weekday_short);
        for (int i = 0; i < currentSessions.size(); i++) {
            CourseSessionEntity s = currentSessions.get(i);
            String day = (s.dayOfWeek >= 1 && s.dayOfWeek <= 7) ? dayNames[s.dayOfWeek - 1] : "";
            labels[i] = getString(R.string.session_primary, day, s.startPeriod, s.endPeriod);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.header_exceptions)
                .setItems(labels, (d, which) ->
                        WeekExceptionsDialog.show(this, currentSessions.get(which).id, null))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
