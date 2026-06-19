# batch-operations Specification

## Purpose
Allow multi-selection of courses or sessions in the timetable grid for batch operations: delete, move to a different day, cancel for a specific week, and edit common fields.

## ADDED Requirements

### Requirement: Enter multi-select mode
The system SHALL let the user enter multi-select mode by long-pressing a course card on the grid. In this mode, tapping additional cards adds/removes them from the selection.

#### Scenario: Long-press enters selection mode
- **WHEN** the user long-presses a course card on the grid
- **THEN** the card shows a selected visual state (check overlay or highlight)
- **AND** a bottom action bar appears with batch operation buttons
- **AND** the toolbar shows the selection count

#### Scenario: Tap toggles selection
- **WHEN** the user taps another course card while in selection mode
- **THEN** that card toggles between selected and deselected

#### Scenario: Exit with back
- **WHEN** the user presses back or taps "取消" in the toolbar
- **THEN** selection mode exits and all cards return to normal state

### Requirement: Batch delete
The system SHALL let the user delete all selected courses and their sessions in one action with a confirmation dialog.

#### Scenario: Batch delete removes multiple courses
- **WHEN** the user has selected 3 courses and taps "批量删除"
- **THEN** a confirmation dialog shows the count ("确定删除 3 门课程？")
- **AND** on confirm, all 3 courses and their sessions are deleted
- **AND** the grid re-renders without them

### Requirement: Batch move to a different day
The system SHALL let the user move all selected sessions to a different weekday, affecting only their `dayOfWeek` field.

#### Scenario: Batch move relocates sessions
- **WHEN** the user has selected 2 courses and picks "移至周几" → chooses Friday
- **THEN** all sessions of the selected courses have their `dayOfWeek` changed to 5 (Friday)
- **AND** the grid re-renders showing them on Friday

### Requirement: Batch cancel for a week
The system SHALL let the user create a `TYPE_CANCEL` exception for all selected sessions for a specified week number.

#### Scenario: Batch cancel creates exceptions
- **WHEN** the user selects 2 courses and picks "停课" → enters week 5
- **THEN** a `session_exception` row with `TYPE_CANCEL` is created for each session in week 5
- **AND** the grid hides those sessions in week 5 but shows them in other weeks

### Requirement: Batch edit common fields
The system SHALL let the user batch-edit the teacher name, location, and color tag for all selected courses.

#### Scenario: Batch edit teacher
- **WHEN** the user selects 3 courses and picks "批量编辑" → changes teacher to "王老师"
- **THEN** all 3 courses have their `teacher` field updated
- **AND** the grid reflects the change immediately

#### Scenario: Batch edit color
- **WHEN** the user selects courses and changes their color tag to index 3 (blue)
- **THEN** all selected courses' color tags update and cards re-render in blue
