package com.courseshedule.data.imports;

import com.courseshedule.data.model.PeriodTime;
import com.courseshedule.data.model.PeriodUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Simplified iCalendar (.ics) importer. Handles VEVENT blocks with DTSTART and
 * a WEEKLY RRULE. Maps the event's start-time to a class period via the
 * period-times table, and the RRULE COUNT/UNTIL or BYDAY to a week pattern.
 *
 * Not a full RFC 5545 implementation; covers the common single-occurrence and
 * weekly-repeating exports produced by calendar tools.
 */
public class IcsScheduleParser implements TimetableImporter {

    private final BufferedReader reader;
    private final List<PeriodTime> periodTimes;

    public IcsScheduleParser(Reader reader, String periodTimesJson) {
        this.reader = new BufferedReader(reader);
        this.periodTimes = PeriodUtils.parse(periodTimesJson);
    }

    @Override
    public List<ParsedCourse> fetch() throws ImportException {
        try {
            List<Map<String, String>> events = readEvents();
            Map<String, ParsedCourse> byName = new LinkedHashMap<>();
            for (Map<String, String> ev : events) {
                ParsedSession s = toSession(ev);
                if (s == null) continue;
                String name = ev.getOrDefault("summary", "未命名");
                ParsedCourse course = byName.get(name);
                if (course == null) {
                    course = new ParsedCourse();
                    course.name = name;
                    course.teacher = "";
                    byName.put(name, course);
                }
                course.sessions.add(s);
            }
            return new ArrayList<>(byName.values());
        } catch (IOException e) {
            throw new ImportException("读取 ICS 失败", e);
        }
    }

    private List<Map<String, String>> readEvents() throws IOException {
        List<Map<String, String>> events = new ArrayList<>();
        Map<String, String> current = null;
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.equals("BEGIN:VEVENT")) {
                current = new LinkedHashMap<>();
            } else if (line.equals("END:VEVENT")) {
                if (current != null) events.add(current);
                current = null;
            } else if (current != null && line.contains(":")) {
                int colon = line.indexOf(':');
                String key = line.substring(0, colon).split(";")[0].toUpperCase();
                String value = line.substring(colon + 1);
                current.put(key.toLowerCase(), unescape(value));
            }
        }
        return events;
    }

    private ParsedSession toSession(Map<String, String> ev) {
        String dtstart = ev.get("dtstart");
        if (dtstart == null) return null;
        Calendar c = parseIcsDate(dtstart);
        if (c == null) return null;

        int dayOfWeek = calendarDayToMonFirst(c.get(Calendar.DAY_OF_WEEK));
        int nowMinutes = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        int period = nearestPeriod(nowMinutes);

        ParsedSession s = new ParsedSession();
        s.dayOfWeek = dayOfWeek;
        s.startPeriod = period;
        s.endPeriod = period;
        s.location = ev.getOrDefault("location", "");
        s.weekPattern = weekPatternFromRrule(ev.get("rrule"));
        return s;
    }

    private int nearestPeriod(int minutes) {
        if (periodTimes.isEmpty()) return 1;
        for (PeriodTime p : periodTimes) {
            if (Math.abs(minutes - p.startMinutes) <= 30) return p.index;
        }
        return PeriodUtils.findCurrentPeriod(periodTimes, minutes);
    }

    private String weekPatternFromRrule(String rrule) {
        // COUNT=N -> weeks 1..N; otherwise default all-16.
        if (rrule != null) {
            for (String token : rrule.split(";")) {
                if (token.toUpperCase().startsWith("COUNT=")) {
                    try {
                        int n = Integer.parseInt(token.substring(6));
                        StringBuilder sb = new StringBuilder();
                        for (int w = 1; w <= n; w++) {
                            if (w > 1) sb.append(',');
                            sb.append(w);
                        }
                        return sb.toString();
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return "1-16";
    }

    private static int calendarDayToMonFirst(int calDay) {
        // Calendar.SUNDAY=1..SATURDAY=7 -> Mon=1..Sun=7
        return (calDay == Calendar.SUNDAY) ? 7 : calDay - 1;
    }

    private static Calendar parseIcsDate(String value) {
        // Forms: 20260101T080000 or 20260101T080000Z or with TZID prefix already stripped.
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() < 8) return null;
        try {
            SimpleDateFormat fmt;
            if (digits.length() >= 14) {
                fmt = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            } else {
                fmt = new SimpleDateFormat("yyyyMMdd", Locale.US);
            }
            Date d = fmt.parse(digits.substring(0, Math.min(digits.length(), 14)));
            if (d == null) return null;
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    private static String unescape(String value) {
        return value.replace("\\,", ",").replace("\\n", "\n").replace("\\\\", "\\");
    }
}
