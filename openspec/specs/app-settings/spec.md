# app-settings Specification

## Purpose
TBD - created by archiving change course-schedule-app. Update Purpose after archive.
## Requirements
### Requirement: Semester configuration
The system SHALL let the user set the semester start date (the Monday of week 1) and the total number of weeks; the current-week computation SHALL derive from these.

#### Scenario: Setting start date shifts current week
- **WHEN** the user changes the semester start date to a later Monday
- **THEN** the computed current week decreases accordingly and the grid reflects it on next open

#### Scenario: Defaults seeded on first launch
- **WHEN** the app launches for the first time and no semester config exists
- **THEN** a default config is seeded (start = today's Monday, total weeks = 16) so the app is usable immediately

### Requirement: Period-times editor
The system SHALL let the user view and edit the start/end time of each class period; the current-period highlight and widget SHALL use the edited values.

#### Scenario: Edited period time affects highlight
- **WHEN** the user changes period 1 to start at 08:30
- **AND** the current time is 08:35
- **THEN** the highlight treats period 1 as in-progress

### Requirement: Dark mode
The system SHALL support light and dark themes, defaulting to follow the system setting, with an optional manual override.

#### Scenario: Follows system dark mode by default
- **WHEN** the device is in dark mode and the app theme preference is "follow system"
- **THEN** the app renders in the dark theme

#### Scenario: Manual override
- **WHEN** the user sets the theme preference to "light" while the device is in dark mode
- **THEN** the app renders in the light theme regardless of the system setting

### Requirement: Data backup export
The system SHALL let the user export their schedule data to a file (CSV) so it can be re-imported or moved between devices.

#### Scenario: Export produces a re-importable file
- **WHEN** the user triggers export
- **THEN** a CSV file is produced that, when re-imported, reproduces the same courses and sessions

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

### Requirement: Batch operation (move to timetable)
The system SHALL provide a batch operation to move selected courses to a different timetable.

#### Scenario: Move selected courses
- **WHEN** the user selects 2+ courses and chooses "移入课表"
- **THEN** a dialog lists all timetables for the active semester
- **AND** selecting a target moves all selected courses to that timetable

### Requirement: Timetable creation requires existing semester
The system SHALL prevent timetable creation when no semester exists. The user SHALL be prompted to create a semester first.

#### Scenario: No semester exists
- **WHEN** the user attempts to create a timetable and no semester exists
- **THEN** the system shows a message prompting the user to create a semester first

