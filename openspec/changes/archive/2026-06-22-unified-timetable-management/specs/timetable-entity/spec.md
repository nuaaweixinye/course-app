## MODIFIED Requirements

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
