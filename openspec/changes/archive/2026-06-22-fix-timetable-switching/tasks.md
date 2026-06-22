## 1. Fix: switchTo also switches active semester

- [x] 1.1 `TimetableRepository`: add `SemesterDao` field (injected via constructor from AppDatabase)
- [x] 1.2 `TimetableRepository.switchTo(id, semesterId)`: after `dao.clearActive(semesterId)` + `dao.setActive(id)`, also call `semesterDao.clearActive()` + `semesterDao.setActive(semesterId)`

## 2. Fix: WeekSession refreshes on timetable change

- [x] 2.1 `CourseRepository`: change `activeTimetableId` from `volatile Long` to `MutableLiveData<Long>`; expose getter
- [x] 2.2 `CourseRepository.setActiveTimetableId()`: use `setValue()` on the LiveData instead of direct field assignment
- [x] 2.3 `CourseRepository.observeWeekSessions()`: add `activeTimetableId` LiveData as third source in MediatorLiveData; on change, re-run `combine()`
- [x] 2.4 `CourseRepository.combine()`: read `activeTimetableId` from the LiveData's current value

## 3. Fix: MainViewModel semester cascade

- [x] 3.1 `MainViewModel`: when active timetable LiveData fires with a non-null timetable, call `onSemesterChanged()` with the timetable's semester if it differs from current
- [x] 3.2 `MainViewModel`: ensure `observeActiveTimetableForSemester()` is called with the timetable's semester (not the stale one)

## 4. Fix: home page empty state

- [x] 4.1 Add string `no_active_timetable` = "没有课表" to strings.xml
- [x] 4.2 `MainActivity.observeViewModel()`: when `activeTimetableName` is null, show "没有课表" instead of "未分配"
- [x] 4.3 `MainActivity.renderGrid()`: when no active timetable (sessions empty due to no filter), show "没有课表" empty state

## 5. Remove independent semester switching

- [x] 5.1 `SemesterManageActivity`: remove "设为当前学期" option from semester tap dialog
- [x] 5.2 `SettingsActivity`: remove semester switching if any independent switch UI exists

## 6. Timetable creation guard

- [x] 6.1 `TimetableManageActivity.promptCreate()`: check if semester list is empty; if so, show toast/dialog prompting to create a semester first
- [x] 6.2 `TimetableManageActivity.confirmImport()`: same guard as 6.1

## 7. Build & verify

- [x] 7.1 Build and resolve compilation errors
- [x] 7.2 Install and test: switch timetable → home page refreshes; no timetable → empty state; semester follows timetable
