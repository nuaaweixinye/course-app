## ADDED Requirements

### Requirement: Import from CSV
The system SHALL accept a `.csv` file and map recognized columns (course name, teacher, weekday, periods, location, week pattern) into courses and sessions after a preview/confirm step.

#### Scenario: Tolerant header recognition
- **WHEN** the user imports a CSV whose headers are any of the common synonyms (e.g., "课程/课程名/名称" for name)
- **THEN** the parser maps the columns correctly and produces a preview of detected courses

#### Scenario: Malformed file aborts import
- **WHEN** the selected CSV cannot be parsed (e.g., inconsistent column counts)
- **THEN** the import aborts with a clear error and no data is written

### Requirement: Import from ICS
The system SHALL accept a standard `.ics` (iCalendar) file, interpret VEVENTs with RRULE-based weekly recurrence, and map them to courses/sessions via the period-times table.

#### Scenario: ICS event becomes a session
- **WHEN** the user imports an ICS file containing a weekly VEVENT
- **THEN** a preview shows the event mapped to a weekday and period (derived from its start time vs. period-times)
- **AND** on confirm, it is persisted as a session

### Requirement: Import from XLSX
The system SHALL accept a `.xlsx` file using a lightweight parser; if no suitably small dependency is available, XLSX support SHALL be deferred and documented.

#### Scenario: XLSX supported when dependency fits
- **WHEN** a lightweight XLSX reader is added and the user imports a valid .xlsx
- **THEN** the file is parsed and previewed identically to CSV

#### Scenario: XLSX unsupported is clearly communicated
- **WHEN** XLSX support is not bundled and the user selects a .xlsx file
- **THEN** the system explains XLSX is not supported and suggests converting to CSV/ICS

### Requirement: Preview and confirm
The system SHALL never write imported data without showing a preview and requiring explicit user confirmation.

#### Scenario: Preview before write
- **WHEN** parsing succeeds
- **THEN** a preview screen lists all detected courses/sessions with editable/deselectable rows
- **AND** nothing is written until the user taps confirm

#### Scenario: Confirm writes in a transaction
- **WHEN** the user confirms an import
- **THEN** all selected rows are persisted atomically; if any insert fails, the whole import rolls back

### Requirement: Pluggable importer interface
The system SHALL define a `TimetableImporter` interface that returns parsed courses, so future 教务系统 integrations reuse the preview/confirm flow.

#### Scenario: Future importer reuses preview
- **WHEN** a future school-specific importer implements the interface
- **THEN** its output feeds the same preview and confirm pipeline as file imports
