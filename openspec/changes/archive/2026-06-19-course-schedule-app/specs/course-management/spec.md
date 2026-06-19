## ADDED Requirements

### Requirement: Create a course with multiple sessions
The system SHALL let the user create a course and attach one or more weekly sessions to it. Each session SHALL specify weekday, start/end period, location, and a week pattern.

#### Scenario: Add a course that meets twice a week
- **WHEN** the user creates course "高等数学" with sessions Mon periods 1-2 in 教三301 (weeks 1-16) and Wed periods 3-4 in 教三301 (weeks 1-16)
- **THEN** both sessions are persisted under the same course
- **AND** both render in the grid for any matching week

#### Scenario: Name is required
- **WHEN** the user attempts to save a course with an empty name
- **THEN** the save is blocked and a validation error is shown

#### Scenario: Period range must be valid
- **WHEN** the user sets a session's end period earlier than its start period
- **THEN** the save is blocked and a validation error is shown

### Requirement: Edit a course
The system SHALL let the user edit any course field and any of its sessions; changes SHALL propagate to the grid.

#### Scenario: Edit teacher name
- **WHEN** the user changes a course's teacher and saves
- **THEN** all sessions of that course display the updated teacher

#### Scenario: Add another session to an existing course
- **WHEN** the user adds a third session to an existing two-session course and saves
- **THEN** the new session appears in the grid for matching weeks without duplicating the course

### Requirement: Delete a course
The system SHALL let the user delete a course, which SHALL cascade-delete all of its sessions and their week exceptions.

#### Scenario: Delete removes course and its sessions
- **WHEN** the user deletes a course that has 2 sessions and 1 week exception
- **THEN** the course, both sessions, and the exception are all removed from storage
- **AND** the grid re-renders without that course

### Requirement: Course color assignment
The system SHALL assign each course a color tag from a fixed 8-color palette, user-selectable on create/edit, used as the card's left border color across all screens.

#### Scenario: Color is selectable and persists
- **WHEN** the user picks palette index 2 (amber) for a new course and saves
- **THEN** that course's cards render with an amber left border in the grid, detail view, and widget

#### Scenario: New course gets a default color
- **WHEN** the user creates a course without explicitly choosing a color
- **THEN** a default palette index is assigned (e.g., the least-used color, or index 0)
