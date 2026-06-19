## Why

The app currently supports only a single semester with a singleton config, making it impossible to keep historical timetables or prepare future ones. Users also lack batch operations for common schedule adjustments, and the course detail page is basic. These gaps limit the app's usefulness as a real semester-planning tool throughout a student's university career.

## What Changes

- **Semester management**: create, switch between, and delete multiple semesters, each with its own start date and week count. Courses/sessions belong to exactly one semester.
- **Batch operations**: multi-select courses or sessions for batch delete, batch move to another day, batch cancel for a week, and batch edit (teacher, location, color).
- **Course detail page improvements**: display session list with week-pattern visualization, exception history, inline session editing, and a "notes" section.
- **Multi-timetable profiles**: support multiple independent timetable profiles within a semester (e.g., "full timetable", "electives only", "review plan"), with profile switching on the main screen.

## Capabilities

### New Capabilities
- `semester-management`: Create, rename, switch, and delete semesters. Each semester owns its own courses and sessions. Semester picker accessible from the main screen.
- `batch-operations`: Multi-select mode in the grid or course list enabling batch delete, batch move-to-day, batch cancel-for-week, and batch edit (teacher, location, color).
- `timetable-profiles`: Create and switch between named timetable profiles within a semester. Profiles are independent course sets that can overlap or be toggled.

### Modified Capabilities
- `course-management`: Add notes display, exception history, inline session editing, and week-pattern visualization to the course detail page.

## Impact

- `CourseEntity`: add `timetableId` (nullable, for profiles) and `semesterId` columns
- `CourseSessionEntity`: add `semesterId` or link via course → semester
- `SemesterConfigEntity`: remove singleton pattern, add `name` and `isActive` columns, make `id` auto-generated
- New DAO queries for semester-scoped and profile-scoped data
- Room migration (version bump)
- `CourseDetailActivity.java`: major UI overhaul
- `MainActivity.java`: add semester picker, multi-select mode toggle
- New activities/fragments for batch operations and semester management
