package com.courseshedule.ui.imports;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.courseshedule.App;
import com.courseshedule.R;
import com.courseshedule.data.imports.CsvScheduleParser;
import com.courseshedule.data.imports.IcsScheduleParser;
import com.courseshedule.data.imports.ImportException;
import com.courseshedule.data.imports.NuaaEamsParser;
import com.courseshedule.data.imports.ParsedCourse;
import com.courseshedule.data.imports.TimetableImporter;
import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.entity.CourseEntity;
import com.courseshedule.data.local.entity.CourseSessionEntity;
import com.courseshedule.data.local.entity.TimetableEntity;
import com.courseshedule.data.repository.CourseRepository;
import com.courseshedule.data.repository.SemesterRepository;
import com.courseshedule.databinding.ActivityImportBinding;
import com.courseshedule.ui.common.ColorPalette;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/** File import flow: pick → parse → preview → confirm. */
public class ImportActivity extends AppCompatActivity {

    private ActivityImportBinding binding;
    private AppDatabase db;
    private CourseRepository repository;
    private final List<ParsedCourse> parsed = new ArrayList<>();
    private final List<CheckBox> checkBoxes = new ArrayList<>();
    private String periodTimesJson;

    private final ActivityResultLauncher<String[]> openFileLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onFileChosen);

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIncomingIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = ((App) getApplication()).getDatabase();
        repository = new CourseRepository(db);
        periodTimesJson = new SemesterRepository(db).getCachedOrDefault().periodTimesJson;

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_confirm_import) {
                onConfirm();
                return true;
            }
            return false;
        });
        handleIncomingIntent(getIntent());

        binding.btnChooseFile.setOnClickListener(v ->
                openFileLauncher.launch(new String[]{"*/*"}));
    }

    private void onFileChosen(Uri uri) {
        if (uri == null) return;
        String fileName = queryDisplayName(uri);
        String lower = fileName == null ? "" : fileName.toLowerCase();
        binding.previewContainer.removeAllViews();
        checkBoxes.clear();
        parsed.clear();

        new Thread(() -> {
            try {
                List<ParsedCourse> result;
                if (lower.endsWith(".html") || lower.endsWith(".htm") || lower.endsWith(".xls")
                        || lower.contains("courseTable") || lower.contains("timetable")) {
                    // 金智/beangle exported timetable (rendered HTML or .xls export).
                    String html = readFully(uri);
                    result = new NuaaEamsParser(html).fetch();
                } else {
                    Reader reader = new InputStreamReader(openInputStream(uri));
                    TimetableImporter importer = lower.endsWith(".ics")
                            ? new IcsScheduleParser(reader, periodTimesJson)
                            : new CsvScheduleParser(reader);
                    result = importer.fetch();
                }
                new Handler(Looper.getMainLooper()).post(() -> showPreview(result));
            } catch (ImportException e) {
                showError(e.getMessage());
            } catch (Exception e) {
                showError(getString(R.string.err_import_parse) + ": " + e.getMessage());
            }
        }).start();
    }

    /** Query the real filename from a SAF Uri (getLastPathSegment is unreliable). */
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

    /** Read a Uri fully into a string, detecting the charset from the &lt;meta charset=...&gt; tag.
     *  NUAA exports are GBK-encoded; reading as UTF-8 would garble Chinese course names. */
    private String readFully(Uri uri) throws java.io.IOException {
        byte[] bytes = readAllBytes(uri);
        String charset = "UTF-8";
        // The meta tag is ASCII even in a GBK file, so scan it as ISO-8859-1.
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

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        Uri incomingUri = null;
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            incomingUri = intent.getData();
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
            incomingUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }
        if (incomingUri != null) onFileChosen(incomingUri);
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

    private void showPreview(List<ParsedCourse> courses) {
        parsed.clear();
        parsed.addAll(courses);
        LinearLayout container = binding.previewContainer;
        container.removeAllViews();
        checkBoxes.clear();
        if (courses.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.err_import_parse);
            container.addView(empty);
            return;
        }
        binding.tvHint.setText(getString(R.string.import_preview_count, courses.size()));
        for (ParsedCourse c : courses) {
            CheckBox cb = new CheckBox(this);
            cb.setText(getString(R.string.session_secondary, c.name,
                    c.sessions.size() + " 时段"));
            cb.setChecked(true);
            cb.setPadding(0, 8, 0, 8);
            container.addView(cb);
            checkBoxes.add(cb);
        }
    }

    private void onConfirm() {
        if (parsed.isEmpty()) {
            Toast.makeText(this, R.string.import_hint, Toast.LENGTH_SHORT).show();
            return;
        }
        TimetableEntity active = db.timetableDao().getActiveGlobal();
        if (active == null) {
            Toast.makeText(this, R.string.hint_create_timetable_first, Toast.LENGTH_SHORT).show();
            return;
        }
        int count = 0;
        for (int i = 0; i < checkBoxes.size(); i++) {
            if (!checkBoxes.get(i).isChecked()) continue;
            ParsedCourse c = parsed.get(i);
            CourseEntity course = new CourseEntity();
            course.name = c.name;
            course.teacher = c.teacher == null ? "" : c.teacher;
            course.colorTag = ColorPalette.defaultTag(count);
            course.timetableId = active.id;
            course.semesterId = active.semesterId;
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
            repository.saveCourse(course, sessions, null);
            count++;
        }
        com.courseshedule.widget.TodayWidgetProvider.refresh(this);
        Toast.makeText(this, getString(R.string.toast_course_saved) + " (" + count + ")",
                Toast.LENGTH_SHORT).show();
        setResult(Activity.RESULT_OK);
        finish();
    }

    private void showError(String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }
}
