# home-widget Specification

## Purpose
TBD - created by archiving change course-schedule-app. Update Purpose after archive.
## Requirements
### Requirement: Today widget
The system SHALL provide a 4×2 home-screen widget that lists today's remaining courses (those whose end time is later than now), applying the current week and week-exceptions.

#### Scenario: Widget shows upcoming courses
- **WHEN** the device is on the current week and today has 2 sessions still ending later
- **THEN** the widget lists those 2 sessions with name, period, and location

#### Scenario: Empty day shows a friendly state
- **WHEN** today has no remaining sessions
- **THEN** the widget shows a "no more classes today" message

### Requirement: Widget respects exceptions
The system SHALL apply week-exceptions (cancellations and moves) when computing the widget's list, identical to the grid.

#### Scenario: Cancelled session excluded from widget
- **WHEN** a session is cancelled for the current week
- **THEN** it does not appear in today's widget even if it would otherwise be upcoming

### Requirement: Widget refreshes on data change
The system SHALL refresh the widget's content whenever schedule data changes, without requiring the user to reopen the app.

#### Scenario: New course appears in widget
- **WHEN** the user adds a course that meets today and returns to the home screen
- **THEN** the widget re-queries and displays the new session

### Requirement: Widget tap opens the app
The system SHALL open the main schedule screen when the widget is tapped.

#### Scenario: Tap launches main activity
- **WHEN** the user taps anywhere on the widget
- **THEN** the app launches at the main timetable screen on the current week

