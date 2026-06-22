# timetable-entity Specification

## Purpose
课表是学期下的子分组，每个学期可以包含多个课表。每个课表有名称，课程可以归属于某个课表。

## Requirements
### Requirement: Create a timetable under a semester
The system SHALL let the user create a named timetable within a semester. The timetable SHALL be persisted with `id`, `name`, and `semesterId`. When creating a timetable, the system SHALL present a semester selector so the user can choose which semester the timetable belongs to, defaulting to the active semester.

#### Scenario: Create timetable with semester selection
- **WHEN** the user opens the timetable management page and taps "新建课表"
- **THEN** a dialog appears with a semester spinner (defaulting to the active semester) and a name input
- **AND** upon save, a new timetable is created linked to the selected semester

#### Scenario: Create timetable in active semester by default
- **WHEN** the user opens the create-timetable dialog without changing the semester spinner
- **THEN** the timetable is created under the currently active semester

#### Scenario: Timetable name is required
- **WHEN** the user attempts to save a timetable with an empty name
- **THEN** the save is blocked and a validation error is shown

### Requirement: List timetables by semester
The system SHALL list all timetables across all semesters in a single unified view. Each timetable row SHALL display the name of its parent semester. Timetables belonging to the active semester SHALL appear first, followed by other semesters in descending start-date order.

#### Scenario: Show all timetables with semester names
- **WHEN** the user opens the timetable management page
- **THEN** all timetables from all semesters are listed in a single view
- **AND** each row shows the timetable name and its parent semester name

#### Scenario: Active semester timetables sorted first
- **WHEN** timetables exist across multiple semesters
- **THEN** timetables belonging to the active semester appear before those of other semesters

#### Scenario: Timetable count per semester is visible
- **WHEN** semester "大一上" has 3 timetables and "大一下" has 2
- **THEN** the unified list shows all 5 timetables, each annotated with its semester name

### Requirement: Rename a timetable
The system SHALL let the user rename any timetable.

#### Scenario: Rename timetable
- **WHEN** the user renames a timetable from "旧名称" to "新名称"
- **THEN** the timetable's name is updated everywhere it is displayed

### Requirement: Delete a timetable
The system SHALL let the user delete a timetable. Courses in that timetable SHALL remain in the semester but become unlinked (timetableId set to null).

#### Scenario: Delete timetable unlinks courses
- **WHEN** the user deletes a timetable that contains 5 courses
- **THEN** the timetable record is removed
- **AND** all 5 courses have their timetableId set to null
- **AND** the courses remain visible in the semester's "全部" view

### Requirement: Switch active timetable filter
The system SHALL let the user set one timetable as active. When switching the active timetable, the system SHALL also switch the active semester to the timetable's owning semester. The active timetable determines which courses are displayed on the main page.

#### Scenario: Switch timetable switches semester
- **WHEN** the user sets timetable "暑期在校" (belonging to semester "暑期") as active
- **THEN** both the timetable and its semester "暑期" become active
- **AND** the main page refreshes to show courses from "暑期在校"

#### Scenario: Show only timetable-filtered courses
- **WHEN** the user selects timetable "在校" as active
- **THEN** only courses with `timetableId` matching that timetable appear in the grid

#### Scenario: Main page refreshes after switching timetable
- **WHEN** the user switches active timetable from timetable A to timetable B
- **THEN** the main page course list updates immediately to show timetable B's courses

### Requirement: Default timetable per semester
Each semester SHALL have a default timetable named "默认课表" created automatically on first access or migration.

#### Scenario: Default timetable exists after migration
- **WHEN** a semester with no timetables is first accessed
- **THEN** a default timetable "默认课表" is created and linked to that semester
