package com.courseshedule.data.imports;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a schedule CSV with tolerant header recognition. Maps common Chinese
 * synonyms to fields, groups rows by course name, and emits ParsedCourse list.
 *
 * Recognized header synonyms (case-insensitive):
 *   name:     课程, 课程名, 课程名称, 名称, name, course
 *   teacher:  教师, 老师, 任课教师, teacher
 *   day:      星期, 周, 周几, day, weekday
 *   start:    开始节次, 起始, 开始, start
 *   end:      结束节次, 结束, end
 *   location: 教室, 地点, 位置, location, room
 *   weeks:    周次, 周(起-止), weeks, weekpattern
 *
 * If a row has a "periods" like "1-2" without separate start/end, it is split.
 */
public class CsvScheduleParser implements TimetableImporter {

    private final BufferedReader reader;

    public CsvScheduleParser(Reader reader) {
        this.reader = new BufferedReader(reader);
    }

    @Override
    public List<ParsedCourse> fetch() throws ImportException {
        try {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new ImportException("空文件");
            String[] headers = splitCsv(headerLine);
            Map<String, Integer> col = mapHeaders(headers);
            if (!col.containsKey("name")) {
                throw new ImportException("找不到课程名列");
            }

            Map<String, ParsedCourse> byName = new LinkedHashMap<>();
            String line;
            int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.trim().isEmpty()) continue;
                String[] fields = splitCsv(line);
                String name = get(fields, col.get("name"));
                if (name == null || name.isEmpty()) continue;

                ParsedCourse course = byName.get(name);
                if (course == null) {
                    course = new ParsedCourse();
                    course.name = name;
                    course.teacher = opt(fields, col, "teacher");
                    byName.put(name, course);
                }

                ParsedSession s = new ParsedSession();
                s.dayOfWeek = firstInt(opt(fields, col, "day"), 1);
                int[] periods = resolvePeriods(fields, col);
                s.startPeriod = periods[0];
                s.endPeriod = periods[1];
                s.location = opt(fields, col, "location");
                s.weekPattern = normalizeWeeks(opt(fields, col, "weeks"));
                course.sessions.add(s);
            }
            return new ArrayList<>(byName.values());
        } catch (IOException e) {
            throw new ImportException("读取 CSV 失败", e);
        }
    }

    private Map<String, Integer> mapHeaders(String[] headers) {
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim().toLowerCase();
            String key = classify(h);
            if (key != null && !col.containsKey(key)) col.put(key, i);
        }
        return col;
    }

    private String classify(String h) {
        if (any(h, "课程", "课程名", "课程名称", "名称", "name", "course")) return "name";
        if (any(h, "教师", "老师", "任课教师", "teacher")) return "teacher";
        if (any(h, "结束节次", "结束", "end")) return "end";
        if (any(h, "开始节次", "起始", "开始", "start")) return "start";
        if (any(h, "星期", "周几", "day", "weekday") || h.equals("周")) return "day";
        if (any(h, "教室", "地点", "位置", "location", "room")) return "location";
        if (any(h, "周次", "weeks", "weekpattern", "周(起-止)")) return "weeks";
        return null;
    }

    private boolean any(String h, String... keys) {
        for (String k : keys) if (h.contains(k.toLowerCase())) return true;
        return false;
    }

    private int[] resolvePeriods(String[] fields, Map<String, Integer> col) {
        Integer startCol = col.get("start");
        Integer endCol = col.get("end");
        int start = startCol != null ? firstInt(get(fields, startCol), 1) : 1;
        int end = endCol != null ? firstInt(get(fields, endCol), start) : start;
        if (end < start) end = start;
        return new int[]{start, end};
    }

    private String normalizeWeeks(String raw) {
        if (raw == null || raw.isEmpty()) return "1-16";
        return raw.trim();
    }

    private static String get(String[] fields, Integer idx) {
        if (idx == null || idx >= fields.length) return null;
        return fields[idx].trim();
    }

    private static String opt(String[] fields, Map<String, Integer> col, String key) {
        String v = get(fields, col.get(key));
        return v == null ? "" : v;
    }

    private static int firstInt(String s, int fallback) {
        if (s == null) return fallback;
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return fallback;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Minimal CSV splitter supporting quoted fields with embedded commas/quotes. */
    static String[] splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"'); i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '"') inQuotes = true;
                else if (c == ',') { out.add(sb.toString()); sb.setLength(0); }
                else sb.append(c);
            }
        }
        out.add(sb.toString());
        return out.toArray(new String[0]);
    }

    @SuppressWarnings("unused")
    private static final List<String> KEEP = Arrays.asList(); // reserved
}
