# timetable-entity Specification

## Purpose
课表是学期下的子分组，每个学期可以包含多个课表。每个课表有名称，课程可以归属于某个课表。

## ADDED Requirements

### Requirement: Create a timetable under a semester
The system SHALL let the user create a named timetable within a semester. The timetable SHALL be persisted with `id`, `name`, and `semesterId`.

#### Scenario: Create timetable in active semester
- **WHEN** the user creates a timetable named "在校课表" under the active semester
- **THEN** a new timetable record exists with that name linked to the active semester's id

#### Scenario: Timetable name is required
- **WHEN** the user attempts to save a timetable with an empty name
- **THEN** the save is blocked and a validation error is shown

### Requirement: List timetables by semester
The system SHALL list all timetables belonging to a given semester.

#### Scenario: Show semester's timetables
- **WHEN** semester "大一上" has 3 timetables
- **THEN** listing timetables for that semester returns exactly 3 records

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
The system SHALL let the user filter the timetable grid by a specific timetable, showing only courses assigned to that timetable.

#### Scenario: Show only timetable-filtered courses
- **WHEN** the user selects timetable "在校" in the toolbar chip group
- **THEN** only courses with `timetableId` matching that timetable appear in the grid

### Requirement: Default timetable per semester
Each semester SHALL have a default timetable named "默认课表" created automatically on first access or migration.

#### Scenario: Default timetable exists after migration
- **WHEN** a semester with no timetables is first accessed
- **THEN** a default timetable "默认课表" is created and linked to that semester
