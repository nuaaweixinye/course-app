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

