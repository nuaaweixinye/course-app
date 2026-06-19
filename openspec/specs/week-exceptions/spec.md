# week-exceptions Specification

## Purpose
TBD - created by archiving change course-schedule-app. Update Purpose after archive.
## Requirements
### Requirement: Week pattern support
The system SHALL support per-session week patterns covering all-week, odd-week (单周), even-week (双周), and custom arbitrary week selections.

#### Scenario: Odd-week session renders only on odd weeks
- **WHEN** a session has week pattern "1,3,5,7,9,11,13,15"
- **AND** the user views an odd week
- **THEN** the session renders in the grid

#### Scenario: Odd-week session hidden on even weeks
- **WHEN** a session has week pattern "1,3,5,7,9,11,13,15"
- **AND** the user views an even week
- **THEN** the session does not render in the grid

### Requirement: Cancel a session for a specific week
The system SHALL let the user mark a session cancelled (停课) for a specific week without affecting other weeks.

#### Scenario: Cancelled session hidden that week
- **WHEN** the user cancels the Mon 1-2 高数 session for week 5
- **AND** the user views week 5
- **THEN** that session does not render, while other weeks still show it

### Requirement: Move a session to another day
The system SHALL let the user move (调换) a session to a different weekday for a specific week.

#### Scenario: Moved session renders on the target day
- **WHEN** the user moves the Wed 3-4 高数 session in week 6 to Friday
- **AND** the user views week 6
- **THEN** the session renders on Friday periods 3-4 and not on Wednesday

### Requirement: Exceptions do not mutate the base session
The system SHALL store exceptions as overrides on top of the base session, never editing the session's own week pattern or weekday.

#### Scenario: Exception is scoped to one week
- **WHEN** a session is cancelled for week 5 and the user views week 6
- **THEN** the session renders normally for week 6

