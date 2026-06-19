## ADDED Requirements

### Requirement: Course card displays name and location
Each rendered course card SHALL display the course name (and, where space allows, the location) in the timetable grid.

#### Scenario: Added course shows its name
- **WHEN** the user adds a course named "高等数学" with a Monday period-1-2 session and returns to the grid
- **THEN** the card in the Monday / period-1-2 cell renders the text "高等数学" (and its location)

#### Scenario: Edited name propagates
- **WHEN** the user renames a course and saves
- **THEN** the grid card for that course shows the new name without a relaunch

### Requirement: Top bar, content, and bottom nav are disjoint
The home screen SHALL partition vertical space into three non-overlapping bands: top app bar, timetable content, and bottom navigation, such that no floating element (including the FAB and its expand menu) ever overlaps the navigation bars at any width/height or FAB state.

#### Scenario: FAB never overlaps bottom nav
- **WHEN** the home screen is displayed in any orientation
- **THEN** the FAB (and its expanded mini-FABs) render entirely above the bottom navigation with a visible gap

#### Scenario: Content sits between the bars
- **WHEN** the timetable renders
- **THEN** the grid occupies the band between the bottom of the top app bar and the top of the bottom navigation, clipping or scrolling rather than overlapping either

#### Scenario: Grid scrolls when taller than the content band
- **WHEN** the 12-period grid is taller than the available content band
- **THEN** the grid scrolls vertically within the content band without being cut off behind the navigation

### Requirement: Direct week entry
The week selector SHALL let the user type a week number to jump to that week, in addition to ±1 nudges.

#### Scenario: Typing a week jumps to it
- **WHEN** the user types "5" into the week field and confirms (IME action done / focus loss)
- **THEN** the grid re-renders for week 5 and the field shows 5

#### Scenario: Out-of-range input is clamped
- **WHEN** the user types a number greater than the total weeks (or less than 1) and confirms
- **THEN** the value is clamped to the nearest valid week (1 or totalWeeks) and the grid renders that week

#### Scenario: Nudge buttons still work
- **WHEN** the user taps the ‹ or › affordance
- **THEN** the selected week decrements or increments by 1 (clamped) and the grid re-renders
