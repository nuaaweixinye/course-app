package com.courseshedule.ui.course;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import com.courseshedule.App;
import com.courseshedule.R;
import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.entity.SemesterConfigEntity;
import com.courseshedule.data.local.entity.SessionExceptionEntity;
import com.courseshedule.data.repository.CourseRepository;
import com.courseshedule.data.repository.SemesterRepository;

import java.util.List;

/**
 * Edits week-exceptions (停课/调换) for a single session. All DB reads/writes
 * run off the main thread; the dialog UI is built on the main thread with the
 * fetched data.
 */
public class WeekExceptionsDialog {

    public static void show(Context context, long sessionId, Runnable onChanged) {
        AppDatabase db = ((App) context.getApplicationContext()).getDatabase();
        CourseRepository repo = new CourseRepository(db);
        int totalWeeks = new SemesterRepository(db).getCachedOrDefault().totalWeeks;

        new Thread(() -> {
            List<SessionExceptionEntity> exceptions = repo.listExceptions(sessionId);
            new Handler(Looper.getMainLooper()).post(() ->
                    showDialog(context, repo, sessionId, totalWeeks, exceptions, onChanged));
        }).start();
    }

    private static void showDialog(Context context, CourseRepository repo, long sessionId,
                                   int totalWeeks, List<SessionExceptionEntity> exceptions,
                                   Runnable onChanged) {
        LinearLayout list = buildList(context, repo, sessionId, totalWeeks, exceptions, onChanged);
        new AlertDialog.Builder(context)
                .setTitle(R.string.header_exceptions)
                .setView(list)
                .setPositiveButton(R.string.action_add_exception,
                        (d, w) -> showAddException(context, repo, sessionId, totalWeeks, onChanged))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static LinearLayout buildList(Context context, CourseRepository repo,
                                          long sessionId, int totalWeeks,
                                          List<SessionExceptionEntity> exceptions, Runnable onChanged) {
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * context.getResources().getDisplayMetrics().density);
        list.setPadding(pad, pad, pad, pad);

        String[] dayNames = context.getResources().getStringArray(R.array.weekday_short);
        if (exceptions.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText(R.string.empty_sessions);
            list.addView(empty);
            return list;
        }
        for (SessionExceptionEntity ex : exceptions) {
            TextView row = new TextView(context);
            row.setPadding(0, 12, 0, 12);
            String text = ex.type == SessionExceptionEntity.TYPE_CANCEL
                    ? context.getString(R.string.exception_cancel_week, ex.weekNo)
                    : context.getString(R.string.exception_move_week, ex.weekNo,
                            ex.moveToDayOfWeek != null && ex.moveToDayOfWeek >= 1 && ex.moveToDayOfWeek <= 7
                                    ? dayNames[ex.moveToDayOfWeek - 1] : "?");
            row.setText(text);
            final long exId = ex.id;
            row.setOnClickListener(v -> new AlertDialog.Builder(context)
                    .setTitle(R.string.action_delete)
                    .setMessage(text)
                    .setPositiveButton(R.string.action_delete,
                            (d, w) -> {
                                repo.deleteException(exId);
                                if (onChanged != null) onChanged.run();
                                show(context, sessionId, onChanged);
                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show());
            list.addView(row);
        }
        return list;
    }

    private static void showAddException(Context context, CourseRepository repo, long sessionId,
                                         int totalWeeks, Runnable onAdded) {
        View form = LayoutInflater.from(context).inflate(R.layout.dialog_exception, null);
        Spinner spinType = form.findViewById(R.id.spinType);
        NumberPicker npWeek = form.findViewById(R.id.npWeek);
        NumberPicker npDay = form.findViewById(R.id.npMoveDay);
        npWeek.setMinValue(1);
        npWeek.setMaxValue(totalWeeks);
        npDay.setMinValue(1);
        npDay.setMaxValue(7);

        form.findViewById(R.id.labelMoveDay).setVisibility(View.GONE);
        npDay.setVisibility(View.GONE);
        spinType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View v, int pos, long id) {
                boolean move = pos == 1;
                form.findViewById(R.id.labelMoveDay).setVisibility(move ? View.VISIBLE : View.GONE);
                npDay.setVisibility(move ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        new AlertDialog.Builder(context)
                .setTitle(R.string.action_add_exception)
                .setView(form)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    SessionExceptionEntity ex = new SessionExceptionEntity();
                    ex.sessionId = sessionId;
                    ex.weekNo = npWeek.getValue();
                    ex.type = spinType.getSelectedItemPosition() == 1
                            ? SessionExceptionEntity.TYPE_MOVED
                            : SessionExceptionEntity.TYPE_CANCEL;
                    if (ex.type == SessionExceptionEntity.TYPE_MOVED) {
                        ex.moveToDayOfWeek = npDay.getValue();
                    }
                    repo.saveException(ex);
                    if (onAdded != null) onAdded.run();
                    show(context, sessionId, onAdded);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
