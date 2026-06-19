package com.courseshedule.ui.settings;

import android.app.TimePickerDialog;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.courseshedule.R;
import com.courseshedule.data.model.PeriodTime;
import com.courseshedule.data.model.PeriodUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog that lists every class period with editable start/end times. Tapping
 * either opens a TimePickerDialog. On confirm, the caller receives the updated
 * JSON string via the callback.
 */
public class PeriodTimesEditorDialog {

    public interface OnConfirmed {
        void onConfirmed(String periodTimesJson);
    }

    private final List<PeriodTime> periods;
    private final List<int[]> working; // [startMin, endMin]

    public PeriodTimesEditorDialog(String periodTimesJson) {
        periods = PeriodUtils.parse(periodTimesJson);
        if (periods.isEmpty()) {
            periods.addAll(PeriodUtils.parse(PeriodUtils.DEFAULT_PERIOD_TIMES_JSON));
        }
        working = new ArrayList<>();
        for (PeriodTime p : periods) {
            working.add(new int[]{p.startMinutes, p.endMinutes});
        }
    }

    public void show(Context context, OnConfirmed callback) {
        LinearLayout list = buildList(context);

        new AlertDialog.Builder(context)
                .setTitle(R.string.header_periods)
                .setView(list)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    List<PeriodTime> updated = new ArrayList<>();
                    for (int i = 0; i < working.size(); i++) {
                        int[] times = working.get(i);
                        updated.add(new PeriodTime(i + 1, times[0], times[1]));
                    }
                    callback.onConfirmed(PeriodUtils.toJson(updated));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private LinearLayout buildList(Context context) {
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * context.getResources().getDisplayMetrics().density);
        list.setPadding(pad, pad, pad, pad);

        for (int i = 0; i < working.size(); i++) {
            final int index = i;
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView label = new TextView(context);
            label.setText(context.getString(R.string.label_period_n, i + 1));
            label.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView times = new TextView(context);
            times.setGravity(Gravity.CENTER);
            times.setPadding(pad, 0, pad, 0);
            updateTimesLabel(times, index);

            row.setOnClickListener(v -> pickStart(context, index, () -> updateTimesLabel(times, index)));
            times.setOnClickListener(v -> pickEnd(context, index, () -> updateTimesLabel(times, index)));

            row.addView(label);
            row.addView(times);
            list.addView(row);
        }
        return list;
    }

    private void updateTimesLabel(TextView tv, int index) {
        int[] t = working.get(index);
        tv.setText(tv.getContext().getString(R.string.period_range,
                PeriodUtils.format(t[0]), PeriodUtils.format(t[1])));
    }

    private void pickStart(Context context, int index, Runnable after) {
        int[] t = working.get(index);
        new TimePickerDialog(context, (view, h, m) -> {
            t[0] = h * 60 + m;
            after.run();
        }, t[0] / 60, t[0] % 60, true).show();
    }

    private void pickEnd(Context context, int index, Runnable after) {
        int[] t = working.get(index);
        new TimePickerDialog(context, (view, h, m) -> {
            t[1] = h * 60 + m;
            after.run();
        }, t[1] / 60, t[1] % 60, true).show();
    }
}
