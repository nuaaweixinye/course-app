# semester-management Specification

## Purpose
Let users create, switch between, rename, and delete multiple semesters. Each semester has its own name, start date, and total week count, and owns an independent set of courses.

## ADDED Requirements

### Requirement: Create a semester
The system SHALL let the user create a new semester with a name, semester start date (Monday of week 1), and total week count.

#### Scenario: Create semester with defaults
- **WHEN** the user taps "新建学期" and enters "2025-2026-2" without changing other fields
- **THEN** a semester is created with the given name, current-Monday start date, and default 16-week total

#### Scenario: Name is required
- **WHEN** the user attempts to save a semester with an empty name
- **THEN** the save is blocked and a validation error is shown

### Requirement: Switch active semester
The system SHALL provide a semester picker (dropdown or bottom sheet) accessible from the main screen, and the grid SHALL display only courses belonging to the selected semester.

#### Scenario: Grid updates on semester switch
- **WHEN** the user switches from "2025-2026-2" to "2025-2026-1" in the picker
- **THEN** the grid re-renders showing only courses from semester "2025-2026-1"

#### Scenario: Semester config loads on switch
- **WHEN** the user switches to a semester with a different start date or week count
- **THEN** the week navigation, current-week calculation, and week labels update to match the new semester

### Requirement: Rename a semester
The system SHALL let the user rename an existing semester from a detail/edit screen.

#### Scenario: Rename updates everywhere
- **WHEN** the user renames "2025-2026-2" to "2025-2026-2 (补考)"
- **THEN** the picker and all semester labels reflect the new name

### Requirement: Delete a semester
The system SHALL let the user delete a semester, which SHALL cascade-delete all its courses and sessions.

#### Scenario: Delete with confirmation
- **WHEN** the user deletes a semester
- **THEN** a confirmation dialog warns about data loss
- **AND** on confirm, the semester and all its courses/sessions/exceptions are deleted
- **AND** the app switches to the next available semester (or shows empty state)

### Requirement: Semester picker location
The system SHALL place the semester picker in the main screen's toolbar area, showing the current semester name as a tappable title or dropdown button.

#### Scenario: Picker shows active semester
- **WHEN** the main screen loads
- **THEN** the toolbar displays the active semester's name as a tappable element
