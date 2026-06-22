## MODIFIED Requirements

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

## REMOVED Requirements

### Requirement: Switch active timetable filter
**Reason**: Replaced by updated version that includes semester switching
**Migration**: Use the updated "Switch active timetable filter" requirement above
