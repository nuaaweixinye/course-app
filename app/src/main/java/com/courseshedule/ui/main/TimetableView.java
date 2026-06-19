package com.courseshedule.ui.main;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.courseshedule.R;
import com.courseshedule.data.model.DisplaySession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TimetableView extends ViewGroup {

    private static final int DAY_COUNT = 7;
    private static final int PERIOD_COUNT = 12;

    private final float density;
    private final int periodColumnWidth;
    private final int rowHeight;
    private final int headerHeight;
    private final int cardGap;

    private final Paint linePaint;
    private final Paint dimPaint;
    private final @ColorInt int outlineColor;
    private final @ColorInt int textColor;
    private final @ColorInt int todayHighlight;

    private final List<DisplaySession> sessions = new ArrayList<>();
    private final List<CourseCardView> cards = new ArrayList<>();
    private int todayDayOfWeek = -1;
    private int currentPeriod = -1;
    private OnSessionClickListener clickListener;
    private OnSelectionListener selectionListener;

    private boolean selectionMode = false;
    private final Set<Integer> selectedIndices = new HashSet<>();

    public interface OnSessionClickListener {
        void onSessionClicked(long courseId);
    }

    public interface OnSelectionListener {
        void onSelectionChanged(Set<Long> selectedCourseIds, boolean inSelectionMode);
    }

    public void setOnSessionClickListener(OnSessionClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnSelectionListener(OnSelectionListener listener) {
        this.selectionListener = listener;
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public Set<Long> getSelectedCourseIds() {
        Set<Long> ids = new HashSet<>();
        for (int idx : selectedIndices) {
            if (idx < sessions.size()) ids.add(sessions.get(idx).courseId);
        }
        return ids;
    }

    public void exitSelectionMode() {
        if (!selectionMode) return;
        selectionMode = false;
        selectedIndices.clear();
        for (CourseCardView card : cards) card.setSelectedState(false);
        if (selectionListener != null) selectionListener.onSelectionChanged(new HashSet<>(), false);
    }

    public TimetableView(Context context) {
        this(context, null);
    }

    public TimetableView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        periodColumnWidth = (int) getResources().getDimension(R.dimen.grid_period_column_width);
        rowHeight = (int) getResources().getDimension(R.dimen.grid_row_height);
        headerHeight = (int) getResources().getDimension(R.dimen.grid_header_height);
        cardGap = (int) getResources().getDimension(R.dimen.grid_card_gap);

        outlineColor = resolveColor(com.google.android.material.R.attr.colorOutline);
        textColor = resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant);
        todayHighlight = resolveColor(com.google.android.material.R.attr.colorPrimaryContainer);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(outlineColor);
        linePaint.setStrokeWidth(Math.max(1f, density * 0.5f));

        dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dimPaint.setColor(0x14000000);

        setWillNotDraw(false);
        buildHeaders();
    }

    public void setSessions(List<DisplaySession> sessions) {
        this.sessions.clear();
        this.sessions.addAll(sessions);
        rebuildCards();
        invalidate();
        requestLayout();
    }

    public void setTodayHighlight(int dayOfWeek, int currentPeriod) {
        this.todayDayOfWeek = dayOfWeek;
        this.currentPeriod = currentPeriod;
        invalidate();
    }

    public void setDateLabels(String[] dateLabels) {
        int[] dayLabels = {
                R.string.day_mon, R.string.day_tue, R.string.day_wed, R.string.day_thu,
                R.string.day_fri, R.string.day_sat, R.string.day_sun
        };
        for (int i = 0; i < 7 && i < (dateLabels == null ? 0 : dateLabels.length); i++) {
            TextView tv = (TextView) getChildAt(i);
            String day = getContext().getString(dayLabels[i]);
            String date = dateLabels[i];
            tv.setText(day + "\n" + (date != null ? date : ""));
            tv.setLineSpacing(0, 0.85f);
        }
    }

    private void buildHeaders() {
        int[] dayLabels = {
                R.string.day_mon, R.string.day_tue, R.string.day_wed, R.string.day_thu,
                R.string.day_fri, R.string.day_sat, R.string.day_sun
        };
        for (int labelRes : dayLabels) {
            TextView tv = new TextView(getContext());
            tv.setText(labelRes);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextColor(textColor);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            addView(tv);
        }
        for (int p = 1; p <= PERIOD_COUNT; p++) {
            TextView tv = new TextView(getContext());
            tv.setText(String.valueOf(p));
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextColor(textColor);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            addView(tv);
        }
    }

    private void rebuildCards() {
        for (CourseCardView card : cards) removeView(card);
        cards.clear();
        selectedIndices.clear();
        for (int i = 0; i < sessions.size(); i++) {
            DisplaySession s = sessions.get(i);
            if (s.dayOfWeek < 1 || s.dayOfWeek > DAY_COUNT) continue;
            if (s.startPeriod < 1 || s.endPeriod > PERIOD_COUNT || s.endPeriod < s.startPeriod) continue;
            CourseCardView card = new CourseCardView(getContext());
            card.bind(s);
            final int index = i;
            card.setOnClickListener(v -> handleCardClick(index));
            card.setOnLongClickListener(v -> {
                handleCardLongClick(index);
                return true;
            });
            card.setClickable(true);
            card.setFocusable(true);
            cards.add(card);
            addView(card);
        }
    }

    private void handleCardClick(int index) {
        if (selectionMode) {
            toggleSelection(index);
        } else {
            if (clickListener != null && index < sessions.size()) {
                clickListener.onSessionClicked(sessions.get(index).courseId);
            }
        }
    }

    private void handleCardLongClick(int index) {
        if (!selectionMode) {
            selectionMode = true;
        }
        toggleSelection(index);
    }

    private void toggleSelection(int index) {
        if (index >= cards.size()) return;
        CourseCardView card = cards.get(index);
        if (selectedIndices.contains(index)) {
            selectedIndices.remove(index);
            card.setSelectedState(false);
        } else {
            selectedIndices.add(index);
            card.setSelectedState(true);
        }
        if (selectedIndices.isEmpty()) {
            selectionMode = false;
        }
        Set<Long> ids = getSelectedCourseIds();
        if (selectionListener != null) selectionListener.onSelectionChanged(ids, selectionMode);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = headerHeight + PERIOD_COUNT * rowHeight;
        setMeasuredDimension(width, height);
        measureChildren(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = getWidth();
        int gridWidth = width - periodColumnWidth;
        int colWidth = gridWidth / DAY_COUNT;

        for (int day = 1; day <= DAY_COUNT; day++) {
            View header = getChildAt(day - 1);
            int xLeft = periodColumnWidth + (day - 1) * colWidth;
            header.layout(xLeft, 0, xLeft + colWidth, headerHeight);
        }
        int firstPeriodIndex = DAY_COUNT;
        for (int p = 1; p <= PERIOD_COUNT; p++) {
            View label = getChildAt(firstPeriodIndex + (p - 1));
            int yTop = headerHeight + (p - 1) * rowHeight;
            label.layout(0, yTop, periodColumnWidth, yTop + rowHeight);
        }
        for (int i = 0; i < cards.size(); i++) {
            CourseCardView card = cards.get(i);
            DisplaySession s = sessions.get(i);
            int xLeft = periodColumnWidth + (s.dayOfWeek - 1) * colWidth + cardGap;
            int yTop = headerHeight + (s.startPeriod - 1) * rowHeight + cardGap;
            int span = (s.endPeriod - s.startPeriod + 1) * rowHeight - cardGap;
            int w = colWidth - cardGap;
            card.measure(
                    MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(span, MeasureSpec.EXACTLY));
            card.layout(xLeft, yTop, xLeft + w, yTop + span);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int gridWidth = width - periodColumnWidth;
        int colWidth = gridWidth / DAY_COUNT;

        if (todayDayOfWeek >= 1 && todayDayOfWeek <= DAY_COUNT) {
            linePaint.setColor(todayHighlight);
            linePaint.setAlpha(60);
            int xLeft = periodColumnWidth + (todayDayOfWeek - 1) * colWidth;
            canvas.drawRect(xLeft, headerHeight, xLeft + colWidth,
                    headerHeight + PERIOD_COUNT * rowHeight, linePaint);
            linePaint.setAlpha(255);
            linePaint.setColor(outlineColor);
        }

        boolean[] dayHasSession = new boolean[DAY_COUNT + 1];
        for (DisplaySession s : sessions) {
            if (s.dayOfWeek >= 1 && s.dayOfWeek <= DAY_COUNT) dayHasSession[s.dayOfWeek] = true;
        }
        for (int d = 1; d <= DAY_COUNT; d++) {
            if (!dayHasSession[d]) {
                int xLeft = periodColumnWidth + (d - 1) * colWidth;
                canvas.drawRect(xLeft, headerHeight, xLeft + colWidth,
                        headerHeight + PERIOD_COUNT * rowHeight, dimPaint);
            }
        }

        for (int p = 0; p <= PERIOD_COUNT; p++) {
            float y = headerHeight + p * rowHeight;
            canvas.drawLine(periodColumnWidth, y, width, y, linePaint);
        }
        for (int d = 0; d <= DAY_COUNT; d++) {
            float x = periodColumnWidth + d * colWidth;
            canvas.drawLine(x, headerHeight, x, headerHeight + PERIOD_COUNT * rowHeight, linePaint);
        }
        canvas.drawLine(0, headerHeight, width, headerHeight, linePaint);
    }

    private @ColorInt int resolveColor(int attr) {
        TypedValue tv = new TypedValue();
        getContext().getTheme().resolveAttribute(attr, tv, true);
        if (tv.resourceId != 0) return ContextCompat.getColor(getContext(), tv.resourceId);
        return tv.data;
    }
}
