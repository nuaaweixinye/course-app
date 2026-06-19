package com.courseshedule.ui.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.courseshedule.App;
import com.courseshedule.R;
import com.courseshedule.data.repository.SemesterRepository;
import com.courseshedule.databinding.ActivityMainBinding;
import com.courseshedule.ui.settings.SettingsActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private TimetableView currentGrid;
    private Set<Long> selectedCourseIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this, new MainViewModel.Factory(getApplication()))
                .get(MainViewModel.class);

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_add_course) {
                startActivity(new Intent(this, com.courseshedule.ui.course.CourseEditActivity.class));
                return true;
            } else if (id == R.id.action_this_week) {
                viewModel.jumpToCurrentWeek();
                return true;
            } else if (id == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });

        binding.btnPrevWeek.setOnClickListener(v -> viewModel.shiftWeek(-1));
        binding.btnNextWeek.setOnClickListener(v -> viewModel.shiftWeek(1));

        binding.etWeek.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                applyWeekInput();
                return true;
            }
            return false;
        });
        binding.etWeek.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) applyWeekInput();
        });

        binding.bottomNav.setSelectedItemId(R.id.tab_timetable);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.tab_tasks) {
                startActivity(new Intent(this, com.courseshedule.ui.task.TaskListActivity.class));
                return false;
            } else if (id == R.id.tab_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return false;
            }
            return true;
        });

        setupBatchActions();
        observeViewModel();
        setupSemesterPicker();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    private void setupBatchActions() {
        binding.btnCloseSelection.setOnClickListener(v -> exitSelectionMode());
        binding.btnBatchDelete.setOnClickListener(v -> {
            if (selectedCourseIds.isEmpty()) return;
            new AlertDialog.Builder(this)
                    .setMessage(R.string.confirm_batch_delete)
                    .setPositiveButton(android.R.string.ok, (d, w) -> {
                        viewModel.batchDeleteCourses(new ArrayList<>(selectedCourseIds));
                        exitSelectionMode();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
        binding.btnBatchMove.setOnClickListener(v -> showMoveDialog());
        binding.btnBatchCancel.setOnClickListener(v -> {
            Toast.makeText(this, R.string.batch_cancel_unavailable, Toast.LENGTH_SHORT).show();
        });
    }

    private void exitSelectionMode() {
        if (currentGrid != null) currentGrid.exitSelectionMode();
        binding.selectionBar.setVisibility(View.GONE);
        selectedCourseIds.clear();
    }

    private void showMoveDialog() {
        com.courseshedule.data.repository.SemesterRepository semRepo =
                new com.courseshedule.data.repository.SemesterRepository(
                        ((App) getApplication()).getDatabase());
        com.courseshedule.data.local.entity.SemesterEntity active = semRepo.getCachedOrDefault();
        com.courseshedule.data.repository.TimetableRepository ttRepo =
                new com.courseshedule.data.repository.TimetableRepository(
                        ((App) getApplication()).getDatabase());
        ttRepo.observeBySemester(active.id).observe(MainActivity.this, new androidx.lifecycle.Observer<java.util.List<com.courseshedule.data.local.entity.TimetableEntity>>() {
            @Override
            public void onChanged(java.util.List<com.courseshedule.data.local.entity.TimetableEntity> timetables) {
                ttRepo.observeBySemester(active.id).removeObserver(this);
                if (timetables == null || timetables.isEmpty()) return;
                String[] names = new String[timetables.size()];
                final long[] ids = new long[timetables.size()];
                for (int i = 0; i < timetables.size(); i++) {
                    names[i] = timetables.get(i).name;
                    ids[i] = timetables.get(i).id;
                }
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.batch_move_title)
                        .setItems(names, (d, which) -> {
                            viewModel.batchMoveCourses(new ArrayList<>(selectedCourseIds), ids[which]);
                            exitSelectionMode();
                            Toast.makeText(MainActivity.this, R.string.toast_batch_moved, Toast.LENGTH_SHORT).show();
                        })
                        .show();
            }
        });
    }

    private void setupSemesterPicker() {
        SemesterRepository repo = new SemesterRepository(((App) getApplication()).getDatabase());
        repo.observeActive().observe(this, semester -> {
            if (semester != null) binding.tvSemesterLabel.setText(semester.name);
            else binding.tvSemesterLabel.setText(R.string.tab_timetable);
        });
    }

    private void observeViewModel() {
        viewModel.getSessions().observe(this, sessions -> renderGrid(sessions));
        viewModel.getSelectedWeek().observe(this, week -> {
            if (week == null) return;
            if (binding.etWeek.hasFocus()) return;
            String current = binding.etWeek.getText().toString().trim();
            if (!String.valueOf(week).equals(current)) {
                binding.etWeek.setText(String.valueOf(week));
            }
        });
        viewModel.getActiveTimetableName().observe(this, name -> {
            if (name != null) {
                binding.tvActiveTimetable.setText(name);
            } else {
                binding.tvActiveTimetable.setText(R.string.timetable_unassigned);
            }
        });
    }

    private void applyWeekInput() {
        String s = binding.etWeek.getText().toString().trim();
        int week;
        try {
            week = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            week = viewModel.getSelectedWeek().getValue() != null
                    ? viewModel.getSelectedWeek().getValue() : 1;
        }
        viewModel.setSelectedWeek(week);
        Integer applied = viewModel.getSelectedWeek().getValue();
        if (applied != null) binding.etWeek.setText(String.valueOf(applied));
        binding.etWeek.clearFocus();
        hideKeyboard();
    }

    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(binding.etWeek.getWindowToken(), 0);
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN && binding.etWeek.hasFocus()) {
            int[] loc = new int[2];
            binding.etWeek.getLocationOnScreen(loc);
            android.graphics.Rect r = new android.graphics.Rect(loc[0], loc[1],
                    loc[0] + binding.etWeek.getWidth(), loc[1] + binding.etWeek.getHeight());
            if (!r.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                binding.etWeek.clearFocus();
                hideKeyboard();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void renderGrid(java.util.List<com.courseshedule.data.model.DisplaySession> sessions) {
        android.widget.FrameLayout container = binding.gridContainer;
        container.removeAllViews();
        currentGrid = null;
        if (sessions == null || sessions.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.empty_timetable);
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(0, 200, 0, 0);
            empty.setTextColor(com.google.android.material.color.MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorOnSurfaceVariant,
                    android.graphics.Color.GRAY));
            container.addView(empty);
        } else {
            TimetableView grid = new TimetableView(this);
            currentGrid = grid;
            grid.setSessions(sessions);
            com.courseshedule.data.local.entity.SemesterEntity sem = viewModel.getActiveSemester().getValue();
            Integer week = viewModel.getSelectedWeek().getValue();
            if (sem != null && week != null) {
                long start = sem.startDate;
                String[] dates = new String[7];
                SimpleDateFormat sdf = new SimpleDateFormat("M/d", Locale.getDefault());
                for (int d = 0; d < 7; d++) {
                    dates[d] = sdf.format(new Date(start + (week - 1) * 604800000L + d * 86400000L));
                }
                grid.setDateLabels(dates);
            }
            grid.setOnSessionClickListener(courseId ->
                    startActivity(new Intent(this, com.courseshedule.ui.course.CourseDetailActivity.class)
                            .putExtra(com.courseshedule.ui.course.CourseDetailActivity.EXTRA_COURSE_ID,
                                    courseId)));
            grid.setOnSelectionListener((ids, inSelectionMode) -> {
                selectedCourseIds = ids;
                if (inSelectionMode && !ids.isEmpty()) {
                    binding.selectionBar.setVisibility(View.VISIBLE);
                    binding.tvSelectionCount.setText(
                            getString(R.string.selection_count, ids.size()));
                } else {
                    binding.selectionBar.setVisibility(View.GONE);
                }
            });
            if (viewModel.isCurrentWeekSelected()) {
                grid.setTodayHighlight(todayDayOfWeek(), viewModel.currentPeriodNow());
            }
            container.addView(grid);
        }
    }

    private int todayDayOfWeek() {
        int cal = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK);
        return (cal == java.util.Calendar.SUNDAY) ? 7 : cal - 1;
    }

    @Override
    public void onBackPressed() {
        if (currentGrid != null && currentGrid.isSelectionMode()) {
            exitSelectionMode();
        } else {
            super.onBackPressed();
        }
    }
}
