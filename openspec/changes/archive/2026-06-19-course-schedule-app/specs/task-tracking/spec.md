## ADDED Requirements

### Requirement: Create a task
The system SHALL let the user create a homework or exam task with a title, due date, and type; the task MAY optionally link to a course.

#### Scenario: Create homework linked to a course
- **WHEN** the user creates a task titled "Lab 3 report", type homework, due Friday, linked to course "编程"
- **THEN** the task is persisted and appears in the task list grouped under its due date

#### Scenario: Title and due date required
- **WHEN** the user attempts to save a task without a title or due date
- **THEN** the save is blocked with a validation error

### Requirement: List tasks
The system SHALL list tasks ordered by due date, grouped for readability, with visual separation between homework and exams.

#### Scenario: Overdue and upcoming grouping
- **WHEN** the task list is shown and some tasks are past due
- **THEN** overdue tasks appear in a distinct group ahead of upcoming tasks

### Requirement: Mark task complete
The system SHALL let the user mark a task done; completed tasks SHALL be visually de-emphasized but retained.

#### Scenario: Toggle completion
- **WHEN** the user taps the completion control on a task
- **THEN** its done state toggles and it renders struck-through or greyed

### Requirement: Edit and delete tasks
The system SHALL let the user edit any task field or delete a task.

#### Scenario: Delete removes the task
- **WHEN** the user deletes a task
- **THEN** it is removed from storage and the list re-renders
