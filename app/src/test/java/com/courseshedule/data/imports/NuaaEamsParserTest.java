package com.courseshedule.data.imports;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NuaaEamsParserTest {

    private String sample() throws Exception {
        // nuaa_sample.html lives in src/test/resources.
        Reader r = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("nuaa_sample.html"),
                StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(r)) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    @Test
    public void parsesSampleCourses() throws Exception {
        List<ParsedCourse> courses = new NuaaEamsParser(sample()).fetch();
        // The sample has 操作系统 (twice, different weeks) and 计算机系统结构.
        assertTrue("expected at least 2 courses, got " + courses.size(), courses.size() >= 2);
    }

    @Test
    public void operatingSystemMonPeriodsAndWeeks() throws Exception {
        List<ParsedCourse> courses = new NuaaEamsParser(sample()).fetch();
        ParsedCourse os = find(courses, "操作系统");
        assertTrue("操作系统 should have a Monday session", hasSession(os, 1, 1, 2));
        ParsedSession mon = firstSession(os, 1);
        // vaildWeeks "001111111111110..." -> weeks 3-14
        assertEquals("3-14", mon.weekPattern);
        assertEquals("王立松", os.teacher);
    }

    @Test
    public void computerArchitectureTuePeriods() throws Exception {
        List<ParsedCourse> courses = new NuaaEamsParser(sample()).fetch();
        ParsedCourse ca = find(courses, "计算机系统结构");
        // index = 1*unitCount+2 and +3 -> Tuesday slots 2,3 -> app periods 3,4
        assertTrue("计算机系统结构 Tuesday 3-4", hasSession(ca, 2, 3, 4));
        assertEquals("顾晶晶", ca.teacher);
        assertEquals("D3102(将军路)", firstSession(ca, 2).location);
    }

    @Test
    public void lunchSlotsSkipped() throws Exception {
        List<ParsedCourse> courses = new NuaaEamsParser(sample()).fetch();
        // No session should ever map to a skipped lunch slot. Verify every session
        // has startPeriod/endPeriod in valid ranges and within 1..11.
        for (ParsedCourse c : courses) {
            for (ParsedSession s : c.sessions) {
                assertTrue("period>=1", s.startPeriod >= 1);
                assertTrue("period<=11", s.endPeriod <= 11);
            }
        }
    }

    @Test
    public void emptyHtmlThrows() {
        try {
            new NuaaEamsParser("<html></html>").fetch();
            org.junit.Assert.fail("expected ImportException");
        } catch (ImportException expected) {
            assertTrue(expected.getMessage().contains("TaskActivity"));
        }
    }

    private static ParsedCourse find(List<ParsedCourse> courses, String name) {
        for (ParsedCourse c : courses) if (c.name.equals(name)) return c;
        throw new AssertionError("course not found: " + name);
    }

    private static boolean hasSession(ParsedCourse c, int day, int start, int end) {
        for (ParsedSession s : c.sessions) {
            if (s.dayOfWeek == day && s.startPeriod == start && s.endPeriod == end) return true;
        }
        return false;
    }

    private static ParsedSession firstSession(ParsedCourse c, int day) {
        for (ParsedSession s : c.sessions) if (s.dayOfWeek == day) return s;
        throw new AssertionError("no session on day " + day);
    }
}
