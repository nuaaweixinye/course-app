package com.courseshedule.data.imports;

import org.junit.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CsvScheduleParserTest {

    @Test
    public void parsesStandardHeaders() throws ImportException {
        String csv = "课程,教师,星期,开始节次,结束节次,教室,周次\n"
                + "高等数学,李老师,1,1,2,教三301,1-16\n"
                + "高等数学,李老师,3,3,4,教三301,1-16\n"
                + "英语,张老师,2,1,2,外院2,1-16\n";
        List<ParsedCourse> courses = new CsvScheduleParser(new StringReader(csv)).fetch();
        assertEquals(2, courses.size());
        ParsedCourse math = courses.get(0);
        assertEquals("高等数学", math.name);
        assertEquals("李老师", math.teacher);
        assertEquals(2, math.sessions.size());
        assertEquals(1, math.sessions.get(0).dayOfWeek);
        assertEquals(1, math.sessions.get(0).startPeriod);
        assertEquals(2, math.sessions.get(0).endPeriod);
        assertEquals("1-16", math.sessions.get(0).weekPattern);
    }

    @Test
    public void tolerantHeaderSynonyms() throws ImportException {
        String csv = "名称,老师,周几,起始,结束,地点\n"
                + "物理,王,4,5,6,理科楼\n";
        List<ParsedCourse> courses = new CsvScheduleParser(new StringReader(csv)).fetch();
        assertEquals(1, courses.size());
        assertEquals("物理", courses.get(0).name);
        assertEquals(4, courses.get(0).sessions.get(0).dayOfWeek);
        assertEquals(5, courses.get(0).sessions.get(0).startPeriod);
        assertEquals(6, courses.get(0).sessions.get(0).endPeriod);
    }

    @Test
    public void quotedFieldsWithCommas() throws ImportException {
        String csv = "课程,教室\n"
                + "政治,\"A,B 教室\"\n";
        List<ParsedCourse> courses = new CsvScheduleParser(new StringReader(csv)).fetch();
        assertEquals("A,B 教室", courses.get(0).sessions.get(0).location);
    }

    @Test
    public void missingNameColumnThrows() {
        String csv = "教师,教室\n张,教三\n";
        try {
            new CsvScheduleParser(new StringReader(csv)).fetch();
            fail("expected ImportException");
        } catch (ImportException expected) {
            assertTrue(expected.getMessage().contains("课程"));
        }
    }

    @Test
    public void emptyFileThrows() {
        try {
            new CsvScheduleParser(new StringReader("")).fetch();
            fail("expected ImportException");
        } catch (ImportException expected) {
            // ok
        }
    }

    @Test
    public void blankLinesSkipped() throws ImportException {
        String csv = "课程,星期\nA,1\n\nB,2\n";
        List<ParsedCourse> courses = new CsvScheduleParser(new StringReader(csv)).fetch();
        assertEquals(2, courses.size());
    }
}
