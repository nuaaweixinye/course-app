## Why

The first iteration (`course-schedule-app`) is running on-device but has usability defects on the home screen and a non-functional home widget. Testing on a real device surfaced: course cards render but their names are blank; the FAB/content still risks overlapping the navigation bars after the earlier partial fix; week switching requires repeated taps on ‹/›; and the home-screen widget does not appear/useful. This change fixes the home screen so it actually shows a correct, editable timetable and delivers a working widget.

## What Changes

- **Fix blank course names**: course cards currently render at the right cell but the name (and location) text is empty. Diagnose the `course.name → SessionWithCourse → DisplaySession → CourseCardView` flow and fix it so names and locations display.
- **Fully separate the three regions**: restructure `activity_main.xml` so the top app bar, the timetable content, and the bottom navigation never overlap (a vertical LinearLayout: app bar / content (weighted) / bottom nav), with the FAB clearly above the bottom nav.
- **Replace the week switcher with an editable week input**: instead of ‹/› arrows, the week is a numeric input field; typing a number jumps to that week (clamped to 1..totalWeeks), with quick ‹ › still available as small affordances.
- **Make the home-screen widget work**: verify the widget appears in the picker and renders today's courses as cards (name + period + colored bar); fix registration/rendering so long-pressing the home screen → widget → "课表" shows real data and updates on schedule changes.

## Capabilities

### New Capabilities
<!-- None — all four items adjust existing capabilities from the course-schedule-app change. -->

### Modified Capabilities
- `timetable`: course cards SHALL display course name and location; the screen layout SHALL keep top bar / content / bottom nav disjoint; week selection SHALL accept direct numeric entry.
- `home-widget`: the widget SHALL appear in the launcher widget picker and render today's courses with names; it SHALL refresh when the schedule changes.

## Impact

- **Code**: `MainActivity`, `activity_main.xml`, `MainViewModel` (week input), `TimetableView`/`CourseCardView` (name display + verify layout), the `SessionWithCourse` join / `DisplaySession` mapping, and `TodayWidgetProvider`/`TodayWidgetFactory` (+ widget XML if needed).
- **No schema/dependency changes**: Room tables and gradle deps are unchanged.
- **Risk**: the blank-name bug's root cause is in the save or join path; the fix may touch `CourseEditViewModel.save` or the `SessionWithCourse` POJO mapping.

## Non-goals

- **Redesigning the timetable grid** (columns, period count, styling stay).
- **Notifications, import, tasks, exceptions** — untouched.
- **Dynamic color / Material You** — still deferred.
