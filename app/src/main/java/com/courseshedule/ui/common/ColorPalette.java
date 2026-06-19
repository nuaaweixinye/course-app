package com.courseshedule.ui.common;

import com.courseshedule.R;

/** Maps a CourseEntity.colorTag (0..7) to its palette color resource id. */
public final class ColorPalette {

    private static final int[] COLORS = {
            R.color.course_0, R.color.course_1, R.color.course_2, R.color.course_3,
            R.color.course_4, R.color.course_5, R.color.course_6, R.color.course_7,
    };

    public static final int SIZE = COLORS.length;

    private ColorPalette() {}

    public static int colorRes(int tag) {
        if (tag < 0 || tag >= COLORS.length) return COLORS[0];
        return COLORS[tag];
    }

    /** A sensible default for a new course (cycles through the palette). */
    public static int defaultTag(int existingCourseCount) {
        return existingCourseCount % SIZE;
    }
}
