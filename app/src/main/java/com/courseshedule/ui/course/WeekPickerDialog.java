package com.courseshedule.ui.course;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import com.courseshedule.R;
import com.courseshedule.data.model.WeekUtils;

import java.util.Set;
import java.util.TreeSet;

public class WeekPickerDialog {

    public interface OnWeeksSelectedListener {
        void onWeeksSelected(String pattern);
    }

    public static void show(Context context, int totalWeeks, String currentPattern,
                           OnWeeksSelectedListener listener) {
        show(context, totalWeeks, currentPattern, listener, null);
    }

    public static void show(Context context, int totalWeeks, String currentPattern,
                           OnWeeksSelectedListener listener, Runnable onCancel) {
        Set<Integer> selected = new TreeSet<>(WeekUtils.parse(currentPattern));

        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(4);
        int pad = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 4, context.getResources().getDisplayMetrics());
        grid.setPadding(pad * 4, pad * 2, pad * 4, pad * 2);

        final boolean[] checked = new boolean[totalWeeks];
        for (int i = 0; i < totalWeeks; i++) {
            checked[i] = selected.contains(i + 1);
        }

        for (int i = 0; i < totalWeeks; i++) {
            final int weekNo = i + 1;
            final int index = i;
            TextView cell = new TextView(context);
            cell.setText(String.valueOf(weekNo));
            cell.setGravity(Gravity.CENTER);
            cell.setTypeface(cell.getTypeface(), Typeface.BOLD);
            cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            int cellPad = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 12, context.getResources().getDisplayMetrics());
            cell.setPadding(cellPad, cellPad / 2, cellPad, cellPad / 2);

            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = 0;
            glp.height = android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;
            glp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
            glp.setMargins(pad, pad, pad, pad);
            cell.setLayoutParams(glp);

            applyCellState(cell, checked[index]);

            cell.setOnClickListener(v -> {
                checked[index] = !checked[index];
                applyCellState(cell, checked[index]);
            });

            grid.addView(cell);
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_select_weeks)
                .setView(grid)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    java.util.List<Integer> weeks = new java.util.ArrayList<>();
                    for (int i = 0; i < totalWeeks; i++) {
                        if (checked[i]) weeks.add(i + 1);
                    }
                    if (weeks.isEmpty()) {
                        listener.onWeeksSelected(WeekUtils.allWeeks(totalWeeks));
                    } else {
                        listener.onWeeksSelected(WeekUtils.compress(weeks));
                    }
                })
                .setNegativeButton(android.R.string.cancel, (d, w) -> {
                    if (onCancel != null) onCancel.run();
                })
                .setOnCancelListener(d -> {
                    if (onCancel != null) onCancel.run();
                })
                .show();
    }

    private static void applyCellState(TextView cell, boolean isSelected) {
        if (isSelected) {
            cell.setBackgroundResource(R.drawable.bg_week_cell_selected);
            cell.setTextColor(androidx.core.content.ContextCompat.getColor(
                    cell.getContext(), android.R.color.white));
        } else {
            cell.setBackgroundResource(R.drawable.bg_week_cell);
            int color = com.google.android.material.color.MaterialColors.getColor(
                    cell.getContext(), com.google.android.material.R.attr.colorOnSurface,
                    android.graphics.Color.BLACK);
            cell.setTextColor(color);
        }
    }
}
