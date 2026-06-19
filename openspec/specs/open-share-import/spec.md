# open-share-import Specification

## Purpose
Handle files opened from other apps (via Android `ACTION_VIEW` / `ACTION_SEND` intents) so the user can share or open `.xls`/`.html`/`.csv`/`.ics` files from WeChat, file managers, or email directly into the 课表 import flow.

## Requirements

### Requirement: Receive shared files via intent
The system SHALL accept incoming `ACTION_VIEW` and `ACTION_SEND` intents that carry a file URI, detect the file type by extension, and route it through the existing import pipeline (parse → preview → confirm).

#### Scenario: File shared from WeChat
- **WHEN** the user opens a `.xls` timetable file in WeChat and taps "用其他应用打开" → selects 课表
- **THEN** the app launches ImportActivity, reads the file, parses it with `NuaaEamsParser`, and shows a preview

#### Scenario: File opened from file manager
- **WHEN** the user navigates to a `.html` timetable file in the Files app and taps to open it → selects 课表
- **THEN** the app imports the file identically to the SAF picker flow

#### Scenario: Non-parseable file returns a clear error
- **WHEN** a file with an unrecognized extension is delivered via intent
- **THEN** the app shows an error message and does not crash

### Requirement: Intent-filter registration
The system SHALL declare intent-filters on ImportActivity so the OS offers the app as a target when opening `.xls`, `.html`, `.csv`, `.ics` files, covering `content://` and `file://` URI schemes.

#### Scenario: Intent-filter covers share
- **WHEN** another app sends `ACTION_SEND` with `EXTRA_STREAM` pointing to a `content://` URI of type `application/vnd.ms-excel`
- **THEN** the system lists 课表 in the share sheet

#### Scenario: Intent-filter covers open
- **WHEN** a file manager fires `ACTION_VIEW` with a `file://` URI ending in `.html`
- **THEN** the system lists 课表 in the "open with" dialog

### Requirement: No data loss on misrouted intents
The system SHALL gracefully handle intents that arrive without a valid file URI (e.g., the user navigates directly to ImportActivity from the app's own navigation without an incoming file).

#### Scenario: Normal app launch shows picker
- **WHEN** the user taps "导入课表" from the bottom nav and no incoming intent exists
- **THEN** ImportActivity shows the SAF file picker button as before, unchanged
