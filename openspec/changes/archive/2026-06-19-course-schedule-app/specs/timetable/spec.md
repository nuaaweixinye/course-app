## ADDED Requirements

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
