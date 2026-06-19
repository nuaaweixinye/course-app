## ADDED Requirements

### Requirement: Widget is installable from the launcher
The home-screen widget SHALL appear in the launcher's widget picker under the app, so a user can long-press the home screen and add it.

#### Scenario: Widget shows in the picker
- **WHEN** the user opens the launcher widget picker
- **THEN** a widget labeled with the app name and a 4×2 size is listed and addable

#### Scenario: Widget renders after placement
- **WHEN** the user places the widget on the home screen
- **THEN** it renders its header and the list of today's courses (or an empty-state message if none)

### Requirement: Widget lists today's remaining courses with names
The widget SHALL list today's courses whose end time is later than now, each showing the course name and period, with a colored bar matching the course's palette color.

#### Scenario: Course name appears in the widget
- **WHEN** the device is on a week/day containing a course that has not yet ended
- **THEN** the widget row for that course shows its name and period

#### Scenario: Empty state when nothing remains
- **WHEN** today has no remaining courses
- **THEN** the widget shows a friendly empty-state message

### Requirement: Widget updates on schedule changes
The widget SHALL refresh its contents when the schedule changes, without reopening the app.

#### Scenario: Added course appears in the widget
- **WHEN** the user adds a course that meets today and returns to the home screen
- **THEN** the widget re-queries and displays the new course

### Requirement: Widget tap opens the app
Tapping the widget SHALL open the main timetable screen.

#### Scenario: Tap launches main screen
- **WHEN** the user taps the widget
- **THEN** the app launches at the timetable screen on the current week
