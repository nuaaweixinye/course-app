# timetable Specification

## Purpose
TBD - created by archiving change course-schedule-app. Update Purpose after archive.
## Requirements
### Requirement: Weekly grid display
The system SHALL render the user's schedule as a 7-column grid (one column per weekday Monday→Sunday) with one row per class period, showing all sessions whose week pattern matches the selected week.

#### Scenario: Empty week renders an empty grid
- **WHEN** the selected week has no matching sessions
- **THEN** the grid renders the 7 weekday columns and period rows with an empty-state affordance and no course cards

#### Scenario: Session renders in the correct cell
- **WHEN** a session exists for Wednesday (dayOfWeek=3), periods 1-2, in week 3
- **AND** the user has selected week 3
- **THEN** a single course card renders spanning periods 1 and 2 in the Wednesday column

#### Scenario: Weekend columns render greyed when empty
- **WHEN** the selected week has no Saturday or Sunday sessions
- **THEN** the Saturday and Sunday columns render visually de-emphasized (greyed) but remain visible

### Requirement: Week navigation
The system SHALL let the user navigate between weeks and SHALL default to the current week on launch.

#### Scenario: Defaults to current week
- **WHEN** the app launches and the current date falls inside the semester
- **THEN** the grid displays the week computed from the semester start date, labelled as "本周"

#### Scenario: User switches weeks
- **WHEN** the user taps the next/previous week control
- **THEN** the grid re-renders for the newly selected week and the week label updates

#### Scenario: Navigation clamps at semester bounds
- **WHEN** the user attempts to navigate before week 1 or after the total week count
- **THEN** the system prevents navigation past the bound and leaves the displayed week unchanged

### Requirement: Current period highlight
The system SHALL visually highlight the currently-in-progress period (or the next upcoming period) on the current week's grid.

#### Scenario: Highlights ongoing period
- **WHEN** the displayed week is the current week
- **AND** the current time falls inside a period's start-end window
- **THEN** that period's row is visually highlighted

#### Scenario: Highlights upcoming period between classes
- **WHEN** the current time falls between two periods
- **THEN** the next upcoming period's row is highlighted as "next"

### Requirement: Today column highlight
The system SHALL visually distinguish the current day's column when the displayed week is the current week.

#### Scenario: Current day column stands out
- **WHEN** the displayed week is the current week
- **THEN** the column matching today's weekday renders with a distinct background/highlight versus other weekday columns

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

