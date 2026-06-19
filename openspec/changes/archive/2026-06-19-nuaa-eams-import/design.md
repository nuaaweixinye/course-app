## Context

NUAA runs 金智/beangle教务 (`/eams/`) behind CAS (`aao-eas.nuaa.edu.cn`). CAS login has a captcha, so the app cannot auto-login safely. The timetable page (`courseTableForStd.action`) embeds the schedule as inline JavaScript: each class is a `new TaskActivity(...)` call followed by `index = day*unitCount+period` cell assignments into `table0.activities[index]`. A real captured sample (in this change's folder) confirms the exact format.

Key fields captured from the sample:
- `var unitCount = 13;` — NUAA's day has 13 slots: 节 1-4 (morning), 午一/午二 (lunch, indices 4-5), 节 5-8 (afternoon, indices 6-9), 节 9-11 (evening, indices 10-12).
- `new TaskActivity(teacherId, teacherName, courseId, courseName, courseNameForExp, roomId, roomName, vaildWeeks, taskId, remark, assistantName, experiItemName, schGroupNo, teachClassName)` — args 0,1,10 are JS expressions; the rest are string literals.
- `vaildWeeks` is a ~50-char `'0'/'1'` string; position `i` (0-based) = week `i+1` is occupied.
- `index = D*unitCount+P` — `D` = weekday (0=Mon…6=Sun), `P` = slot index.

## Goals / Non-Goals

**Goals:**
- Log in via WebView (user handles CAS+captcha), no credential storage.
- Fetch the rendered timetable page and parse `TaskActivity` data into courses/sessions.
- Reuse the existing preview/confirm import.

**Non-Goals:** credential storage, captcha OCR, other schools, multi-semester.

## Decisions

### Decision 1: WebView login, not credential-based HTTP
**Choice:** `WebViewLoginActivity` loads the CAS login URL; the user logs in themselves (captcha included). When the WebView reaches an `aao-eas.nuaa.edu.cn/eams/...` URL, the app navigates to `courseTableForStd.action`, then reads the page's HTML via `WebView.evaluateJavascript("document.documentElement.outerHTML")`.
**Why:** CAS + captcha cannot be automated reliably or safely; the WebView lets the user authenticate normally while the app reuses the WebView's cookie jar (WebView shares cookies with the app via `CookieManager`). No password is ever read or stored.
**Alternatives:** capture cookies then HttpURLConnection — rejected (extra step; the WebView already has the page).

### Decision 2: Parse `new TaskActivity(...)` + `index` assignments with regex
**Choice:** Split the captured HTML on `new TaskActivity(`. For each segment:
- `vaildWeeks` = the `^[01]{30,}$` literal.
- Collect `"..."` string literals; `literals[1]` = courseName, `literals[4]` = roomName.
- Teacher = first `name:"..."` from the preceding `actTeachers` array (best-effort).
- Cells = all `index\s*=\s*(\d+)\s*\*\s*unitCount\s*\+\s*(\d+)` matches → (day, slot).
**Why:** The format is consistent and server-rendered into the inline script; a tolerant regex parser is robust to whitespace and handles the JS-expression args (teacherId/teacherName/assistantName) by skipping them.
**Alternatives:** run a JS engine to evaluate the page — rejected (heavy dependency, and the static text is sufficient).

### Decision 3: NUAA slot → app period mapping (skip lunch)
**Choice:** Map slot index → app period: `0-3 → 1-4`, `4-5` (lunch) skipped, `6-9 → 5-8`, `10-12 → 9-11`. So `appPeriod = slot <= 3 ? slot+1 : slot-1` (for non-lunch slots).
**Why:** NUAA has 11 real periods + 2 lunch slots; our app's 12-period grid fits the 11 real ones in order. Lunch slots (4-5) are skipped because real classes aren't scheduled there (confirmed by the sample).
**Day:** `appDay = D + 1` (D=0 Mon → app 1=Mon).

### Decision 4: Group consecutive slots per course into one session
**Choice:** For each course, on each weekday, sort its slot indices, map to app periods, and merge consecutive periods into a single session (`startPeriod`..`endPeriod`). Non-consecutive runs become separate sessions.
**Why:** NUAA schedules classes as 2-3 consecutive periods (e.g. 操作系统 Mon periods 1-2); the app models a session as `startPeriod`/`endPeriod`.

### Decision 5: `vaildWeeks` binary → compressed week pattern
**Choice:** Collect weeks where `vaildWeeks[i] == '1'` (week = i+1) and compress into the app's pattern string (e.g. weeks 3-14 → `"3-14"`, odd weeks → `"1,3,5,..."`).
**Why:** The app stores `weekPattern` as a compressed string parsed by `WeekUtils`; a binary-per-week column would bloat the DB.

## Risks / Trade-offs

- **[Page format changes]** → Mitigation: parser logs how many `TaskActivity` blocks it found; if 0, show a clear "未能解析，请确认已登录并看到课表" error.
- **[WebView cookie not shared / CAS redirect loop]** → Mitigation: enable `CookieManager.getInstance().setAcceptCookie(true)` and `setAcceptThirdPartyCookies(webView, true)`; detect login by URL contains `/eams/`.
- **[Timetable needs `ids`/`semester.id` params]** → Mitigation: load `courseTableForStd.action` first; if it lacks `TaskActivity`, fall back to letting the user navigate to 我的课表 in the WebView then tap a "抓取" button.
- **[Lunch-slot edge case]** → Mitigation: if a slot is 4-5, skip it (no real classes); if encountered, log a warning.

## Migration Plan

None — additive feature; existing CSV/ICS import unchanged.

## Open Questions

- After CAS login, does `courseTableForStd.action` show the current semester directly, or need the user to pick? Default: load it; if no `TaskActivity`, show a "请点开我的课表后再抓取" button.
