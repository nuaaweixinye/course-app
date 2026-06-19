## 1. Project infrastructure

- [x] 1.1 Enable ViewBinding in `app/build.gradle.kts` (`buildFeatures { viewBinding = true }`) and add lifecycle (viewmodel + livedata) entries to `gradle/libs.versions.toml` + `app/build.gradle.kts` dependencies; sync Gradle and verify a clean build.
- [x] 1.2 Add Room (runtime + annotationProcessor for compiler) to `gradle/libs.versions.toml` + `app/build.gradle.kts`; create the `AppDatabase extends RoomDatabase` class in `data/local/`; verify build.
- [x] 1.3 Create the `App extends Application` class, register it in `AndroidManifest.xml` (`android:name`), and expose a singleton database accessor; verify it initializes without crashing on a cold start.
- [x] 1.4 Create base package layout (`data/{local,local/dao,local/entity,model,repository,import}`, `ui/{main,task,course,import,settings,common}`, `widget/`) as empty packages so subsequent tasks have a home.

## 2. Data layer (entities + DAOs)

- [x] 2.1 Create `CourseEntity` (`id`, `name`, `teacher`, `colorTag`, `note`) + `CourseDao` (insert/update/delete + live query for all and by id); add to `AppDatabase`.
- [x] 2.2 Create `CourseSessionEntity` (`id`, `courseId` FK with cascade, `dayOfWeek`, `startPeriod`, `endPeriod`, `location`, `weekPattern`) + `CourseSessionDao` (insert/update/delete + by-course + by-day live queries); add to `AppDatabase`.
- [x] 2.3 Create `SessionExceptionEntity` (`id`, `sessionId` FK cascade, `weekNo`, `type`, `moveToDayOfWeek`) + `SessionExceptionDao` (insert/delete + by-session/week query); add to `AppDatabase`.
- [x] 2.4 Create `TaskEntity` (`id`, `title`, `type`, `courseId` nullable FK, `dueDate`, `done`, `note`) + `TaskDao` (CRUD + ordered-by-due live query + toggle-done); add to `AppDatabase`.
- [x] 2.5 Create `SemesterConfigEntity` (single-row, `id=1`, `startDate`, `totalWeeks`, `periodTimesJson`) + `SemesterConfigDao` (get/insert-or-update); add to `AppDatabase`. Bump DB version to 1.

## 3. Domain layer (models + utilities)

- [x] 3.1 Create domain model POJOs (`Course`, `CourseSession`, `Task`, `SemesterConfig`, `PeriodTime`) separate from entities, plus mappers in `data/model/`.
- [x] 3.2 Implement `WeekUtils` (pure Java): parse `weekPattern` strings (`"1-16"`, `"1,3,5,7"`, single/odd/even presets) into a `Set<Integer>`; `currentWeek(startDate, totalWeeks, today)` with clamping; `matchesWeek(pattern, weekNo)`.
- [x] 3.3 Implement `PeriodUtils` (pure Java): parse `periodTimesJson` into a `List<PeriodTime>`; `findCurrentPeriod(periods, now)` returning ongoing or next-up period.
- [x] 3.4 Add JUnit unit tests for `WeekUtils` (all/odd/even/custom/out-of-range) and `PeriodUtils` (within/after/before periods) in `app/src/test/java/com/courseshedule/`.

## 4. Repositories

