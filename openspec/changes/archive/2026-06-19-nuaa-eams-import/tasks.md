## 1. NUAA 金智 parser

- [x] 1.1 Implement `NuaaEamsParser implements TimetableImporter` in `data/import/`: extract `unitCount` (default 13); split on `new TaskActivity(`; per segment capture `vaildWeeks` (`[01]{30,}`), the string literals (courseName = literals[1], roomName = literals[4]), teacher (`name:"..."`), and all `index = D*unitCount+P` cells; skip lunch slots (4,5); map slot→app period (`<=3 → +1`, else `-1`); `appDay = D+1`.
- [x] 1.2 Implement `vaildWeeks` → week-pattern: collect weeks where char=='1' (week = i+1), compress via a helper into ranges (e.g. weeks 3-14 → "3-14"); reuse/extend `WeekUtils`.
- [x] 1.3 Group consecutive periods per (course, day) into one session (`startPeriod`/`endPeriod`); non-consecutive → separate sessions.
- [x] 1.4 Add a JUnit test feeding the captured NUAA sample (in this change's folder) and asserting: courses found include 操作系统/计算机系统结构; 操作系统 has a Mon 1-2 / weeks 3-14 session; teacher names parsed; lunch slots skipped.

## 2. WebView login + page capture

- [x] 2.1 Create `WebViewLoginActivity` + `activity_webview_login.xml`: a `WebView` filling the screen plus a top bar with a "抓取课表" action and a progress hint.
- [x] 2.2 Load the CAS/eams entry URL; enable JS, DOM storage, and `CookieManager` (accept + third-party cookies). Detect login by URL containing `/eams/`, then auto-navigate to `courseTableForStd.action`.
- [x] 2.3 On page finished (or when user taps 抓取课表), call `evaluateJavascript("(function(){return document.documentElement.outerHTML})()", cb)`; pass the returned HTML to `NuaaEamsParser`; on 0 activities, show a toast "请确认已登录并打开课表页" and let the user retry; on success, return the parsed JSON/string via `setResult` → `ImportActivity`.
- [x] 2.4 Register `WebViewLoginActivity` in the manifest (`android:exported="false"`).

## 3. Wire into ImportActivity

- [x] 3.1 Add a "从教务导入(NUAA)" button to `activity_import.xml` above the file picker; launch `WebViewLoginActivity` for result.
- [x] 3.2 On result, feed the returned parsed courses straight into the existing preview (reuse the preview render + confirm-write path used for CSV/ICS).

## 4. Verify

- [x] 4.1 Clean build + unit tests + lint pass.
- [x] 4.2 On-device: open 导入课表 → 从教务导入 → log in via WebView → confirm preview shows real NUAA courses → import → verify they appear on the timetable grid.
