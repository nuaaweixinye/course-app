package com.courseshedule.ui.main;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

import com.courseshedule.R;
import com.courseshedule.data.model.DisplaySession;
import com.courseshedule.ui.common.ColorPalette;

public class CourseCardView extends LinearLayout {

    private final View coloredBar;
    private final TextView nameText;
    private final TextView locText;
    private boolean isSelectedState = false;
    private final Paint selectedPaint;
    private @ColorInt int selectedTint;

    public CourseCardView(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        setBackground(ContextCompat.getDrawable(context, R.drawable.bg_course_card));

        int padH = (int) getResources().getDimension(R.dimen.grid_card_padding_h);
        int padV = (int) getResources().getDimension(R.dimen.grid_card_padding_v);

        coloredBar = new View(context);
        LayoutParams barLp = new LayoutParams(
                (int) getResources().getDimension(R.dimen.grid_card_border),
                LayoutParams.MATCH_PARENT);
        addView(coloredBar, barLp);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(VERTICAL);
        textCol.setGravity(Gravity.CENTER_VERTICAL);
        textCol.setPadding(padH, padV, padH, padV);
        LayoutParams textLp = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        addView(textCol, textLp);

        nameText = new TextView(context);
        nameText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        nameText.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
        nameText.setMaxLines(2);
        textCol.addView(nameText, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        locText = new TextView(context);
        locText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
        locText.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        locText.setMaxLines(1);
        textCol.addView(locText, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        setPadding(0, 0, 0, 0);

        selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(3 * getResources().getDisplayMetrics().density);
        selectedTint = resolveColor(android.R.attr.colorPrimary);

        setWillNotDraw(false);
    }

    public void bind(DisplaySession session) {
        @ColorInt int color = ContextCompat.getColor(getContext(),
                ColorPalette.colorRes(session.colorTag));
        coloredBar.setBackgroundColor(color);
        nameText.setText(session.courseName);
        locText.setText(session.location);
    }

    void setSelectedState(boolean selected) {
        if (this.isSelectedState != selected) {
            this.isSelectedState = selected;
            invalidate();
        }
    }

    boolean isSelectedState() {
        return isSelectedState;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isSelectedState) {
            selectedPaint.setColor(selectedTint);
            canvas.drawRoundRect(1.5f, 1.5f, getWidth() - 1.5f, getHeight() - 1.5f,
                    8 * getResources().getDisplayMetrics().density,
                    8 * getResources().getDisplayMetrics().density, selectedPaint);
        }
    }

    private @ColorInt int resolveColor(int attr) {
        TypedValue tv = new TypedValue();
        getContext().getTheme().resolveAttribute(attr, tv, true);
        if (tv.resourceId != 0) return ContextCompat.getColor(getContext(), tv.resourceId);
        return tv.data;
    }
}
