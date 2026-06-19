## ADDED Requirements

### Requirement: User can set a timetable as active for the semester
The system SHALL allow users to set one timetable per semester as "active". Only one timetable per semester can be active at a time. The active timetable determines which courses are displayed on the main page.

#### Scenario: Set timetable as active from manage page
- **WHEN** user opens TimetableManageActivity and taps a timetable, then selects "设为当前"
- **THEN** the system sets that timetable as the active timetable for the current semester, shows a checkmark indicator on the row, and the timetable remains on the manage page

#### Scenario: Setting another timetable replaces active state
- **WHEN** timetable A is active and user sets timetable B as active
- **THEN** timetable A becomes inactive (isActive = 0) and timetable B becomes active (isActive = 1)

#### Scenario: Deleting active timetable clears active state
- **WHEN** user deletes the currently active timetable
- **THEN** the system removes the timetable and the semester no longer has an active timetable

### Requirement: Main page filters courses by active timetable
The main page SHALL display only courses belonging to the active timetable. If no timetable is active, the system SHALL display only courses with no timetable assignment (`timetableId IS NULL`).

#### Scenario: Active timetable filters course list
- **WHEN** user has an active timetable set and views the main page
- **THEN** only courses with `timetableId` matching the active timetable appear in the course list and on the week grid

#### Scenario: No active timetable shows unassigned courses
- **WHEN** no timetable is active in the current semester
- **THEN** only courses with `timetableId IS NULL` appear in the course list and on the week grid

#### Scenario: Changing active timetable updates main page
- **WHEN** user navigates back to main page after setting a different active timetable
- **THEN** the main page automatically refreshes to show courses for the newly active timetable

### Requirement: Main page displays active timetable name
The system SHALL show the name of the currently active timetable on the main page, or "未分配" when no timetable is active.

#### Scenario: Active timetable name shown
- **WHEN** an active timetable exists and user is on the main page
- **THEN** the timetable name is displayed above the course list

#### Scenario: "Unassigned" shown when no active timetable
- **WHEN** no active timetable exists and user is on the main page
- **THEN** the text "未分配" is displayed

### Requirement: Active timetable persists across restarts
The system SHALL persist the active timetable state in the database so it survives app restarts.

#### Scenario: Active timetable restored after restart
- **WHEN** user sets an active timetable, closes the app, and reopens it
- **THEN** the same timetable is still active and the main page shows its courses

#### Scenario: Active timetable per semester is independent
- **WHEN** user switches to a different semester and then switches back
- **THEN** each semester's active timetable is independently preserved
