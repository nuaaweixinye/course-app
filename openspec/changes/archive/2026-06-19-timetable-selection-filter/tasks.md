## 1. Data Layer — Timetable active state

- [x] 1.1 Add `isActive` boolean field to TimetableEntity (default false, @ColumnInfo)
- [x] 1.2 Add `clearActive`/`setActive`/`observeActive`/`getActive` queries to TimetableDao
- [x] 1.3 Register TimetableEntity change in AppDatabase (no change needed — already registered)
- [x] 1.4 Add MIGRATION_4_5: ALTER TABLE timetables ADD COLUMN isActive INTEGER NOT NULL DEFAULT 0
- [x] 1.5 Update database version from 4 to 5 in @Database annotation
- [x] 1.6 Add `switchTo(id, semesterId)` and `observeActive(long semesterId)` to TimetableRepository

## 2. Data Layer — Course filtering

- [x] 2.1 Add `observeByTimetable(long timetableId)` query to CourseDao
- [x] 2.2 Add `observeUnassigned(long semesterId)` query to CourseDao
- [x] 2.3 Refactor `CourseRepository.observeCourses()` to MediatorLiveData that re-queries based on activeTimetableId
- [x] 2.4 Update `combine()` in CourseRepository: when activeTimetableId is null, filter to sessions with timetableId IS NULL

## 3. ViewModel

- [x] 3.1 MainViewModel: observe active semester via SemesterRepository.observeActive()
- [x] 3.2 MainViewModel: when active semester changes → observe TimetableRepository.observeActive(newSemesterId)
- [x] 3.3 MainViewModel: when active timetable changes → update CourseRepository.activeTimetableId
- [x] 3.4 MainViewModel: expose activeTimetable name as LiveData<String> for MainActivity display
- [x] 3.5 MainViewModel: call semesterRepository.loadAsync() during init to seed DB, then set up active timetable observation

## 4. TimetableManageActivity UI

- [x] 4.1 Add "设为当前" option in timetable click dialog (show only when timetable is not active)
- [x] 4.2 In adapter, show active indicator (e.g., "✓" or highlight) on the active timetable row
- [x] 4.3 Wire "设为当前" click to TimetableRepository.switchTo(timetable.id, timetable.semesterId)
- [x] 4.4 Ensure list auto-refreshes via LiveData after switchTo (observeBySemester already triggers)

## 5. MainActivity UI

- [x] 5.1 Add a TextView above the course list showing active timetable name (or "未分配")
- [x] 5.2 Wire to MainViewModel's active timetable name LiveData
- [x] 5.3 Add string resources for "未分配" label

## 6. Strings & Polish

- [x] 6.1 Add string "设为当前" to strings.xml
- [x] 6.2 Add string "未分配" to strings.xml
- [x] 6.3 Add string "当前课表" header label to strings.xml
- [x] 6.4 Build and verify no compilation errors
- [x] 6.5 Install on device and test full flow: select timetable → return to main page → verify filtered courses
