# course-management Specification (Delta)

## ADDED Requirements

### Requirement: Week-pattern visualization
The system SHALL display each session's week pattern as a visual bar (filled cells for active weeks, empty for inactive) on the course detail page.

#### Scenario: Week bar shows 1-16 pattern
- **WHEN** a session has week pattern "1-16"
- **THEN** the detail page shows a 16-cell bar with all cells filled

#### Scenario: Week bar shows odd weeks
- **WHEN** a session has week pattern "1,3,5,7,9,11,13,15"
- **THEN** the bar shows filled cells at odd positions and empty at even positions

### Requirement: Exception history on detail page
The system SHALL display a list of all `session_exception` entries for each session (cancelled/moved weeks) on the course detail page.

#### Scenario: Exception list shows cancelled weeks
- **WHEN** a session has a `TYPE_CANCEL` exception for week 5
- **THEN** the detail page shows "第5周 停课" in the exception list for that session

#### Scenario: Exception list shows moved weeks
- **WHEN** a session has a `TYPE_MOVED` exception to Friday for week 8
- **THEN** the detail page shows "第8周 调至 周五" in the exception list

### Requirement: Inline session editing on detail page
The system SHALL let the user edit session fields (start/end period, location, week pattern) directly on the detail page without navigating to a separate editor.

#### Scenario: Edit location inline
- **WHEN** the user taps a session's location field on the detail page
- **THEN** an edit dialog opens
- **AND** on save, the session's location updates and the detail page refreshes

#### Scenario: Edit week pattern inline
- **WHEN** the user taps a session's week pattern on the detail page
- **THEN** a week-picker dialog opens showing all semester weeks as toggleable cells
- **AND** on save, the week pattern is updated

### Requirement: Notes section on detail page
The system SHALL display the course's `note` field as a visible, editable text area on the detail page.

#### Scenario: Show and edit note
- **WHEN** the course has a non-empty note
- **THEN** the note is displayed in a text area on the detail page
- **AND** the user can tap to edit and save changes inline

## MODIFIED Requirements

### Requirement: Edit a course
The system SHALL let the user edit any course field and any of its sessions from the detail page directly, or navigate to the full edit form for the course.

#### Scenario: Edit from detail page
- **WHEN** the user changes the course name inline on the detail page
- **THEN** the change saves and the grid reflects the updated name

## REMOVED Requirements

### Requirement: Delete a course
**Reason**: Delete functionality is retained, but now also accessible via batch operations. The detail page's delete action remains unchanged.
**Migration**: Detail page delete still works. Batch operations provide an alternative multi-course delete path.
