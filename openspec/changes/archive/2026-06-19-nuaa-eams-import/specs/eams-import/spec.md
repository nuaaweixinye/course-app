## ADDED Requirements

### Requirement: CAS login via WebView
The system SHALL let the user log in to the school system inside an in-app WebView, so the user handles CAS authentication (including any captcha) themselves and the app never reads or stores credentials.

#### Scenario: User logs in
- **WHEN** the user opens "从教务导入" and the WebView shows the CAS login page
- **THEN** the user can type their credentials and captcha directly in the WebView
- **AND** the app does not read, store, or transmit the password

#### Scenario: Login success is detected
- **WHEN** after login the WebView reaches a URL under the 教务 (eams) domain
- **THEN** the app proceeds to fetch the timetable page automatically

### Requirement: Parse NUAA 金智 timetable data
The system SHALL extract courses from the rendered 金智 timetable page by parsing the embedded `new TaskActivity(...)` calls and their `index = day*unitCount+period` cell assignments, and decode the `vaildWeeks` binary string into a week pattern.

#### Scenario: A course with a weekly range is parsed
- **WHEN** the captured page contains a `TaskActivity` for "操作系统" with `vaildWeeks` encoding weeks 3-14 and `index` assignments for Monday slots 0 and 1
- **THEN** the parser produces a course "操作系统" with one Monday session, periods 1-2, week pattern "3-14"

#### Scenario: Lunch slots are skipped
- **WHEN** an `index` assignment resolves to a NUAA lunch slot (slot index 4 or 5)
- **THEN** that slot is skipped (no session created for it)

#### Scenario: Teacher is captured when available
- **WHEN** the activity's preceding `actTeachers` array contains a teacher name
- **THEN** the parsed course's teacher field is set to that name

### Requirement: Preview and confirm 教务 import
The system SHALL never write 教务-imported data without showing the same preview/confirm flow used for file imports.

#### Scenario: Preview before write
- **WHEN** the parser finishes
- **THEN** a preview lists the detected courses/sessions (deselectable) and nothing is written until the user confirms

### Requirement: Clear error on parse failure
The system SHALL show a clear, actionable message if the timetable page cannot be parsed.

#### Scenario: No TaskActivity found
- **WHEN** the captured page contains zero `TaskActivity` blocks (e.g. not logged in, or wrong page)
- **THEN** the app shows "未能解析课表，请确认已登录并在课表页面" and writes nothing
