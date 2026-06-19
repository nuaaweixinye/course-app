# eams-import Specification

## Purpose
TBD - created by archiving change nuaa-eams-import. Update Purpose after archive.
## Requirements
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


