## ADDED Requirements

### Requirement: Week selection dialog from toolbar
The system SHALL let the user tap the week input field on the main page to open a dialog listing all weeks (1..totalWeeks). The current week SHALL be annotated. The user SHALL be able to tap any week to switch to it immediately.

#### Scenario: Tap week input opens selection dialog
- **WHEN** the user taps the week number input field on the main page
- **THEN** a dialog appears listing "第1周" through "第N周" (N = total weeks)
- **AND** the current week is annotated with "(本周)"

#### Scenario: Select a week from dialog
- **WHEN** the user taps "第5周" in the week selection dialog
- **THEN** the grid switches to week 5 and the dialog closes

#### Scenario: Manual input still works
- **WHEN** the user types a number and presses done in the week input field
- **THEN** the grid switches to that week (clamped to valid range) without opening the dialog
