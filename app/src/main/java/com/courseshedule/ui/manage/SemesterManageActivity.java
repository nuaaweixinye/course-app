package com.courseshedule.ui.manage;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.courseshedule.App;
import com.courseshedule.R;
import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.entity.SemesterEntity;
import com.courseshedule.data.model.PeriodUtils;
import com.courseshedule.data.repository.SemesterRepository;
import com.courseshedule.databinding.ActivitySemesterManageBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SemesterManageActivity extends AppCompatActivity {

    private ActivitySemesterManageBinding binding;
    private SemesterRepository repository;
    private static final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySemesterManageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppDatabase db = ((App) getApplication()).getDatabase();
        repository = new SemesterRepository(db);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.btnAddSemester.setOnClickListener(v -> promptCreate());

        repository.observeAll().observe(this, semesters -> {
            binding.semesterList.setLayoutManager(new LinearLayoutManager(this));
            binding.semesterList.setAdapter(new SemesterAdapter(semesters, this::onItemClick, this::openTimetableManage));
        });
    }

    private void onItemClick(SemesterEntity semester) {
        new AlertDialog.Builder(this)
                .setTitle(semester.name)
                .setItems(new String[]{
                        semester.isActive ? getString(R.string.action_edit) : getString(R.string.action_set_active),
                        getString(R.string.action_edit),
                        getString(R.string.action_delete)
                }, (d, which) -> {
                    if (which == 0) {
                        if (semester.isActive) promptEditDetails(semester);
                        else setActive(semester);
                    } else if (which == 1) {
                        promptEditDetails(semester);
                    } else {
                        promptDelete(semester);
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void openTimetableManage(SemesterEntity semester) {
        startActivity(new Intent(this, TimetableManageActivity.class)
                .putExtra(TimetableManageActivity.EXTRA_SEMESTER_ID, semester.id)
                .putExtra(TimetableManageActivity.EXTRA_SEMESTER_NAME, semester.name));
    }

    private void setActive(SemesterEntity semester) {
        repository.switchTo(semester.id);
        Toast.makeText(this, R.string.toast_semester_activated, Toast.LENGTH_SHORT).show();
    }

    private void promptCreate() {
        EditText input = new EditText(this);
        input.setHint(R.string.dialog_semester_name);
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_add_semester)
                .setView(input)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    repository.create(name,
                            PeriodUtils.mondayOfDay(System.currentTimeMillis()), 16,
                            id -> {
                                repository.switchTo(id);
                                runOnUiThread(() ->
                                    Toast.makeText(this, R.string.toast_semester_created, Toast.LENGTH_SHORT).show());
                            });
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void promptEditDetails(SemesterEntity semester) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_semester_edit, null);
        EditText etName = view.findViewById(R.id.etEditName);
        TextView tvStartDate = view.findViewById(R.id.tvEditStartDate);
        EditText etTotalWeeks = view.findViewById(R.id.etEditTotalWeeks);

        etName.setText(semester.name);
        etName.setSelection(semester.name.length());
        tvStartDate.setText(dateFmt.format(new Date(semester.startDate)));
        etTotalWeeks.setText(String.valueOf(semester.totalWeeks));

        tvStartDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(semester.startDate);
            DatePickerDialog dpd = new DatePickerDialog(this,
                    (view1, year, month, dayOfMonth) -> {
                        Calendar c = Calendar.getInstance();
                        c.set(year, month, dayOfMonth, 0, 0, 0);
                        c.set(Calendar.MILLISECOND, 0);
                        long monday = PeriodUtils.mondayOfDay(c.getTimeInMillis());
                        tvStartDate.setText(dateFmt.format(new Date(monday)));
                    },
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dpd.show();
        });

        new AlertDialog.Builder(this)
                .setTitle(R.string.action_edit)
                .setView(view)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) return;
                    int weeks;
                    try {
                        weeks = Integer.parseInt(etTotalWeeks.getText().toString().trim());
                        if (weeks < 1) weeks = 16;
                    } catch (NumberFormatException e) {
                        weeks = 16;
                    }
                    semester.name = name;
                    semester.totalWeeks = weeks;
                    try {
                        semester.startDate = dateFmt.parse(tvStartDate.getText().toString()).getTime();
                    } catch (Exception e) {
                        // keep original
                    }
                    repository.update(semester);
                    Toast.makeText(this, R.string.toast_semester_updated, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void promptDelete(SemesterEntity semester) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_delete)
                .setMessage(getString(R.string.dialog_delete_semester, semester.name))
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    repository.delete(semester);
                    Toast.makeText(this, R.string.toast_semester_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private static class SemesterAdapter extends RecyclerView.Adapter<SemesterAdapter.ViewHolder> {
        private List<SemesterEntity> list;
        private final OnItemClick listener;
        private final OnTimetableClick ttListener;

        interface OnItemClick { void onItemClick(SemesterEntity semester); }
        interface OnTimetableClick { void onTimetableClick(SemesterEntity semester); }

        SemesterAdapter(List<SemesterEntity> list, OnItemClick listener, OnTimetableClick ttListener) {
            this.list = list;
            this.listener = listener;
            this.ttListener = ttListener;
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_semester_row, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int i) {
            SemesterEntity s = list.get(i);
            h.tvName.setText(s.name);
            h.tvInfo.setText(s.isActive ? "\u2713 " + h.itemView.getContext().getString(R.string.label_active) : "");
            h.tvDate.setText(dateFmt.format(new Date(s.startDate)));
            h.itemView.setOnClickListener(v -> listener.onItemClick(s));
            h.btnTt.setOnClickListener(v -> ttListener.onTimetableClick(s));
        }

        @Override public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvInfo, tvDate;
            android.widget.Button btnTt;
            ViewHolder(@NonNull View v) {
                super(v);
                tvName = v.findViewById(R.id.tvSemesterName);
                tvInfo = v.findViewById(R.id.tvSemesterInfo);
                tvDate = v.findViewById(R.id.tvSemesterDate);
                btnTt = v.findViewById(R.id.btnManageTimetables);
            }
        }
    }
}
