package com.courseshedule.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure-Java helpers for week patterns and current-week computation. No Android
 * dependencies so it can be unit tested directly.
 *
 * Week patterns are compressed strings:
 *   "1-16"            range
 *   "1,3,5,7"         enumeration
 *   "1-5,8,10-12"     mixed
 * Whitespace tolerant; duplicates collapsed.
 */
public final class WeekUtils {

    /** Milliseconds in 7 days, used for current-week math. */
    public static final long WEEK_MILLIS = 7L * 24L * 60L * 60L * 1000L;

    private WeekUtils() {}

    /** Parse a week-pattern string into an ordered, de-duplicated set of week numbers. */
    public static Set<Integer> parse(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<Integer> weeks = new HashSet<>();
        for (String token : pattern.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.contains("-")) {
                String[] range = trimmed.split("-");
                if (range.length != 2) continue;
                int start = safeParse(range[0]);
                int end = safeParse(range[1]);
                if (start <= 0 || end <= 0) continue;
                int lo = Math.min(start, end);
                int hi = Math.max(start, end);
                for (int w = lo; w <= hi; w++) weeks.add(w);
            } else {
                int w = safeParse(trimmed);
                if (w > 0) weeks.add(w);
            }
        }
        return weeks;
    }

    /** True if the pattern includes the given week. */
    public static boolean matchesWeek(String pattern, int weekNo) {
        return parse(pattern).contains(weekNo);
    }

    /**
     * Compute the 1-based current week from the semester start (Monday, midnight
     * local). Clamps to [1, totalWeeks]. Returns 1 if today is before the start.
     */
    public static int currentWeek(long startDateMillis, int totalWeeks, long nowMillis) {
        if (nowMillis < startDateMillis) return 1;
        int week = (int) ((nowMillis - startDateMillis) / WEEK_MILLIS) + 1;
        if (week < 1) week = 1;
        if (totalWeeks > 0 && week > totalWeeks) week = totalWeeks;
        return week;
    }

    /** Convenience presets used by the course-edit week-pattern picker. */
    public static String allWeeks(int totalWeeks) {
        return "1-" + Math.max(1, totalWeeks);
    }

    public static String oddWeeks(int totalWeeks) {
        return joinParity(totalWeeks, true);
    }

    public static String evenWeeks(int totalWeeks) {
        return joinParity(totalWeeks, false);
    }

    public static String fromSelection(List<Integer> selectedWeeks) {
        List<Integer> sorted = new ArrayList<>(selectedWeeks);
        Collections.sort(sorted);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(sorted.get(i));
        }
        return sb.toString();
    }

    public static List<Integer> expand(String pattern) {
        List<Integer> sorted = new ArrayList<>(parse(pattern));
        Collections.sort(sorted);
        return sorted;
    }

    /** Compress a sorted list of week numbers into a compact pattern string:
     *  consecutive runs become "start-end" (e.g. 3..14 -> "3-14"), singles stay "n". */
    public static String compress(List<Integer> weeks) {
        if (weeks == null || weeks.isEmpty()) return "";
        List<Integer> sorted = new ArrayList<>(weeks);
        Collections.sort(sorted);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < sorted.size()) {
            int start = sorted.get(i);
            int end = start;
            while (i + 1 < sorted.size() && sorted.get(i + 1) == end + 1) {
                end = sorted.get(++i);
            }
            if (sb.length() > 0) sb.append(',');
            if (start == end) sb.append(start);
            else sb.append(start).append('-').append(end);
            i++;
        }
        return sb.toString();
    }

    private static String joinParity(int totalWeeks, boolean odd) {
        int rem = odd ? 1 : 0;
        List<String> out = new ArrayList<>();
        for (int w = 1; w <= totalWeeks; w++) {
            if (w % 2 == rem) out.add(String.valueOf(w));
        }
        return String.join(",", out);
    }

    private static int safeParse(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
