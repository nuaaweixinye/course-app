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
import com.courseshedule.data.local.entity.SemesterEntity;
import com.courseshedule.data.local.entity.TimetableEntity;
import com.courseshedule.data.model.TimetableWithSemester;
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
    private SemesterRepository semesterRepository;
    private String periodTimesJson;
    private List<SemesterEntity> spinnerSemesters;
    private Long pendingImportTimetableId;

    private final ActivityResultLauncher<String[]> openFileLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onFileChosen);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTimetableManageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppDatabase db = ((App) getApplication()).getDatabase();
        repository = new TimetableRepository(db);
        courseRepository = new CourseRepository(db);
        semesterRepository = new SemesterRepository(db);
        periodTimesJson = semesterRepository.getCachedOrDefault().periodTimesJson;

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.inflateMenu(R.menu.timetable_manage_menu);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_import) {
                onStartImport();
                return true;
            }
            return false;
        });
        binding.btnAddTimetable.setOnClickListener(v -> promptCreate());

        repository.observeAllWithSemester().observe(this, timetables -> {
            binding.timetableList.setLayoutManager(new LinearLayoutManager(this));
            binding.timetableList.setAdapter(new TimetableAdapter(timetables, this::onItemClick));
        });

        semesterRepository.observeAll().observe(this, semesters ->
                spinnerSemesters = semesters);
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
        if (pendingImportTimetableId == null) {
            Toast.makeText(this, R.string.err_import_parse, Toast.LENGTH_LONG).show();
            return;
        }
        new Thread(() -> {
            AppDatabase db = ((App) getApplication()).getDatabase();
            TimetableEntity tt = db.timetableDao().getById(pendingImportTimetableId);
            if (tt == null) {
                runOnUiThread(() -> Toast.makeText(this, R.string.err_import_parse, Toast.LENGTH_LONG).show());
                return;
            }
            int count = saveCourses(courses, tt.id, tt.semesterId);
            final int finalCount = count;
            runOnUiThread(() ->
                    Toast.makeText(this, getString(R.string.toast_imported, tt.name, finalCount), Toast.LENGTH_SHORT).show());
        }).start();
    }

    private void onStartImport() {
        new Thread(() -> {
            List<TimetableWithSemester> all = repository.listAllWithSemester();
            runOnUiThread(() -> showImportTimetablePicker(all));
        }).start();
    }

    private void showImportTimetablePicker(List<TimetableWithSemester> timetables) {
        String[] items = new String[timetables.size() + 1];
        items[0] = getString(R.string.import_create_new);
        for (int i = 0; i < timetables.size(); i++) {
            TimetableWithSemester tt = timetables.get(i);
            items[i + 1] = tt.name + " (" + tt.semesterName + ")";
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_select_target_timetable)
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        promptCreateForImport();
                    } else {
                        pendingImportTimetableId = timetables.get(which - 1).id;
                        openFileLauncher.launch(new String[]{"*/*"});
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void promptCreateForImport() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_timetable_create, null);
        android.widget.Spinner spinSemester = view.findViewById(R.id.spinSemester);
        EditText etName = view.findViewById(R.id.etTimetableName);

        if (spinnerSemesters == null || spinnerSemesters.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_semester, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[spinnerSemesters.size()];
        int activePos = 0;
        for (int i = 0; i < spinnerSemesters.size(); i++) {
            names[i] = spinnerSemesters.get(i).name;
            if (spinnerSemesters.get(i).isActive) activePos = i;
        }
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinSemester.setAdapter(adapter);
        spinSemester.setSelection(activePos);

        new AlertDialog.Builder(this)
                .setTitle(R.string.action_add_timetable)
                .setView(view)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) return;
                    long semId = spinnerSemesters.get(spinSemester.getSelectedItemPosition()).id;
                    repository.create(name, semId, id -> {
                        pendingImportTimetableId = id;
                        runOnUiThread(() -> openFileLauncher.launch(new String[]{"*/*"}));
                    });
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private int saveCourses(List<ParsedCourse> courses, long timetableId, long semesterId) {
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
        return count;
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

    private TimetableEntity toEntity(TimetableWithSemester tt) {
        TimetableEntity e = new TimetableEntity();
        e.id = tt.id;
        e.name = tt.name;
        e.semesterId = tt.semesterId;
        e.isActive = tt.isActive;
        return e;
    }

    private void onItemClick(TimetableWithSemester tt) {
        String[] items;
        int activeActionIndex, addCourseIndex, viewActionIndex, renameActionIndex, deleteActionIndex;
        if (tt.isActive) {
            items = new String[]{
                    getString(R.string.dialog_add_course),
                    getString(R.string.action_view_timetable),
                    getString(R.string.dialog_rename),
                    getString(R.string.action_delete)
            };
            addCourseIndex = 0;
            viewActionIndex = 1;
            renameActionIndex = 2;
            deleteActionIndex = 3;
            new AlertDialog.Builder(this)
                    .setTitle(tt.name)
                    .setItems(items, (d, which) -> {
                        if (which == addCourseIndex) addCourseToTimetable(tt);
                        else if (which == viewActionIndex) viewTimetable(tt);
                        else if (which == renameActionIndex) promptRename(toEntity(tt));
                        else promptDelete(toEntity(tt));
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        } else {
            items = new String[]{
                    getString(R.string.action_set_active_timetable),
                    getString(R.string.dialog_add_course),
                    getString(R.string.action_view_timetable),
                    getString(R.string.dialog_rename),
                    getString(R.string.action_delete)
            };
            activeActionIndex = 0;
            addCourseIndex = 1;
            viewActionIndex = 2;
            renameActionIndex = 3;
            deleteActionIndex = 4;
            new AlertDialog.Builder(this)
                    .setTitle(tt.name)
                    .setItems(items, (d, which) -> {
                        if (which == activeActionIndex) {
                            repository.switchTo(tt.id, tt.semesterId);
                            Toast.makeText(this, R.string.toast_timetable_activated, Toast.LENGTH_SHORT).show();
                        } else if (which == addCourseIndex) addCourseToTimetable(tt);
                        else if (which == viewActionIndex) viewTimetable(tt);
                        else if (which == renameActionIndex) promptRename(toEntity(tt));
                        else promptDelete(toEntity(tt));
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        }
    }

    private void addCourseToTimetable(TimetableWithSemester tt) {
        Intent intent = new Intent(this, com.courseshedule.ui.course.CourseEditActivity.class);
        intent.putExtra(com.courseshedule.ui.course.CourseEditActivity.EXTRA_TIMETABLE_ID, tt.id);
        startActivity(intent);
    }

    private void viewTimetable(TimetableWithSemester tt) {
        Intent intent = new Intent(this, TimetableCourseListActivity.class);
        intent.putExtra(TimetableCourseListActivity.EXTRA_TIMETABLE_ID, tt.id);
        intent.putExtra(TimetableCourseListActivity.EXTRA_TIMETABLE_NAME, tt.name);
        startActivity(intent);
    }

    private void promptCreate() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_timetable_create, null);
        android.widget.Spinner spinSemester = view.findViewById(R.id.spinSemester);
        EditText etName = view.findViewById(R.id.etTimetableName);

        if (spinnerSemesters == null || spinnerSemesters.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_semester, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[spinnerSemesters.size()];
        int activePos = 0;
        for (int i = 0; i < spinnerSemesters.size(); i++) {
            names[i] = spinnerSemesters.get(i).name;
            if (spinnerSemesters.get(i).isActive) activePos = i;
        }
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinSemester.setAdapter(adapter);
        spinSemester.setSelection(activePos);

        new AlertDialog.Builder(this)
                .setTitle(R.string.action_add_timetable)
                .setView(view)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) return;
                    long semId = spinnerSemesters.get(spinSemester.getSelectedItemPosition()).id;
                    repository.create(name, semId, id ->
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
        private final List<TimetableWithSemester> list;
        private final OnItemClick listener;

        interface OnItemClick { void onItemClick(TimetableWithSemester tt); }

        TimetableAdapter(List<TimetableWithSemester> list, OnItemClick listener) {
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
            TimetableWithSemester tt = list.get(i);
            h.tvName.setText((tt.isActive ? "\u2713 " : "") + tt.name);
            h.tvSemester.setText(tt.semesterName);
            h.itemView.setOnClickListener(v -> listener.onItemClick(tt));
        }

        @Override public int getItemCount() { return list == null ? 0 : list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvSemester;
            ViewHolder(@NonNull View v) {
                super(v);
                tvName = v.findViewById(R.id.tvTimetableName);
                tvSemester = v.findViewById(R.id.tvSemesterName);
            }
        }
    }
}
