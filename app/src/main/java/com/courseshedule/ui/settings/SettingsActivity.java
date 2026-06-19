package com.courseshedule.ui.settings;

import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.net.Uri;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.courseshedule.App;
import com.courseshedule.R;
import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.entity.SemesterEntity;
import com.courseshedule.data.repository.SemesterRepository;
import com.courseshedule.databinding.ActivitySettingsBinding;
import com.courseshedule.ui.common.ThemePrefs;
import com.courseshedule.ui.common.ExportUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SemesterRepository configRepository;
    private SemesterEntity config;
    private ThemePrefs themePrefs;
    private final SimpleDateFormat dateFmt =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final ActivityResultLauncher<String> createDocLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"),
                    this::onExportTarget);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppDatabase db = ((App) getApplication()).getDatabase();
        configRepository = new SemesterRepository(db);
        themePrefs = new ThemePrefs(this);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        loadConfig();

        binding.btnSemesterStart.setOnClickListener(v -> pickSemesterStart());
        binding.btnManageSemesters.setOnClickListener(v ->
                startActivity(new Intent(this, com.courseshedule.ui.manage.SemesterManageActivity.class)));
        binding.btnManageTimetables.setOnClickListener(v -> {
            configRepository.loadAsync(() -> {
                com.courseshedule.data.local.entity.SemesterEntity active = configRepository.getCachedOrDefault();
                runOnUiThread(() -> startActivity(new Intent(this, com.courseshedule.ui.manage.TimetableManageActivity.class)
                        .putExtra(com.courseshedule.ui.manage.TimetableManageActivity.EXTRA_SEMESTER_ID, active.id)
                        .putExtra(com.courseshedule.ui.manage.TimetableManageActivity.EXTRA_SEMESTER_NAME, active.name)));
            });
        });
        binding.btnEditPeriods.setOnClickListener(v -> editPeriods());
        binding.btnExport.setOnClickListener(v -> createDocLauncher.launch("course_export.csv"));
        binding.btnImportHelp.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_import_help_title)
                        .setMessage(R.string.dialog_import_help_message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show());

        setupThemeSpinner();
    }

    private void loadConfig() {
        config = configRepository.getCachedOrDefault();
        binding.btnSemesterStart.setText(
                getString(R.string.label_semester_start) + ": " + dateFmt.format(new Date(config.startDate)));
        binding.etTotalWeeks.setText(String.valueOf(config.totalWeeks));
    }

    private void saveConfig() {
        int weeks = parseTotalWeeks();
        config.totalWeeks = weeks;
        configRepository.update(config);
    }

    private int parseTotalWeeks() {
        try {
            return Integer.parseInt(binding.etTotalWeeks.getText().toString().trim());
        } catch (NumberFormatException e) {
            return SemesterEntity.DEFAULT_TOTAL_WEEKS;
        }
    }

    private void pickSemesterStart() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(config.startDate);
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, day);
            config.startDate = com.courseshedule.data.model.PeriodUtils.mondayOfDay(
                    picked.getTimeInMillis());
            saveConfig();
            binding.btnSemesterStart.setText(
                    getString(R.string.label_semester_start) + ": " + dateFmt.format(new Date(config.startDate)));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void editPeriods() {
        new PeriodTimesEditorDialog(config.periodTimesJson)
                .show(this, json -> {
                    config.periodTimesJson = json;
                    saveConfig();
                    Toast.makeText(this, R.string.toast_course_saved, Toast.LENGTH_SHORT).show();
                });
    }

    private void setupThemeSpinner() {
        android.widget.Spinner spin = binding.spinTheme;
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{
                        getString(R.string.theme_follow_system),
                        getString(R.string.theme_light),
                        getString(R.string.theme_dark)
                });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(adapter);
        spin.setSelection(themePrefs.getMode());
        spin.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View v, int pos, long id) {
                if (pos != themePrefs.getMode()) themePrefs.setMode(pos);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void onExportTarget(Uri uri) {
        if (uri == null) return;
        saveConfig();
        final AppDatabase db = ((App) getApplication()).getDatabase();
        new Thread(() -> {
            final boolean ok = ExportUtil.exportCsv(this, db, uri);
            runOnUiThread(() -> {
                String msg = ok ? getString(R.string.toast_exported, uri.getLastPathSegment())
                        : getString(R.string.toast_export_failed);
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (config != null) saveConfig();
    }
}
