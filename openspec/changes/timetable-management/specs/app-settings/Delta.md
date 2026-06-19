# app-settings Specification (Delta)

## Purpose
设置页增加学期管理和课表管理界面。

## ADDED Requirements

### Requirement: Semester management in settings
The system SHALL let the user manage all semesters from within the Settings page, without needing a separate Activity.

#### Scenario: View semesters list
- **WHEN** the user opens Settings and scrolls to the "学期" section
- **THEN** all semesters are listed with the active one marked

#### Scenario: Create semester from settings
- **WHEN** the user taps "新建学期" in the settings page
- **THEN** a dialog prompts for a name and creates the semester with default values

#### Scenario: Edit semester details
- **WHEN** the user taps a semester row
- **THEN** a dialog shows options: 设为当前学期 / 编辑（名称、开学日期、总周数）/ 删除

### Requirement: Timetable management in settings
The system SHALL let the user view, create, rename, delete, and switch timetables for the active semester from the Settings page.

#### Scenario: List timetables for active semester
- **WHEN** the user opens Settings and scrolls to the "课表" section
- **THEN** all timetables for the active semester are listed

#### Scenario: Create timetable
- **WHEN** the user taps "新建课表" and enters a name
- **THEN** a timetable record is created under the active semester

#### Scenario: Delete timetable
- **WHEN** the user deletes a timetable
- **THEN** a confirmation dialog warns that courses will be unlinked
- **AND** courses in that timetable get `timetableId` set to null

### Requirement: Batch operation (move to timetable)
The system SHALL provide a batch operation to move selected courses to a different timetable.

#### Scenario: Move selected courses
- **WHEN** the user selects 2+ courses and chooses "移入课表"
- **THEN** a dialog lists all timetables for the active semester
- **AND** selecting a target moves all selected courses to that timetable
