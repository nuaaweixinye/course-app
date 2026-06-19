# course-management Specification (Delta: timetable-assignment)

## Purpose
课程关联方式从 `timetableProfiles` 标签改为 `timetableId` 外键。

## ADDED Requirements

### Requirement: Create a course with timetable assignment
A course SHALL optionally be assigned to a timetable via `timetableId`. The create/edit screen SHALL show a timetable picker.

#### Scenario: Assign course to a timetable
- **WHEN** the user creates a course and selects timetable "在校课表"
- **THEN** the course's `timetableId` is set to the selected timetable's id

#### Scenario: Course without timetable is visible in "全部" view
- **WHEN** a course has no timetableId assigned
- **THEN** it appears in the "全部" chip view but not in any specific timetable filter

### Requirement: Edit course timetable assignment
The system SHALL let the user change a course's timetable via the edit screen.

#### Scenario: Change course's timetable
- **WHEN** the user changes a course's timetable from "在校" to "网课" and saves
- **THEN** the course disappears from the "在校" filtered grid
- **AND** appears in the "网课" filtered grid

### Requirement: Batch move courses between timetables
The system SHALL let the user select multiple courses and move them to a different timetable in one action.

#### Scenario: Batch move to another timetable
- **WHEN** the user selects 3 courses and chooses to move them to timetable "网课"
- **THEN** all 3 courses have their `timetableId` updated to the target timetable's id
