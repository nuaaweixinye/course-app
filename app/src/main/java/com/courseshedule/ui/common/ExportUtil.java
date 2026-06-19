package com.courseshedule.ui.common;

import android.content.Context;
import android.net.Uri;

import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.entity.CourseEntity;
import com.courseshedule.data.local.entity.CourseSessionEntity;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Exports the schedule to CSV. The output uses the tolerant header convention
 * that CsvScheduleParser understands, so export→import round-trips cleanly.
 */
public final class ExportUtil {

    private ExportUtil() {}

    public static boolean exportCsv(Context context, AppDatabase db, Uri target) {
        try {
            List<CourseEntity> allCourses = db.courseDao().listAll();

            OutputStream out = context.getContentResolver().openOutputStream(target);
            if (out == null) return false;
            OutputStreamWriter w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            try {
                w.write("课程,教师,星期,起始节次,结束节次,教室,周次\n");
                for (CourseEntity c : allCourses) {
                    List<CourseSessionEntity> sessions = db.courseSessionDao().listByCourse(c.id);
                    for (CourseSessionEntity s : sessions) {
                        w.write(csv(c.name));
                        w.write(",");
                        w.write(csv(c.teacher));
                        w.write(",");
                        w.write(String.valueOf(s.dayOfWeek));
                        w.write(",");
                        w.write(String.valueOf(s.startPeriod));
                        w.write(",");
                        w.write(String.valueOf(s.endPeriod));
                        w.write(",");
                        w.write(csv(s.location));
                        w.write(",");
                        w.write(csv(s.weekPattern));
                        w.write("\n");
                    }
                }
            } finally {
                w.flush();
                w.close();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Quote a CSV field if it contains comma/quote/newline. */
    private static String csv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
