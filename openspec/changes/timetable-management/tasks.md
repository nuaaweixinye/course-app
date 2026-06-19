## 1. Data Layer

- [x] 1.1 Create `TimetableEntity` (id, name, semesterId) + `TimetableDao`
- [x] 1.2 Add `TimetableEntity` to AppDatabase entities list, bump version to 3, create `MIGRATION_2_3`
- [x] 1.3 Change `CourseEntity.timetableProfiles` to `timetableId` (Long, nullable)
- [x] 1.4 Create `TimetableRepository` (CRUD + list by semester + active timetable cache)
- [x] 1.5 Update `CourseRepository` — remove `observeProfiles()`, add `setActiveTimetableId()`, filter sessions by timetableId

## 2. Settings Page Integration

- [x] 2.1 Add semester management UI to `SettingsActivity` (list semesters, CRUD, set active) — replaces `SemesterManageActivity`
- [x] 2.2 Add timetable management UI to `SettingsActivity` (list timetables for active semester, CRUD)
- [x] 2.3 Remove `SemesterManageActivity` + layout + manifest registration

## 3. Main Toolbar & Filtering

- [x] 3.1 Replace profile chips with timetable chips in `MainActivity` (load timetables for active semester)
- [x] 3.2 Wire `TimetableRepository.observeBySemester()` to chip group, "全部" chips shows all, specific chip filters by timetableId

## 4. Course Edit Screen

- [x] 4.1 Add timetable picker (dropdown/spinner) to `CourseEditActivity` — list timetables of current semester
- [x] 4.2 Save `timetableId` on course create/update

## 5. Batch Operations

- [x] 5.1 Change batch move dialog from selecting a semester to selecting a timetable
- [x] 5.2 Update `batchMoveCourses` to update `timetableId` instead of `semesterId`

## 6. Migration

- [x] 6.1 Implement MIGRATION_2_3: create timetables table, for each semester create a default "默认课表", migrate `timetableProfiles` to `timetableId` (first tag becomes timetable name, empty profiles → default timetable)
- [x] 6.2 Drop old `timetableProfiles` column from courses table after migration
