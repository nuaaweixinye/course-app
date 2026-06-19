## Context

Greenfield Android project (`com.courseshedule`): Java 11, Material Components 1.13, minSdk 31. Currently an empty shell — no Activity, no database, ViewBinding disabled. This change builds the full 课表 (course schedule) app end-to-end.

The user-validated visual + interaction design lives in `docs/superpowers/specs/2026-06-18-course-schedule-app-design.md` (the brainstorming output). This document records the technical decisions; that document records the product decisions. When they disagree on product behavior, the brainstorming doc wins.

## Goals / Non-Goals

**Goals:**
- Establish a maintainable MVVM + Room foundation that survives feature growth (especially the v2 教务系统 importer).
- Render a correct weekly grid for the real-world complexity of Chinese university schedules: multi-session courses, 单/双周, 调休, 停课.
- Keep the APK dependency footprint small (no DI framework, no Compose, no heavy XLSX lib unless justified).
- Make every screen work in both light and dark themes.

**Non-Goals:**
- Notifications, cloud sync, accounts, actual 教务 scraping, Compose migration (see proposal Non-goals).
- Material You dynamic color — fixed palette for brand consistency; can be added later as a setting.

## Decisions

### Decision 1: Multi-Activity MVVM over Single-Activity + Fragments
**Choice:** One Activity per screen (6 total), each with its own ViewModel where state is needed.
**Why:** The app has ~6 screens with little shared UI state. Multi-Activity keeps each screen's lifecycle self-contained, simplifies widget deep-linking (Intent → Activity), and lowers the learning curve for a Java-only project.
**Alternatives considered:**
- *Single-Activity + Navigation Component*: cleaner shared state and transitions, but adds Navigation graph boilerplate and fragment lifecycle complexity that buys little at this scale.
- *Multi-Activity without architecture (raw SQLite/Cursor)*: rejected — data model is too complex (5 tables, week patterns, exceptions) for Cursor-driven UI.

### Decision 2: Room with a normalized 5-table schema
**Choice:** Tables: `Course`, `CourseSession`, `SessionException`, `Task`, `SemesterConfig` (see brainstorming doc §3 for full columns).
**Why:** A course with multiple weekly slots is the core reality (e.g., 高数 Mon 1-2 + Wed 3-4). Modeling sessions as children of course lets one edit propagate to all slots and keeps color/teacher DRY. `weekPattern` as a compressed string (`"1-16"`, `"1,3,5,7"`) avoids 16 rows per session while staying human-editable.
**Alternatives considered:**
- *Denormalized single table (one row per course-slot-week)*: explodes to hundreds of rows; editing a course means touching many rows.
- *Separate WeekMembership table*: more queryable but heavier; the compressed string + a parsed `Set<Integer>` cache is enough for ≤20 weeks.

### Decision 3: Repository as the only data access surface
**Choice:** UI talks to `*Repository` only; repositories wrap DAOs + import parsers + preference access. ViewModels hold repositories, never DAOs.
**Why:** Gives a single seam to plug the v2 `TimetableImporter` (教务系统) and to swap storage. Keeps ViewModels thin and unit-testable with fake repositories.

### Decision 4: Custom `TimetableView` using absolute positioning
**Choice:** The 7×N grid renders course cards as absolutely-positioned children of a custom `ViewGroup` (via `ConstraintLayout` or a bespoke `AbsoluteLayout`-equivalent). Vertical position = `startPeriod` × rowHeight; height = `(endPeriod − startPeriod + 1) × rowHeight`.
**Why:** RecyclerView's flow model fights cross-period spanning cards. Absolute positioning makes "高数 spans periods 1-2" a simple height calculation and makes week-exception overlays trivial (just hide/move a child).
**Alternatives considered:**
- *RecyclerView grid*: simple for single-period cells, awkward for spanning; would need full-span item decoration.
- *TableView library*: extra dependency, limited customization for the minimalist card style.
**Trade-off:** Initial `onLayout`/`onMeasure` work is non-trivial; mitigated by starting with a fixed row height and deferring dynamic measurement to polish.

### Decision 5: File import order CSV → ICS → XLSX
**Choice:** Implement and ship CSV and ICS parsers first; XLSX last and only if a lightweight reader fits.
**Why:** CSV matches what 教务 systems export and is trivial to parse. ICS gives calendar-interop for free (`.ics` opens in Google Calendar). XLSX needs Apache POI (multi-MB) or a niche lib — defer until value is proven.
**Alternatives considered:**
- *All three up front*: schedule risk; CSV/ICS cover the majority of real import paths.

### Decision 6: Material 3 `DayNight` theme, fixed 8-color course palette
**Choice:** Theme extends `Theme.Material3.DayNight`. App shell (toolbar, FAB, bottom nav, dialogs) uses M3 components with indigo `#4f46e5` accent. Course cards are custom (white/dark surface + 3dp colored left border) using a fixed 8-color palette indexed by `Course.colorTag`.
**Why:** DayNight gives dark mode for free via `values-night`. Fixed palette guarantees consistent per-course color across renders (widget, detail, grid) without dynamic-color nondeterminism.
**Alternatives considered:**
- *Material You dynamic color*: nice on Android 12+ but makes per-course colors harder to keep distinct; revisit as a setting.

### Decision 7: Widget via `RemoteViewsService` + `ListView`
**Choice:** 4×2 widget, `RemoteViews` with a `ListView` backed by `RemoteViewsService.RemoteViewsFactory` querying today's remaining sessions.
**Why:** `RemoteViews` only supports a limited view set; `ListView` + factory is the supported pattern for dynamic content. Data changes trigger `AppWidgetManager.notifyAppWidgetViewDataChanged`.
**Alternatives considered:**
- *Stack of static `TextView`s*: brittle once course count varies.

## Risks / Trade-offs

- **[Custom TimetableView complexity]** → Mitigation: ship a fixed-row-height version first (MVP task), defer dynamic height and cross-period merge polish to the final polish task. Fallback to RecyclerView if layout proves unstable.
- **[XLSX dependency bloat]** → Mitigation: CSV/ICS first; only add an XLSX reader if a sub-500KB lib exists, else document XLSX as "convert to CSV".
- **[Week-pattern string parsing bugs]** → Mitigation: `WeekUtils` is pure Java → heavy unit testing (all/odd/even/custom ranges, out-of-range weeks) before any UI depends on it.
- **[Multi-Activity shared state (e.g., reflecting a new course on return to grid)]** → Mitigation: grid observes Room via LiveData; edits land in DB, grid auto-updates. No manual refresh needed.
- **[Widget update lag after edits]** → Mitigation: repositories call `AppWidgetManager` notify on every mutating operation; widget re-queries fresh data.
- **[SemesterConfig single-row invariant]** → Mitigation: enforce id=1 at the entity/DAO level; seed in `Application.onCreate` if absent.

## Migration Plan

N/A — greenfield. First launch seeds `SemesterConfig` defaults (today as semester start, 16 weeks, default period-times JSON) so the grid renders before the user configures anything.

## Open Questions

- **Exact CSV column convention**: which 教务 export format to target first? Default: a tolerant parser that maps common header names (课程/课程名/名称 → name; 老师/教师 → teacher; 星期/周几 → dayOfWeek; 节次 → periods; 教室 → location; 周次 → weekPattern). Confirmable during the import task.
- **Weekend column collapse threshold**: hide Sat/Sun columns only when both are empty all semester, or always show greyed? Decision deferred to polish task; default = always show, greyed when empty.
