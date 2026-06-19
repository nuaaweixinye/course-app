## Why

Students need a clean, offline-first 课表 (course schedule) app for Chinese universities. Existing apps are ad-heavy or lock features behind accounts. This project builds a lightweight, locally-stored schedule with file import, homework/exam tracking, week-exception (调休) handling, and a home-screen widget — using the project's existing Java + Material 3 stack.

## What Changes

- Build a complete course-schedule Android app from the empty project shell (no Activity exists yet).
- Add weekly 7-column grid timetable with week switching and current-period highlighting.
- Add manual course CRUD: a course may span multiple weekly time slots (sessions), each with a week pattern (全周/单周/双周/自定义).
- Add file import (.csv / .xlsx / .ics) with parse → preview → confirm flow, plus a `TimetableImporter` interface reserved for v2 教务系统 integration.
- Add homework/exam task tracking, independent of the timetable, optionally linked to a course.
- Add week-exception handling (停课 / 调换 / single-double week) overlaid on the schedule.
- Add a 4×2 home-screen widget showing today's remaining courses.
- Add app settings: semester start date, period-times editor, dark-mode toggle (follows system).
- Visual style: minimalist white cards with colored left borders; Material 3 app shell.

## Capabilities

### New Capabilities
- `timetable`: weekly 7-column grid display, week navigation, current-week/current-period computation and highlighting.
- `course-management`: create/read/update/delete courses and their multi-session time slots; course detail and edit forms.
- `course-import`: parse, preview, and import schedule files (.csv/.xlsx/.ics); defines the `TimetableImporter` extension interface.
- `task-tracking`: homework and exam reminders with due dates, optional course link, and completion state.
- `week-exceptions`: per-session week-level overrides for cancellations and moved classes; single/double-week patterns.
- `home-widget`: app-widget rendering today's upcoming courses with tap-to-open.
- `app-settings`: semester configuration, period-times, and theme/dark-mode preferences.

### Modified Capabilities
<!-- None — greenfield project, no existing specs. -->

## Impact

- **New code**: entire `com.courseshedule` app package — data layer (Room entities/DAOs/repository), UI (6 Activities + ViewModels + adapters), import parsers, widget provider, and `Application` initializer.
- **Dependencies**: add Room (runtime + compiler), lifecycle (viewmodel + livedata); enable ViewBinding. Evaluate a lightweight XLSX reader (CSV/ICS first).
- **Build config**: `app/build.gradle.kts` enables ViewBinding; `gradle/libs.versions.toml` gains new entries; `AndroidManifest.xml` declares Activities and the widget provider/receiver.
- **Resources**: Material 3 `DayNight` theme, `values-night` overrides, 8-color course palette, layouts for all screens.

## Non-goals

- **Class-start notifications** — deferred to a later change.
- **Actual 教务系统 scraping** — only the importer interface is reserved; no school-specific implementation.
- **Cloud sync / accounts** — v1 is fully local.
- **Jetpack Compose / Kotlin migration** — stays Java + XML Views.
- **iOS / cross-platform** — Android only.
