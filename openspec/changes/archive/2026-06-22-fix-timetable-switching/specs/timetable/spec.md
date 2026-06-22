## MODIFIED Requirements

### Requirement: Main page filters courses by active timetable
The main page SHALL display only courses belonging to the active timetable. If no timetable is active, the system SHALL display an empty state with the message "没有课表" instead of showing any courses.

#### Scenario: Active timetable filters course list
- **WHEN** user has an active timetable set and views the main page
- **THEN** only courses with `timetableId` matching the active timetable appear in the course list and on the week grid

#### Scenario: No active timetable shows empty state
- **WHEN** no timetable is active in the current semester
- **THEN** the main page displays the message "没有课表" and no course cards are rendered

#### Scenario: Changing active timetable updates main page immediately
- **WHEN** user navigates back to main page after setting a different active timetable
- **THEN** the main page automatically refreshes to show courses for the newly active timetable
