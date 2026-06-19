package com.courseshedule.ui.common;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/** Stores the user's theme preference and applies it. */
public class ThemePrefs {

    private static final String FILE = "ui_prefs";
    private static final String KEY_THEME = "theme_mode";

    public static final int MODE_FOLLOW_SYSTEM = 0;
    public static final int MODE_LIGHT = 1;
    public static final int MODE_DARK = 2;

    private final SharedPreferences prefs;

    public ThemePrefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public int getMode() {
        return prefs.getInt(KEY_THEME, MODE_FOLLOW_SYSTEM);
    }

    public void setMode(int mode) {
        prefs.edit().putInt(KEY_THEME, mode).apply();
        apply(mode);
    }

    public void applyCurrent() {
        apply(getMode());
    }

    private void apply(int mode) {
        switch (mode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
