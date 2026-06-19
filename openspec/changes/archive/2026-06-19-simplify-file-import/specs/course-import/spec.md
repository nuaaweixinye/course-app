# course-import Specification (Delta)

## ADDED Requirements

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
