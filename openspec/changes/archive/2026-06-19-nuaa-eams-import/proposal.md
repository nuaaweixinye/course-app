## Why

The app currently imports schedules only from CSV/ICS files. The user (NUAA) wants to import directly from the school's 金智(beangle)教务系统 at `aao-eas.nuaa.edu.cn`, which uses CAS single-sign-on (with captcha). Manual CSV export is tedious. This change adds an in-app "从教务导入" path: the user logs in via a WebView (handling CAS + captcha themselves — no credentials stored), the app captures the rendered timetable page, parses the embedded `TaskActivity` data, and feeds the existing preview/confirm import flow.

## What Changes

- Add a `WebViewLoginActivity` that loads the CAS login page; after the user logs in, it navigates to `courseTableForStd.action` and captures the page HTML (via `evaluateJavascript`).
- Add a `NuaaEamsParser` implementing `TimetableImporter` that extracts `new TaskActivity(...)` calls + their `index = day*unitCount+period` assignments, decodes the 50-char `vaildWeeks` binary string into a week pattern, maps the 13-slot NUAA day layout (skipping 午休 slots) to app periods, and groups consecutive periods per course into sessions.
- Add a "从教务导入" entry that launches the WebView flow; on success, the parsed courses go through the existing `ImportActivity` preview/confirm pipeline.
- No new gradle dependencies (WebView + HttpURLConnection are framework APIs).

## Capabilities

### New Capabilities
- `eams-import`: CAS-WebView-login + 金智/NUAA timetable parsing → preview → import.

### Modified Capabilities
- `course-import`: the import screen gains a "从教务导入" launcher that runs the WebView flow and feeds its result into the existing preview/confirm.

## Impact

- **New code**: `WebViewLoginActivity` + layout; `NuaaEamsParser` (in `data/import/`); wiring in `ImportActivity` (a button) and manifest registration.
- **No schema/dependency changes**.
- **Privacy/security**: the app never sees or stores the user's CAS password — login happens in the WebView controlled by the system/browser; only the resulting session cookies are used in-process to fetch the timetable page.

## Non-goals

- Storing credentials, auto-filling login, or OCR-ing the captcha.
- Supporting schools other than NUAA (the parser is NUAA-金智 specific; the `TimetableImporter` interface remains for future schools).
- Importing anything beyond the current-semester timetable.
