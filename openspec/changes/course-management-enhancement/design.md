## Context

The app currently stores a single semester as a singleton row in `semester_config` (id=1). `CourseEntity` and `CourseSessionEntity` have no semester or profile linkage, so all courses live in one flat namespace. The `CourseDetailActivity` shows a basic read-only list of sessions with a button to navigate to the edit activity.

Users need to: manage multiple semesters (archive old ones, prepare next semester), bulk-adjust schedules, view richer course details, and maintain multiple timetable views within a semester.

## Goals / Non-Goals

**Goals:**
- Multiple semesters: each with name, start date, week count; courses scoped to a semester
- Semester picker on the main screen to switch active semester
- Batch selection and operations (delete, move to day, cancel for week, edit teacher/location/color)
- Enhanced detail page: session list with week-pattern bar, exception history, inline edit, notes
- Timetable profiles: named course subsets within a semester, toggleable from the main grid

**Non-Goals:**
- No cloud sync or cross-device semester transfer
- No semester auto-detect from academic calendar data
- No undo history beyond the one-step delete confirmation

## Decisions

1. **Schema: add `semesterId` to `CourseEntity`** — simplest scoping. `CourseSessionEntity` inherits via FK relationship. A new `semester` table replaces the singleton `semester_config`. Room migration adds columns with a default value (migrate existing data to "默认学期").

2. **Profile as a tag on sessions** — `timetableProfiles` is a comma-separated tag field on `CourseEntity` (e.g., "full,electives,review"). Profiles are toggled via chips on the main screen. Simple, no join table needed.

3. **Batch mode: long-press on grid enters selection mode** — same UX as Android's multi-select in lists. Selected cards show a check overlay; a bottom action bar appears with batch operations. Cancel with back.

4. **Detail page: inline editing sections** — the detail page becomes a scrollable form with editable fields (name, teacher, note, color) and an editable session list with inline week-pattern picker. No separate edit activity navigation needed for common operations.

5. **Room migration via `fallbackToDestructiveMigration` is NOT acceptable** — use a manual migration with `Migration(5, 6)` that creates the new `semesters` table, inserts a default row, adds `semesterId` to `courses` with a default value.

## Risks / Trade-offs

- Comma-separated profile tags lack referential integrity → mitigated by simple parsing and validation at write time
- Batch multi-select on grid may conflict with existing tap-to-detail behavior → mitigated by explicit mode toggle (long-press enters mode, regular tap selects/deselects in mode)
- Schema migration failure could lose data → mitigated by testing migration on a copy of the real database
