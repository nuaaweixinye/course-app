## MODIFIED Requirements

### Requirement: Semester configuration
The system SHALL let the user set the semester start date (the Monday of week 1) and the total number of weeks from within the semester management page. The settings page SHALL NOT contain semester-level configuration fields. The current-week computation SHALL derive from these values.

#### Scenario: Setting start date shifts current week
- **WHEN** the user edits a semester's start date to a later Monday via semester management
- **THEN** the computed current week decreases accordingly and the grid reflects it on next open

#### Scenario: Defaults seeded on first launch
- **WHEN** the app launches for the first time and no semester config exists
- **THEN** a default config is seeded (start = today's Monday, total weeks = 16) so the app is usable immediately

#### Scenario: Settings page does not show semester date or total weeks
- **WHEN** the user opens the Settings page
- **THEN** no semester start date picker or total weeks field is present
- **AND** these fields are accessible only through semester management's edit dialog
