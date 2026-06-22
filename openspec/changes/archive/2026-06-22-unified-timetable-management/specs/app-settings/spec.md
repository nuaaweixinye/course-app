## MODIFIED Requirements

### Requirement: Semester management in settings
The system SHALL let the user manage all semesters from the Settings page. Semester management SHALL be limited to semester-specific operations (create, edit, delete, set active) and SHALL NOT include timetable management entry points.

#### Scenario: View semesters list
- **WHEN** the user opens the semester management page
- **THEN** all semesters are listed with the active one marked

#### Scenario: Create semester from settings
- **WHEN** the user taps "新建学期" in the semester management page
- **THEN** a dialog prompts for a name and creates the semester with default values

#### Scenario: Edit semester details
- **WHEN** the user taps a semester row
- **THEN** a dialog shows options: 编辑（名称、开学日期、总周数）/ 删除

#### Scenario: No timetable entry from semester page
- **WHEN** the user is on the semester management page
- **THEN** no "课表管理" button or link is present on semester rows

### Requirement: Timetable management in settings
The system SHALL let the user open a unified timetable management page from the Settings page. The timetable management page SHALL display all timetables across all semesters, not just the active semester's.

#### Scenario: Open timetable management from settings
- **WHEN** the user taps "管理课表" in the Settings page
- **THEN** the timetable management page opens showing all timetables across all semesters
- **AND** no semester pre-selection or filtering is applied

#### Scenario: Create timetable from unified view
- **WHEN** the user taps "新建课表" in the timetable management page
- **THEN** a dialog with a semester selector and name input appears
- **AND** the created timetable is linked to the selected semester

#### Scenario: Delete timetable
- **WHEN** the user deletes a timetable
- **THEN** a confirmation dialog warns that courses will be unlinked
- **AND** courses in that timetable get `timetableId` set to null
