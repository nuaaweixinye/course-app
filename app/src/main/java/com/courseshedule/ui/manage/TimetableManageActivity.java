package com.courseshedule.ui.manage;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.courseshedule.App;
import com.courseshedule.R;
import com.courseshedule.data.imports.CsvScheduleParser;
import com.courseshedule.data.imports.IcsScheduleParser;
import com.courseshedule.data.imports.ImportException;
import com.courseshedule.data.imports.NuaaEamsParser;
import com.courseshedule.data.imports.ParsedCourse;
import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.entity.CourseEntity;
import com.courseshedule.data.local.entity.CourseSessionEntity;
import com.courseshedule.data.local.entity.TimetableEntity;
import com.courseshedule.data.repository.CourseRepository;
import com.courseshedule.data.repository.SemesterRepository;
import com.courseshedule.data.repository.TimetableRepository;
import com.courseshedule.databinding.ActivityTimetableManageBinding;
import com.courseshedule.ui.common.ColorPalette;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class TimetableManageActivity extends AppCompatActivity {

    public static final String EXTRA_SEMESTER_ID = "semester_id";
    public static final String EXTRA_SEMESTER_NAME = "semester_name";

    private ActivityTimetableManageBinding binding;
    private TimetableRepository repository;
    private CourseRepository courseRepository;
    private long semesterId;
    private String periodTimesJson;

    private final ActivityResultLauncher<String[]> openFileLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onFileChosen);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTimetableManageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        semesterId = getIntent().getLongExtra(EXTRA_SEMESTER_ID, -1L);
        String semesterName = getIntent().getStringExtra(EXTRA_SEMESTER_NAME);
        if (semesterId < 0) { finish(); return; }

        AppDatabase db = ((App) getApplication()).getDatabase();
        repository = new TimetableRepository(db);
        courseRepository = new CourseRepository(db);
        periodTimesJson = new SemesterRepository(db).getCachedOrDefault().periodTimesJson;

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        if (semesterName != null) {
            binding.toolbar.setTitle(getString(R.string.header_timetable_manage) + " - " + semesterName);
        }
        binding.toolbar.inflateMenu(R.menu.timetable_manage_menu);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_import) {
                openFileLauncher.launch(new String[]{"*/*"});
                return true;
            }
            return false;
        });
        binding.btnAddTimetable.setOnClickListener(v -> promptCreate());

        repository.observeBySemester(semesterId).observe(this, timetables -> {
            binding.timetableList.setLayoutManager(new LinearLayoutManager(this));
            binding.timetableList.setAdapter(new TimetableAdapter(timetables, this::onItemClick));
        });
    }

    private void onFileChosen(Uri uri) {
        if (uri == null) return;
        String fileName = queryDisplayName(uri);
        String lower = fileName == null ? "" : fileName.toLowerCase();

        new Thread(() -> {
            try {
                List<ParsedCourse> result;
                if (lower.endsWith(".html") || lower.endsWith(".htm") || lower.endsWith(".xls")
                        || lower.contains("coursetable") || lower.contains("timetable")) {
                    String html = readFully(uri);
                    result = new NuaaEamsParser(html).fetch();
                } else {
                    Reader reader = new InputStreamReader(openInputStream(uri));
                    com.courseshedule.data.imports.TimetableImporter importer = lower.endsWith(".ics")
                            ? new IcsScheduleParser(reader, periodTimesJson)
                            : new CsvScheduleParser(reader);
                    result = importer.fetch();
                }
                final String importName = fileName != null ? fileName.replaceFirst("\\.[^.]+$", "") : getString(R.string.timetable_import_default);
                final List<ParsedCourse> finalResult = result;
                runOnUiThread(() -> showImportPreview(importName, finalResult));
            } catch (ImportException e) {
                runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.err_import_parse) + ": " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void showImportPreview(String name, List<ParsedCourse> courses) {
        if (courses.isEmpty()) {
            Toast.makeText(this, R.string.err_import_parse, Toast.LENGTH_LONG).show();
            return;
        }
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_import_preview, null);
        TextView tvInfo = view.findViewById(R.id.tvImportInfo);
        tvInfo.setText(getString(R.string.import_preview_count, courses.size()));

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.import_create_timetable, name))
                .setView(view)
                .setPositiveButton(R.string.action_confirm_import, (d, w) -> confirmImport(name, courses))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void confirmImport(String name, List<ParsedCourse> courses) {
        repository.create(name, semesterId, timetableId -> {
            int count = 0;
            for (ParsedCourse c : courses) {
                CourseEntity course = new CourseEntity();
                course.name = c.name;
                course.teacher = c.teacher == null ? "" : c.teacher;
                course.colorTag = ColorPalette.defaultTag(count);
                course.semesterId = semesterId;
                course.timetableId = timetableId;
                List<CourseSessionEntity> sessions = new ArrayList<>();
                for (com.courseshedule.data.imports.ParsedSession ps : c.sessions) {
                    CourseSessionEntity s = new CourseSessionEntity();
                    s.dayOfWeek = ps.dayOfWeek;
                    s.startPeriod = ps.startPeriod;
                    s.endPeriod = ps.endPeriod;
                    s.location = ps.location == null ? "" : ps.location;
                    s.weekPattern = ps.weekPattern == null ? "1-16" : ps.weekPattern;
                    sessions.add(s);
                }
                courseRepository.saveCourse(course, sessions, null);
                count++;
            }
            final int finalCount = count;
            runOnUiThread(() -> {
                Toast.makeText(this, getString(R.string.toast_imported, name, finalCount), Toast.LENGTH_SHORT).show();
            });
        });
    }

    private String queryDisplayName(Uri uri) {
        String result = uri.getLastPathSegment();
        try (android.database.Cursor c = getContentResolver().query(
                uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME},
                null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0 && c.getString(idx) != null) result = c.getString(idx);
            }
        } catch (Exception ignored) {}
        return result;
    }

    private String readFully(Uri uri) throws java.io.IOException {
        byte[] bytes = readAllBytes(uri);
        String charset = "UTF-8";
        int headLen = Math.min(bytes.length, 2000);
        String head = new String(bytes, 0, headLen, java.nio.charset.StandardCharsets.ISO_8859_1);
        java.util.regex.Matcher cm = java.util.regex.Pattern.compile(
                "charset=[\"']?([A-Za-z0-9-]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(head);
        if (cm.find()) charset = cm.group(1);
        try {
            return new String(bytes, charset);
        } catch (java.io.UnsupportedEncodingException e) {
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private java.io.InputStream openInputStream(Uri uri) throws java.io.IOException {
        if ("file".equals(uri.getScheme())) {
            return new java.io.FileInputStream(uri.getPath());
        }
        return getContentResolver().openInputStream(uri);
    }

    private byte[] readAllBytes(Uri uri) throws java.io.IOException {
        try (java.io.InputStream in = openInputStream(uri)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    private void onItemClick(TimetableEntity tt) {
        String[] items;
        int activeActionIndex, viewActionIndex, renameActionIndex, deleteActionIndex;
        if (tt.isActive) {
            items = new String[]{
                    getString(R.string.action_view_timetable),
                    getString(R.string.dialog_rename),
                    getString(R.string.action_delete)
            };
            viewActionIndex = 0;
            renameActionIndex = 1;
            deleteActionIndex = 2;
            new AlertDialog.Builder(this)
                    .setTitle(tt.name)
                    .setItems(items, (d, which) -> {
                        if (which == viewActionIndex) viewTimetable(tt);
                        else if (which == renameActionIndex) promptRename(tt);
                        else promptDelete(tt);
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        } else {
            items = new String[]{
                    getString(R.string.action_set_active_timetable),
                    getString(R.string.action_view_timetable),
                    getString(R.string.dialog_rename),
                    getString(R.string.action_delete)
            };
            activeActionIndex = 0;
            viewActionIndex = 1;
            renameActionIndex = 2;
            deleteActionIndex = 3;
            new AlertDialog.Builder(this)
                    .setTitle(tt.name)
                    .setItems(items, (d, which) -> {
                        if (which == activeActionIndex) {
                            repository.switchTo(tt.id, tt.semesterId);
                            Toast.makeText(this, R.string.toast_timetable_activated, Toast.LENGTH_SHORT).show();
                        } else if (which == viewActionIndex) {
                            viewTimetable(tt);
                        } else if (which == renameActionIndex) {
                            promptRename(tt);
                        } else {
                            promptDelete(tt);
                        }
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        }
    }

    private void viewTimetable(TimetableEntity tt) {
        Intent intent = new Intent(this, TimetableCourseListActivity.class);
        intent.putExtra(TimetableCourseListActivity.EXTRA_TIMETABLE_ID, tt.id);
        intent.putExtra(TimetableCourseListActivity.EXTRA_TIMETABLE_NAME, tt.name);
        startActivity(intent);
    }

    private void promptCreate() {
        EditText input = new EditText(this);
        input.setHint(R.string.dialog_timetable_name);
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_add_timetable)
                .setView(input)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    repository.create(name, semesterId, id ->
                            Toast.makeText(this, R.string.toast_timetable_created, Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void promptRename(TimetableEntity tt) {
        EditText input = new EditText(this);
        input.setText(tt.name);
        input.setSelection(tt.name.length());
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_rename)
                .setView(input)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    tt.name = name;
                    repository.update(tt);
                    Toast.makeText(this, R.string.toast_timetable_updated, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void promptDelete(TimetableEntity tt) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_delete)
                .setMessage(getString(R.string.dialog_delete_timetable, tt.name))
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    repository.delete(tt);
                    Toast.makeText(this, R.string.toast_timetable_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private static class TimetableAdapter extends RecyclerView.Adapter<TimetableAdapter.ViewHolder> {
        private final List<TimetableEntity> list;
        private final OnItemClick listener;

        interface OnItemClick { void onItemClick(TimetableEntity tt); }

        TimetableAdapter(List<TimetableEntity> list, OnItemClick listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_timetable_row, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int i) {
            TimetableEntity tt = list.get(i);
            h.tvName.setText((tt.isActive ? "\u2713 " : "") + tt.name);
            h.itemView.setOnClickListener(v -> listener.onItemClick(tt));
        }

        @Override public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            ViewHolder(@NonNull View v) {
                super(v);
                tvName = v.findViewById(R.id.tvTimetableName);
            }
        }
    }
}
