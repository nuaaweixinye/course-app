package com.courseshedule.data.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Pure-ish helpers for class-period times. Only Android dependency is
 * org.json (bundled in the framework); the time-matching logic is plain Java
 * and unit-testable via the minutes-based inputs.
 */
public final class PeriodUtils {

    /** Default 12-period day schedule as a JSON string (see design doc §3.3). */
    public static final String DEFAULT_PERIOD_TIMES_JSON = buildDefault();

    private PeriodUtils() {}

    /** Parse a period-times JSON array into ordered PeriodTime objects. */
    public static List<PeriodTime> parse(String json) {
        List<PeriodTime> out = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return out;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                int start = PeriodTime.toMinutes(o.getString("start"));
                int end = PeriodTime.toMinutes(o.getString("end"));
                out.add(new PeriodTime(i + 1, start, end));
            }
        } catch (JSONException e) {
            // Malformed config -> treat as empty; callers fall back to default.
            return new ArrayList<>();
        }
        return out;
    }

    /**
     * Find the current period given "now". Returns the in-progress period if
     * now falls inside one; otherwise the next upcoming period; otherwise -1 if
     * all periods are over.
     *
     * @param nowMinutes minutes since midnight (local)
     */
    public static int findCurrentPeriod(List<PeriodTime> periods, int nowMinutes) {
        for (PeriodTime p : periods) {
            if (nowMinutes >= p.startMinutes && nowMinutes <= p.endMinutes) {
                return p.index;
            }
        }
        for (PeriodTime p : periods) {
            if (nowMinutes < p.startMinutes) return p.index;
        }
        return -1;
    }

    /** Serialize a period-times list back to the stored JSON shape. */
    public static String toJson(List<PeriodTime> periods) {
        JSONArray arr = new JSONArray();
        for (PeriodTime p : periods) {
            JSONObject o = new JSONObject();
            try {
                o.put("start", fmt(p.startMinutes));
                o.put("end", fmt(p.endMinutes));
            } catch (JSONException e) {
                // JSONObject.put never throws for plain strings
            }
            arr.put(o);
        }
        return arr.toString();
    }

    /** Monday of the week containing the given time, at midnight local. */
    public static long mondayOfDay(long timeMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeMillis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        // Calendar.MONDAY == 2; first day of week is Sunday(1) by default in US locale.
        int dow = c.get(Calendar.DAY_OF_WEEK);
        int diff;
        if (dow == Calendar.SUNDAY) {
            diff = -6; // Sunday -> previous Monday
        } else {
            diff = Calendar.MONDAY - dow; // 2 - dow
        }
        c.add(Calendar.DAY_OF_MONTH, diff);
        return c.getTimeInMillis();
    }

    /** Format minutes-since-midnight as "HH:MM". */
    public static String format(int minutes) {
        return fmt(minutes);
    }

    private static String fmt(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return String.format("%02d:%02d", h, m);
    }

    private static String buildDefault() {
        // 4 morning + 4 afternoon + 4 evening
        String[][] slots = {
                {"08:00", "08:45"}, {"08:55", "09:40"}, {"10:00", "10:45"}, {"10:55", "11:40"},
                {"14:00", "14:45"}, {"14:55", "15:40"}, {"16:00", "16:45"}, {"16:55", "17:40"},
                {"19:00", "19:45"}, {"19:55", "20:40"}, {"20:50", "21:35"}, {"21:45", "22:30"}
        };
        JSONArray arr = new JSONArray();
        for (String[] s : slots) {
            JSONObject o = new JSONObject();
            try {
                o.put("start", s[0]);
                o.put("end", s[1]);
            } catch (JSONException e) {
                // ignore
            }
            arr.put(o);
        }
        return arr.toString();
    }
}
