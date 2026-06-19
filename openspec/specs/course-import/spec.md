# course-import Specification

## Purpose
TBD - created by archiving change course-schedule-app. Update Purpose after archive.
## Requirements
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

### Requirement: Import from incoming intent
The system SHALL accept a file URI delivered via `ACTION_VIEW` or `ACTION_SEND` intent and import it through the same parse → preview → confirm pipeline used by the SAF picker.

#### Scenario: Intent URI parsed identically to SAF URI
- **WHEN** the same file is opened via intent and via the SAF picker
- **THEN** the parsed courses are identical in both cases

### Requirement: Robust URI handling
The system SHALL handle both `content://` and `file://` URI schemes, query display names via `OpenableColumns.DISPLAY_NAME`, and detect file extension for parser routing.

#### Scenario: content:// URI shared from another app
- **WHEN** the intent carries a `content://` URI with `EXTRA_STREAM`
- **THEN** the app opens the input stream, detects the extension (`.xls`, `.html`, `.csv`, `.ics`), and routes to the correct parser

#### Scenario: file:// URI opened from file manager
- **WHEN** the intent carries a `file://` URI
- **THEN** the app reads the file directly and imports it

### Requirement: In-app picker remains as fallback
The system SHALL keep the SAF `ACTION_OPEN_DOCUMENT` picker accessible from within ImportActivity even when intent-based import is available.

#### Scenario: User taps "选择文件" button
- **WHEN** the user taps the "选择文件" button inside ImportActivity
- **THEN** the SAF picker opens as before, allowing file selection regardless of intent support

