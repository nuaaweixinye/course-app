## ADDED Requirements

### Requirement: TimetableRepository switchTo ensures global uniqueness
The test suite SHALL verify that `TimetableRepository.switchTo` clears ALL timetables' isActive globally (not just within one semester) and sets only the target timetable active. It SHALL also verify the target semester becomes the sole active semester.

#### Scenario: switchTo clears previously active timetable in different semester
- **WHEN** timetable A in semester 1 is active, and switchTo is called for timetable B in semester 2
- **THEN** timetable A's isActive becomes 0, timetable B's isActive becomes 1, and no other timetable has isActive = 1

#### Scenario: switchTo activates the target semester
- **WHEN** switchTo is called for a timetable in semester 2 while semester 1 is active
- **THEN** semester 1's isActive becomes 0 and semester 2's isActive becomes 1

### Requirement: CourseRepository observeWeekSessions filters by timetable and semester
The test suite SHALL verify that `observeWeekSessions` returns only sessions matching the active semester, active timetable, and selected week.

#### Scenario: Only active timetable courses appear
- **WHEN** courses exist in timetable A and timetable B, and timetable A is active
- **THEN** observeWeekSessions returns only sessions from courses in timetable A

#### Scenario: Week pattern filtering
- **WHEN** a session has weekPattern "1-8" and week 10 is selected
- **THEN** that session does not appear in the results

#### Scenario: Cancelled session excluded
- **WHEN** a session has a CANCEL exception for week 5, and week 5 is selected
- **THEN** that session does not appear in the results

### Requirement: CourseRepository batch operations work correctly
The test suite SHALL verify batchDelete removes courses and cascades, and batchMove transfers courses between timetables.

#### Scenario: batchDelete removes multiple courses
- **WHEN** batchDelete is called with 3 course IDs
- **THEN** all 3 courses and their sessions are removed

#### Scenario: batchMove transfers courses to target timetable
- **WHEN** batchMove is called with 2 course IDs targeting timetable B
- **THEN** both courses have their timetableId set to timetable B's ID

### Requirement: TimetableDao observeAllWithSemester returns correct JOIN data
The test suite SHALL verify that `observeAllWithSemester` returns timetable rows with their parent semester names, with active semester timetables sorted first.

#### Scenario: JOIN returns semester name
- **WHEN** timetable "课表1" belongs to semester "大一上"
- **THEN** the query result includes a row with name="课表1" and semesterName="大一上"

#### Scenario: Active semester timetables first
- **WHEN** timetables exist in both active and non-active semesters
- **THEN** active semester's timetables appear before non-active semester's timetables

### Requirement: ColorPalette cycles correctly
The test suite SHALL verify that `ColorPalette.defaultTag` cycles through all palette indices.

#### Scenario: Sequential calls cycle through palette
- **WHEN** defaultTag is called with count 0, 1, ..., SIZE-1, SIZE
- **THEN** the results are 0, 1, ..., SIZE-1, 0

### Requirement: End-to-end integration test for timetable switching
The test suite SHALL include a full-flow integration test covering: create semester → create timetable → add courses → set timetable active → verify course filtering.

#### Scenario: Full flow from creation to filtering
- **WHEN** a semester is created, a timetable created, courses added, and the timetable set active
- **THEN** observeWeekSessions returns exactly those courses, and switching to a different timetable changes the results
