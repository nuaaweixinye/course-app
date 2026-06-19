package com.courseshedule.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.courseshedule.data.local.dao.CourseDao;
import com.courseshedule.data.local.dao.CourseSessionDao;
import com.courseshedule.data.local.dao.SemesterDao;
import com.courseshedule.data.local.dao.SessionExceptionDao;
import com.courseshedule.data.local.dao.TaskDao;
import com.courseshedule.data.local.dao.TimetableDao;
import com.courseshedule.data.local.entity.CourseEntity;
import com.courseshedule.data.local.entity.CourseSessionEntity;
import com.courseshedule.data.local.entity.SemesterEntity;
import com.courseshedule.data.local.entity.SessionExceptionEntity;
import com.courseshedule.data.local.entity.TaskEntity;
import com.courseshedule.data.local.entity.TimetableEntity;

@Database(
        entities = {
                CourseEntity.class,
                CourseSessionEntity.class,
                SessionExceptionEntity.class,
                TaskEntity.class,
                SemesterEntity.class,
                TimetableEntity.class
        },
        version = 5,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract CourseDao courseDao();

    public abstract CourseSessionDao courseSessionDao();

    public abstract SessionExceptionDao sessionExceptionDao();

    public abstract TaskDao taskDao();

    public abstract SemesterDao semesterDao();

    public abstract TimetableDao timetableDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS `semesters`");
            db.execSQL("CREATE TABLE IF NOT EXISTS `semesters` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`startDate` INTEGER NOT NULL, " +
                    "`totalWeeks` INTEGER NOT NULL, " +
                    "`periodTimesJson` TEXT NOT NULL, " +
                    "`isActive` INTEGER NOT NULL)");
            db.execSQL("INSERT OR IGNORE INTO semesters " +
                    "(id, name, startDate, totalWeeks, periodTimesJson, isActive) " +
                    "SELECT 1, '默认学期', startDate, totalWeeks, periodTimesJson, 1 " +
                    "FROM semester_config WHERE id = 1");
            db.execSQL("INSERT OR IGNORE INTO semesters " +
                    "(id, name, startDate, totalWeeks, periodTimesJson, isActive) " +
                    "VALUES (1, '默认学期', 0, 16, '', 1)");
            try {
                db.execSQL("ALTER TABLE courses ADD COLUMN semesterId INTEGER NOT NULL DEFAULT 1");
            } catch (Exception ignored) { }
            try {
                db.execSQL("ALTER TABLE courses ADD COLUMN timetableProfiles TEXT DEFAULT NULL");
            } catch (Exception ignored) { }
            db.execSQL("DROP TABLE IF EXISTS semester_config");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `timetables` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT, " +
                    "`semesterId` INTEGER NOT NULL)");

            // For each semester, create a default timetable named "默认课表"
            db.execSQL("INSERT OR IGNORE INTO timetables (id, name, semesterId) " +
                    "SELECT -1, '默认课表', id FROM semesters WHERE id IN " +
                    "(SELECT DISTINCT semesterId FROM courses)");

            // For courses with non-null timetableProfiles, create timetables from first tag
            db.execSQL("CREATE TEMP TABLE IF NOT EXISTS profile_map AS " +
                    "SELECT DISTINCT c.semesterId, " +
                    "TRIM(SUBSTR(c.timetableProfiles, 1, INSTR(c.timetableProfiles || ',', ',') - 1)) AS tag " +
                    "FROM courses c WHERE c.timetableProfiles IS NOT NULL AND c.timetableProfiles != ''");

            db.execSQL("INSERT OR IGNORE INTO timetables (name, semesterId) " +
                    "SELECT tag, semesterId FROM profile_map");

            // Add timetableId column to courses
            try {
                db.execSQL("ALTER TABLE courses ADD COLUMN timetableId INTEGER");
            } catch (Exception ignored) {
                // Column may already exist from a previous partial migration
            }

            // Update courses: set timetableId based on timetableProfiles tag
            db.execSQL("UPDATE courses SET timetableId = " +
                    "(SELECT t.id FROM timetables t " +
                    "WHERE t.semesterId = courses.semesterId " +
                    "AND t.name = TRIM(SUBSTR(courses.timetableProfiles, 1, INSTR(courses.timetableProfiles || ',', ',') - 1)) " +
                    "AND courses.timetableProfiles IS NOT NULL AND courses.timetableProfiles != '')");

            // For courses with null/empty timetableProfiles, link to default timetable
            db.execSQL("UPDATE courses SET timetableId = " +
                    "(SELECT id FROM timetables WHERE semesterId = courses.semesterId LIMIT 1) " +
                    "WHERE timetableId IS NULL");

            // Remove timetableProfiles column (recreate table for compatibility)
            db.execSQL("CREATE TABLE courses_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT, " +
                    "teacher TEXT, " +
                    "colorTag INTEGER NOT NULL, " +
                    "note TEXT, " +
                    "semesterId INTEGER NOT NULL, " +
                    "timetableId INTEGER)");
            db.execSQL("INSERT INTO courses_new (id, name, teacher, colorTag, note, semesterId, timetableId) " +
                    "SELECT id, name, teacher, colorTag, note, semesterId, timetableId FROM courses");
            db.execSQL("DROP TABLE courses");
            db.execSQL("ALTER TABLE courses_new RENAME TO courses");

            db.execSQL("DROP TABLE IF EXISTS profile_map");

            // Fix the negative id used for default timetables (reserve id=-1 for sentinel)
            db.execSQL("UPDATE timetables SET id = ABS(id) WHERE id < 0");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Recreate timetables table with nullable name column
            db.execSQL("CREATE TABLE timetables_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT, " +
                    "semesterId INTEGER NOT NULL)");
            db.execSQL("INSERT INTO timetables_new (id, name, semesterId) " +
                    "SELECT id, name, semesterId FROM timetables");
            db.execSQL("DROP TABLE timetables");
            db.execSQL("ALTER TABLE timetables_new RENAME TO timetables");

            // Recreate courses table without timetableProfiles and with clean timetableId column
            db.execSQL("CREATE TABLE courses_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT, " +
                    "teacher TEXT, " +
                    "colorTag INTEGER NOT NULL, " +
                    "note TEXT, " +
                    "semesterId INTEGER NOT NULL, " +
                    "timetableId INTEGER)");
            db.execSQL("INSERT INTO courses_new (id, name, teacher, colorTag, note, semesterId, timetableId) " +
                    "SELECT id, name, teacher, colorTag, note, semesterId, timetableId FROM courses");
            db.execSQL("DROP TABLE courses");
            db.execSQL("ALTER TABLE courses_new RENAME TO courses");
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE timetables ADD COLUMN isActive INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static AppDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "course_shedule.db"
                            )
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