- [x] 4.1 Implement `CourseRepository` (insert/update/delete course + its sessions in a transaction; observe courses, sessions by day, sessions-with-exceptions for a week via a DAO result relation).
- [x] 4.2 Implement `TaskRepository` (CRUD + toggle done + observe-by-due-date) and `SemesterConfigRepository` (get-or-seed-default + update; seeds today's-Monday + 16 weeks + default period-times JSON on first access).
- [x] 4.3 Add instrumented/in-memory Room tests for `CourseRepository` covering insert-with-sessions and cascade-delete in `app/src/androidTest/`.

## 5. Main timetable UI (MVP grid)

- [x] 5.1 Define the Material 3 `Theme.CourseShedule.DayNight` parent in `themes.xml`, indigo accent `#4f46e5`, and the 8-color palette resource (`colors.xml`); verify applies to a placeholder layout.
- [x] 5.2 Create `MainActivity` + `activity_main.xml` shell: `TopAppBar` (current-week label, settings action), week-switcher row, a `FrameLayout` placeholder for the grid, bottom navigation (课表/任务/设置), and an extended FAB; register in manifest as launcher. Verify it launches.
- [x] 5.3 Implement `MainViewModel` exposing `LiveData<SemesterConfig>`, `LiveData<Integer> selectedWeek`, `LiveData<List<display-session>>` (course + session + exception for the selected week), and week-switch actions.
- [x] 5.4 Implement a custom `TimetableView` (fixed row height, absolute positioning): renders weekday header row, period labels column, and course cards positioned by `dayOfWeek`/`startPeriod`/`endPeriod`. Start without cross-period merge polish.
- [x] 5.5 Implement the minimalist course-card visual (white/dark surface + 3dp colored left border by `colorTag`) and the today-column + current-period highlights; render weekend columns greyed when empty.
- [x] 5.6 Wire `MainViewModel` to `TimetableView` so the current week loads on launch and switching weeks re-renders; verify the empty-state renders when there are no sessions.

## 6. Course management UI

- [x] 6.1 Implement `CourseDetailActivity` + layout: shows course fields and all its sessions, with Edit and Delete actions; launch it from a tap on a grid card (passing courseId).
- [x] 6.2 Implement `CourseEditActivity` + `CourseEditViewModel` for create/edit: name/teacher/note fields, 8-color palette picker, and a dynamic list of session editors (weekday picker, period range pickers, location, week-pattern preset + custom multi-select 1–16).
- [x] 6.3 Add validation (name required, end period ≥ start, week pattern non-empty) and save/cancel; verify a created course appears on the grid and edits propagate.
- [x] 6.4 Implement delete-with-confirmation on `CourseDetailActivity`; verify cascade-delete removes sessions + exceptions and the grid updates.

## 7. Settings + semester config

- [x] 7.1 Implement `SettingsActivity` + layout: semester start date picker, total-weeks input, theme selector (follow system / light / dark), and entries for period-times editor + export.
- [x] 7.2 Implement the period-times editor screen/dialog (list of 12 periods with start/end time pickers) writing back to `SemesterConfig.periodTimesJson`; verify highlight updates after a change.
- [x] 7.3 Implement theme application: read the preference and call `AppCompatDelegate.setDefaultNightMode()`; verify light/dark/follow-system all behave.

## 8. File import

- [x] 8.1 Define the `TimetableImporter` interface and a `ParsedCourse`/`ParsedSession` value model in `data/import/` so file and future 教务 importers share a contract.
- [x] 8.2 Implement `CsvScheduleParser` with tolerant header mapping (课程/课程名/名称 → name; 老师/教师 → teacher; 星期/周几 → day; 节次 → periods; 教室 → location; 周次 → weekPattern); add unit tests with sample CSVs incl. a malformed one.
- [x] 8.3 Implement `IcsScheduleParser` mapping VEVENT + RRULE to sessions via `PeriodUtils`; add unit tests.
- [x] 8.4 Implement `ImportActivity` + `ImportViewModel`: file picker (SAF), parser dispatch by extension, parse on a background thread, and a preview screen with editable/deselectable rows; confirm writes via `CourseRepository` in a transaction. Verify a CSV round-trips into the grid.

## 9. Task tracking UI

- [x] 9.1 Implement `TaskListActivity` + `TaskListViewModel` (launched from the bottom nav "任务" tab): grouped-by-due-date list (overdue / today / upcoming) with homework/exam visual distinction and a completion toggle.
- [x] 9.2 Implement task create/edit dialog: title, type, optional course link, due date, note; validation (title + due required). Verify create/mark-done/delete all reflect on reopen.

## 10. Week exceptions UI

- [x] 10.1 Add a per-session "week exceptions" editor reachable from `CourseDetailActivity`: list exceptions (week, cancel/move), add cancel-for-week, add move-to-weekday, delete exception.
- [x] 10.2 Wire exception handling into the grid render path (WeekUtils matches → apply SessionException CANCEL hide / MOVED relocate) and verify cancel/move scenarios from the spec render correctly.

## 11. Home-screen widget

- [x] 11.1 Declare the widget (`TodayWidgetProvider` + `AppWidgetProviderInfo` XML) and add it to the manifest; create a 4×2 `initialLayout` with a header and a `ListView`.
- [x] 11.2 Implement `TodayWidgetService` + `RemoteViewsFactory` that queries today's remaining sessions (end-time > now) via repositories, applying current week + exceptions; verify it renders sample data.
- [x] 11.3 Trigger `AppWidgetManager.notifyAppWidgetViewDataChanged` from repositories on every mutating operation and set a click `PendingIntent` on the widget that opens `MainActivity`; verify add/delete reflects on the widget without reopening the app.

## 12. Export + polish

- [x] 12.1 Implement CSV export from `SettingsActivity` (courses + sessions) producing a file that re-imports to the same data; verify round-trip.
- [x] 12.2 Add the extended FAB actions (添加课程 → CourseEditActivity; 导入课表 → ImportActivity) replacing any placeholder, and confirm both paths work from `MainActivity`.
- [x] 12.3 Polish: cross-period card merge in `TimetableView`, empty states for task list + grid, dark-mode color verification across all screens, and weekend-collapse decision; verify the app renders cleanly in both themes end-to-end.
- [x] 12.4 Final verification: clean build, unit + instrumented tests pass, and a manual smoke test covering create course → import → switch week → add task → mark done → widget shows today → cancel a session → export.
