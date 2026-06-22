package com.courseshedule.ui.common;

import org.junit.Test;
import static org.junit.Assert.*;

public class ColorPaletteTest {

    @Test
    public void defaultTagCyclesThroughPalette() {
        assertEquals(0, ColorPalette.defaultTag(0));
        assertEquals(1, ColorPalette.defaultTag(1));
        assertEquals(ColorPalette.SIZE - 1, ColorPalette.defaultTag(ColorPalette.SIZE - 1));
        assertEquals(0, ColorPalette.defaultTag(ColorPalette.SIZE));
        assertEquals(1, ColorPalette.defaultTag(ColorPalette.SIZE + 1));
    }

    @Test
    public void colorResReturnsCorrectResource() {
        assertEquals(com.courseshedule.R.color.course_0, ColorPalette.colorRes(0));
        assertEquals(com.courseshedule.R.color.course_7, ColorPalette.colorRes(7));
    }

    @Test
    public void colorResBoundaryClampsToZero() {
        assertEquals(com.courseshedule.R.color.course_0, ColorPalette.colorRes(-1));
        assertEquals(com.courseshedule.R.color.course_0, ColorPalette.colorRes(ColorPalette.SIZE));
        assertEquals(com.courseshedule.R.color.course_0, ColorPalette.colorRes(999));
    }
}
