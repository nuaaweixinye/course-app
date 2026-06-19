package com.courseshedule.ui.task;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.courseshedule.R;
import com.courseshedule.data.local.entity.TaskEntity;
import com.courseshedule.databinding.ActivityTaskListBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Homework/exam list grouped by due-date bucket, with add/edit/delete. */
public class TaskListActivity extends AppCompatActivity {

    private ActivityTaskListBinding binding;
    private TaskListViewModel viewModel;
    private final SimpleDateFormat dateFmt =
            new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTaskListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this, new TaskListViewModel.Factory(getApplication()))
                .get(TaskListViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.fabAdd.setOnClickListener(v -> showTaskDialog(null));

        viewModel.getTasks().observe(this, this::renderTasks);
    }

    private void renderTasks(List<TaskEntity> tasks) {
        LinearLayout container = binding.taskContainer;
        container.removeAllViews();
        if (tasks == null || tasks.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.empty_tasks);
            empty.setPadding(0, 80, 0, 0);
            empty.setGravity(android.view.Gravity.CENTER);
            container.addView(empty);
            return;
        }

        long now = System.currentTimeMillis();
        long endOfToday = endOfToday();
        Map<Integer, List<TaskEntity>> groups = group(tasks, now, endOfToday);
        int[] order = {0 /*overdue*/, 1 /*today*/, 2 /*upcoming*/};
        int[] titles = {R.string.group_overdue, R.string.group_today, R.string.group_upcoming};
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int g = 0; g < order.length; g++) {
            List<TaskEntity> bucket = groups.get(order[g]);
            if (bucket == null || bucket.isEmpty()) continue;
            TextView header = new TextView(this);
            header.setText(titles[g]);
            header.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall);
            header.setPadding(0, 16, 0, 8);
            container.addView(header);
            for (TaskEntity t : bucket) container.addView(taskRow(inflater, t));
        }
    }

    private View taskRow(LayoutInflater inflater, TaskEntity t) {
        View row = inflater.inflate(R.layout.item_task, binding.taskContainer, false);
        CheckBox cb = row.findViewById(R.id.cbDone);
        TextView title = row.findViewById(R.id.tvTitle);
        TextView meta = row.findViewById(R.id.tvMeta);
        cb.setChecked(t.done);
        title.setText(t.title);
        if (t.done) title.setPaintFlags(title.getPaintFlags()
                | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        String type = t.type == TaskEntity.TYPE_EXAM
                ? getString(R.string.type_exam) : getString(R.string.type_homework);
        meta.setText(type + " · " + dateFmt.format(new Date(t.dueDate)));
        cb.setOnCheckedChangeListener((v, checked) -> viewModel.toggleDone(t));
        row.setOnClickListener(v -> showTaskDialog(t));
        row.findViewById(R.id.btnDelete).setOnClickListener(v -> viewModel.delete(t));
        return row;
    }

    private Map<Integer, List<TaskEntity>> group(List<TaskEntity> tasks, long now, long endOfToday) {
        Map<Integer, List<TaskEntity>> groups = new HashMap<>();
        for (TaskEntity t : tasks) {
            int key;
            if (!t.done && t.dueDate < now) key = 0;
            else if (t.dueDate <= endOfToday) key = 1;
            else key = 2;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }
        return groups;
    }

    private long endOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        return c.getTimeInMillis();
    }

    private void showTaskDialog(TaskEntity existing) {
        final View form = LayoutInflater.from(this).inflate(R.layout.dialog_task, null);
        final EditText etTitle = form.findViewById(R.id.etTitle);
        final Spinner spinType = form.findViewById(R.id.spinType);
        final TextView tvDue = form.findViewById(R.id.tvDue);
        final EditText etNote = form.findViewById(R.id.etNote);

        final long[] dueHolder = new long[]{System.currentTimeMillis() + 86_400_000L};
        if (existing != null) {
            etTitle.setText(existing.title);
            spinType.setSelection(existing.type);
            dueHolder[0] = existing.dueDate;
            etNote.setText(existing.note);
        }
        tvDue.setText(dateFmt.format(new Date(dueHolder[0])));
        tvDue.setOnClickListener(v -> pickDueDate(dueHolder, tvDue));

        new AlertDialog.Builder(this)
                .setTitle(existing == null ? R.string.tab_tasks : R.string.action_edit)
                .setView(form)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(this, R.string.err_title_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    TaskEntity t = existing == null ? new TaskEntity() : existing;
                    t.title = title;
                    t.type = spinType.getSelectedItemPosition();
                    t.dueDate = dueHolder[0];
                    t.note = etNote.getText().toString().trim();
                    viewModel.save(t);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void pickDueDate(long[] holder, TextView label) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(holder[0]);
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar p = Calendar.getInstance();
            p.set(year, month, day, 9, 0);
            holder[0] = p.getTimeInMillis();
            label.setText(dateFmt.format(new Date(holder[0])));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }
}
