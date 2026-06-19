## 1. Database schema migration

- [x] 1.1 Create `SemesterEntity` table (name, startDate, totalWeeks, periodTimesJson, isActive) with auto-generated id; add DAO queries (insert/update/delete/getAll/getActive/setActive)
- [x] 1.2 Add `semesterId` column to `CourseEntity` with default value referencing the first semester; update DAO queries to filter by active semester
- [x] 1.3 Add `timetableProfiles` column (comma-separated String) to `CourseEntity`
- [x] 1.4 Write Room migration `MIGRATION_1_2`: create semesters table, insert a default row from existing `semester_config`, add columns to courses, drop old `semester_config` table
- [x] 1.5 Update `SemesterConfigRepository` to `SemesterRepository` working with the new multi-row table
- [x] 1.6 Update `AppDatabase` version and register migration

## 2. Semester management UI

- [ ] 2.1 Create `SemesterManageActivity` (list existing semesters, add/rename/delete, mark active)
- [ ] 2.2 Add semester picker to `MainActivity` toolbar (show active semester name, tap to open switcher bottom sheet)
- [ ] 2.3 Wire MainViewModel to observe active semester changes and re-query courses/sessions
- [ ] 2.4 On first launch after migration, ensure at least one semester exists

## 3. Course detail page improvements

- [ ] 3.1 Add week-pattern visual bar to `activity_course_detail.xml` and render it in `renderSessions()`
- [ ] 3.2 Add exception history list per session to the detail page (query `SessionExceptionEntity` for each session, display as cancel/move labels)
- [ ] 3.3 Add inline editing for session fields (tap location → edit dialog, tap weekPattern → week-picker dialog)
- [ ] 3.4 Make course name, teacher, and note fields editable inline on the detail page
- [ ] 3.5 Add profile assignment checkboxes to the course edit/detail screen

## 4. Timetable profiles

- [ ] 4.1 Add profile chip/toggle UI to `MainActivity` (bottom sheet or horizontal chip group showing available profiles for the active semester)
- [ ] 4.2 Wire profile toggles to MainViewModel: filter visible courses by active profile tags
- [ ] 4.3 Add profile CRUD: small inline UI to create/rename/delete profiles (stored as a Set<String> in the active semester config or a new table)

## 5. Batch operations

- [ ] 5.1 Implement multi-select mode in TimetableView/Grid: long-press enters mode, tap toggles selection, visual check overlay on selected cards, bottom action bar appears
- [ ] 5.2 Implement batch delete: confirm dialog → delete all selected courses via CourseRepository
- [ ] 5.3 Implement batch move to day: day-picker dialog → update `dayOfWeek` on all selected sessions
- [ ] 5.4 Implement batch cancel for week: week number input → create `TYPE_CANCEL` exceptions for all selected sessions
- [ ] 5.5 Implement batch edit: dialog for teacher/location/color → apply to all selected courses

## 6. Verification

- [ ] 6.1 Build the app, ensure it compiles without errors
- [ ] 6.2 Run on device: verify migration doesn't lose existing courses
- [ ] 6.3 Test semester creation and switching
- [ ] 6.4 Test detail page improvements (week bar, exceptions, inline editing)
- [ ] 6.5 Test profile creation, assignment, and toggle filtering
- [ ] 6.6 Test batch operations (delete, move, cancel, edit)
