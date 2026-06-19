package com.courseshedule.data.imports;

import com.courseshedule.data.model.WeekUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a rendered NUAA 金智(beangle) timetable page. The page embeds each class
 * as a {@code new TaskActivity(...)} call followed by {@code index = D*unitCount+P}
 * cell assignments. This parser extracts course name / room / teacher / weeks and
 * the day+period cells, maps NUAA's 13-slot day (skipping the 午休 lunch slots),
 * groups consecutive periods into one session, and compresses the 50-char
 * vaildWeeks binary string into the app's week pattern.
 *
 * Thread-safety: stateless; safe to call from any thread.
 */
public class NuaaEamsParser implements TimetableImporter {

    private static final Pattern UNIT_COUNT = Pattern.compile("var\\s+unitCount\\s*=\\s*(\\d+)");
    private static final Pattern INDEX_ASSIGN =
            Pattern.compile("index\\s*=\\s*(\\d+)\\s*\\*\\s*unitCount\\s*\\+\\s*(\\d+)");
    private static final Pattern VAILD_WEEKS = Pattern.compile("[01]{30,}");
    private static final Pattern STRING_LITERAL = Pattern.compile("\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"");
    private static final Pattern TEACHER_NAME = Pattern.compile("name:\\s*\"([^\"]+)\"");

    private final String html;

    public NuaaEamsParser(String html) {
        this.html = html == null ? "" : html;
    }

    @Override
    public List<ParsedCourse> fetch() throws ImportException {
        if (!html.contains("new TaskActivity(")) {
            throw new ImportException("未找到课表数据（new TaskActivity），请确认已登录并在课表页面");
        }
        int unitCount = parseUnitCount();

        // Locate every TaskActivity call site.
        List<Integer> starts = new ArrayList<>();
        int from = 0;
        int hit;
        while ((hit = html.indexOf("new TaskActivity(", from)) >= 0) {
            starts.add(hit);
            from = hit + 1;
        }

        Map<String, ParsedCourse> byName = new LinkedHashMap<>();
        for (int i = 0; i < starts.size(); i++) {
            int start = starts.get(i);
            int argsBegin = start + "new TaskActivity(".length();

            // This activity's region ends at the next TaskActivity or the marshal call.
            int regionEnd = (i + 1 < starts.size()) ? starts.get(i + 1) : html.length();
            int marshalAt = html.indexOf("marshalTable", start);
            if (marshalAt > 0 && marshalAt < regionEnd) regionEnd = marshalAt;
            String region = html.substring(argsBegin, Math.min(regionEnd, html.length()));

            // argsPart is the TaskActivity(...) arguments, before the index assignments.
            int indexKw = region.indexOf("index");
            String argsPart = indexKw > 0 ? region.substring(0, indexKw) : region;

            Matcher vw = VAILD_WEEKS.matcher(argsPart);
            if (!vw.find()) continue;
            String vaildWeeks = vw.group();

            List<String> lits = extractLiterals(argsPart);
            String courseName = lits.size() > 1 ? unquote(lits.get(1)) : "";
            String roomName = lits.size() > 4 ? unquote(lits.get(4)) : "";
            if (courseName.isEmpty()) continue;

            String teacher = findTeacherBefore(start);
            String weekPattern = weeksToPattern(vaildWeeks);

            // Collect (day, slot) cells for this activity, group per day, merge consecutive.
            Map<Integer, List<Integer>> slotsByDay = new TreeMap<>();
            Matcher im = INDEX_ASSIGN.matcher(region);
            while (im.find()) {
                int day = Integer.parseInt(im.group(1));
                int slot = Integer.parseInt(im.group(2));
                slotsByDay.computeIfAbsent(day, d -> new ArrayList<>()).add(slot);
            }

            ParsedCourse course = byName.get(courseName);
            if (course == null) {
                course = new ParsedCourse();
                course.name = courseName;
                course.teacher = teacher;
                byName.put(courseName, course);
            }

            for (Map.Entry<Integer, List<Integer>> e : slotsByDay.entrySet()) {
                int appDay = e.getKey() + 1; // 0=Mon -> 1
                List<Integer> periods = new ArrayList<>();
                for (int slot : e.getValue()) {
                    int p = slotToPeriod(slot, unitCount);
                    if (p > 0) periods.add(p); // skip lunch / invalid
                }
                if (periods.isEmpty()) continue;
                // Merge consecutive periods into sessions.
                int s = 0;
                while (s < periods.size()) {
                    int begin = periods.get(s);
                    int end = begin;
                    while (s + 1 < periods.size() && periods.get(s + 1) == end + 1) {
                        end = periods.get(++s);
                    }
                    ParsedSession ps = new ParsedSession(appDay, begin, end, roomName, weekPattern);
                    course.sessions.add(ps);
                    s++;
                }
            }
        }
        return new ArrayList<>(byName.values());
    }

    private int parseUnitCount() {
        Matcher m = UNIT_COUNT.matcher(html);
        return m.find() ? Integer.parseInt(m.group(1)) : 13;
    }

    /**
     * Map a NUAA slot index to an app period. NUAA's 13-slot day: 0-3 = morning
     * (periods 1-4), 4-5 = 午休 lunch (skipped), 6-9 = afternoon (5-8), 10-12 = evening (9-11).
     * Returns the app period (1-based), or -1 to skip.
     */
    private static int slotToPeriod(int slot, int unitCount) {
        if (unitCount != 13) {
            // Generic fallback: 1-based, no special lunch handling.
            return slot >= 0 ? slot + 1 : -1;
        }
        if (slot <= 3) return slot + 1;        // morning -> 1..4
        if (slot <= 5) return -1;              // lunch, skip
        return slot - 1;                       // afternoon/evening -> 5..11
    }

    private static String weeksToPattern(String vaildWeeks) {
        List<Integer> weeks = new ArrayList<>();
        for (int i = 0; i < vaildWeeks.length() && i < 53; i++) {
            if (vaildWeeks.charAt(i) == '1') weeks.add(i + 1);
        }
        String compressed = WeekUtils.compress(weeks);
        return compressed.isEmpty() ? "1" : compressed;
    }

    /** Nearest {@code name:"..."} before the TaskActivity call = the teacher. */
    private String findTeacherBefore(int callStart) {
        int windowStart = Math.max(0, callStart - 600);
        String window = html.substring(windowStart, callStart);
        Matcher m = TEACHER_NAME.matcher(window);
        String last = "";
        while (m.find()) last = m.group(1);
        return last;
    }

    private static List<String> extractLiterals(String s) {
        List<String> out = new ArrayList<>();
        Matcher m = STRING_LITERAL.matcher(s);
        while (m.find()) out.add(m.group());
        return out;
    }

    private static String unquote(String literal) {
        if (literal.length() >= 2 && literal.startsWith("\"") && literal.endsWith("\"")) {
            return literal.substring(1, literal.length() - 1);
        }
        return literal;
    }
}
