## 1. Fix blank course names

- [x] 1.1 Add temporary logging at three points — `CourseSessionDao.listAllWithCourse()`/`observeAllWithCourse()` result, `CourseRepository.combine()` (log `courseName` per session), and `CourseCardView.bind()` (log `session.courseName`) — to identify which hop drops the name. Build, run, add a course on-device, capture logcat.
- [x] 1.2 Fix the guilty hop. Prime suspect: the `SessionWithCourse` POJO↔column mapping for `c.name AS courseName`. If the column doesn't map, add an explicit alias/`@Embedded(CourseEntity)` or rename the field to match. If instead `course.name` isn't persisted, fix `CourseEditViewModel.save`.
- [x] 1.3 Verify on-device: add a course → grid card shows the name and location; edit the name → card updates. Remove the temporary logging.

## 2. Separate top bar / content / bottom nav

- [x] 2.1 Rewrite `activity_main.xml` root as a vertical `LinearLayout`: `[AppBarLayout, wrap_height]` / `[FrameLayout @id/gridContainer, 0dp + weight=1]` / `[BottomNavigationView, wrap_height]`. Move the toolbar + week switcher into the `AppBarLayout`.
- [x] 2.2 Re-parent the FAB column into the content `FrameLayout` with `layout_gravity="bottom|end"` and a bottom margin that clears the bottom nav (e.g., 16dp above content bottom); keep `@id/fabAdd`, `@id/fabMenu`, `@id/fabImport`, `@id/fabAddCourse`, labels.
- [x] 2.3 Wrap the grid (or `gridContainer`) in a `ScrollView` so the 12-period grid scrolls when taller than the content band; verify cards aren't clipped.
- [x] 2.4 Verify on-device (via `uiautomator` bounds) that the FAB and its expanded menu sit entirely above the bottom nav with a gap, in portrait.

## 3. Editable week input

- [x] 3.1 Replace the `weekSwitcher` row: small `ImageButton` ‹ / `EditText @id/etWeek` (inputType number, maxLength 2, width ~80dp, IME action done) / small `ImageButton` ›.
- [x] 3.2 In `MainActivity`, wire the `EditText`: on `IME_ACTION_DONE` and `OnFocusChangeListener`, parse the int, clamp to `[1, totalWeeks]`, call `viewModel.setSelectedWeek(n)`, and write the clamped value back. Sync the field whenever `getWeekLabel()`/selected week changes.
- [x] 3.3 Add `MainViewModel.setSelectedWeek(int)` (clamp + `selectedWeek.setValue`) if not present; keep `shiftWeek` for ‹ ›.
- [x] 3.4 Verify on-device: type "5" → grid shows week 5; type "99" → clamps to total; ‹ › still nudge.

## 4. Working home-screen widget

- [x] 4.1 Verify the widget is registered: confirm the `<receiver>` (`APPWIDGET_UPDATE` + `@xml/today_widget_info`) and `BIND_REMOTEVIEWS` `<service>` are in the manifest. Run `adb shell dumpsys appwidget` to confirm the provider is known to the system.
- [x] 4.2 Confirm `widget_today.xml` and `widget_item.xml` use only RemoteViews-allowed view types; fix any that don't. Ensure `widget_item` shows course name + period + colored bar.
- [x] 4.3 Place the widget on the home screen on-device; capture `uiautomator` to confirm it lists today's courses with names (depends on task 1.2 for the name fix flowing into `TodayWidgetFactory`), and shows the empty-state when none.
- [x] 4.4 Verify refresh: add/delete a course, return to the home screen, and confirm the widget updates without relaunch. If it doesn't, confirm `TodayWidgetProvider.refresh()` is called from the save/delete/import paths.

## 5. Final verification

- [x] 5.1 Clean build + unit tests + lint pass (`./gradlew clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`).
- [x] 5.2 On-device smoke test: add course (name shows) → type a week → expand FAB → check widget renders today + opens app → no overlap between bars.
