## Context

The `course-schedule-app` change is deployed on-device. Real-device testing surfaced four home-screen issues. This change fixes them without altering the data model or dependencies. The first change's design lives at `docs/superpowers/specs/2026-06-18-course-schedule-app-design.md` and the affected code at `app/src/main/java/com/courseshedule/`.

A confirmed on-device observation drives item 1: after adding a course, `uiautomator` shows the `CourseCardView` (a `LinearLayout`) rendered at the correct cell (period 1 row, ~y552), **but its name/location `TextView`s are blank** — so the card draws, only the text is missing. The data is in the DB (save completes and returns to the grid without error). The break is therefore in the **read path**: `SessionWithCourse` join → `DisplaySession.courseName` → `CourseCardView.bind`.

## Goals / Non-Goals

**Goals:**
- Course cards display their name and location.
- Top bar, timetable content, and bottom nav are structurally disjoint — no overlap regardless of FAB/menu state.
- Week is directly typeable.
- The home-screen widget appears in the picker and shows real, named courses.

**Non-Goals:** grid/column redesign, new dependencies, notifications/import/tasks, dynamic color.

## Decisions

### Decision 1: Fix the name by instrumenting then patching the read path
**Choice:** Add temporary logging at three points (DAO result, `CourseRepository.combine`, `CourseCardView.bind`) to confirm which hop drops `courseName`; fix the guilty hop. The prime suspect is the `SessionWithCourse` POJO↔column mapping (`c.name AS courseName`).
**Why:** The card renders, so geometry and the LiveData pipeline are correct; only the string is lost. A 3-point log pinpoints it in one run rather than guessing.
**Alternatives:** Blindly rewrite the POJO with `@Embedded` — rejected, could mask a different cause (e.g. name not persisted).
**Fallback:** If the name is genuinely not persisted, also fix `CourseEditViewModel.save`.

### Decision 2: Root layout becomes a vertical LinearLayout for guaranteed separation
**Choice:** Change `activity_main.xml` root from `CoordinatorLayout` to a `LinearLayout` (vertical): `[AppBarLayout, wrap]` / `[FrameLayout @id/gridContainer, 0dp + weight=1]` / `[BottomNavigationView, wrap]`. The FAB column lives inside the content `FrameLayout` with `layout_gravity="bottom|end"` and a bottom margin equal to the nav height, so it can never overlap the nav.
**Why:** Overlap recurred because `CoordinatorLayout` lets children float freely and the FAB straddled the nav. A vertical `LinearLayout` partitions vertical space deterministically — top bar, content, bottom nav each own a disjoint band.
**Alternatives:** Keep `CoordinatorLayout` and tune margins/anchors — rejected, already proved fragile across two attempts.
**Trade-off:** Loses toolbar scroll-away behavior; acceptable (the toolbar is static by design).

### Decision 3: Week switcher → numeric EditText
**Choice:** Replace the `‹ label ›` row with: small `‹` button / `EditText` (number, width ~80dp, IME action done) / small `›` button. On `EditorInfo.IME_ACTION_DONE` (and on focus loss), parse the int, clamp to `[1, totalWeeks]`, call `viewModel.setSelectedWeek(n)`, and update the field. `‹ ›` still nudge ±1 for quick hopping.
**Why:** Typing "3" to reach week 3 beats tapping ‹ repeatedly. Keeping `‹ ›` preserves discoverability.
**Alternatives:** A `NumberPicker` dialog — rejected, more taps to reach a specific week.

### Decision 4: Make the widget appear and render
**Choice:** Verify the `<receiver>` (with `APPWIDGET_UPDATE` + `@xml/today_widget_info` meta-data) and the `BIND_REMOTEVIEWS` `<service>` are present in the manifest (they are, from change 1). Then ensure: (a) `widget_today.xml`/`widget_item.xml` only use `RemoteViews`-allowed views; (b) `TodayWidgetFactory` returns `getCount()` > 0 and `getViewAt` sets the name from `DisplaySession.courseName` (this depends on Decision 1's fix — names must flow for the widget too); (c) a preview is visible in the picker.
**Why:** The widget compiled but was never confirmed on-device. Most "widget missing" reports come from a bad provider meta-data or an unsupported view in the layout.
**Alternatives:** Rebuild the widget from scratch — rejected; the structure is correct, only verification + the name fix are needed.

## Risks / Trade-offs

- **[Name bug root cause uncertain]** → Mitigation: 3-point logging first; do not refactor blind. If the fix touches the Room POJO, re-run unit + instrumented tests.
- **[LinearLayout root changes scrolling]** → The timetable may exceed content height; Mitigation: wrap `gridContainer` in a `ScrollView` if the 12-period grid is taller than the band (likely). Add this as a task step.
- **[Widget still missing after fixes]** → Mitigation: task includes pulling `adb shell dumpsys appwidget` to confirm registration and checking logcat for `RemoteViews` errors.

## Migration Plan

None — pure UI/layout/widget fixes; no data migration. Existing courses persist and will display correctly once Decision 1 lands.

## Open Questions

- Should the week input also accept pressing Enter on a hardware keyboard to jump? Default: yes (`IME_ACTION_DONE`).
- Widget size: keep 4×2, or also offer 2×2? Default: keep 4×2 only (YAGNI).
