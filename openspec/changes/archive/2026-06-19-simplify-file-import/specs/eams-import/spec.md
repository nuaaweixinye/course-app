# eams-import Specification (Delta)

## REMOVED Requirements

### Requirement: CAS login via WebView
**Reason**: The WebView CAS login path is unreliable (NUAA's portal uses AJAX/pages that resist automation). The file-based .xls import is simpler, faster, and works with 100% reliability. The parser (`NuaaEamsParser`) remains active — it is invoked when a `.xls` or `.html` file is imported.
**Migration**: Use the .xls file import path instead. Export your timetable from the 教务 system as `.xls`, then open/share it into the 课表 app.

### Requirement: Preview and confirm 教务 import
**Reason**: The WebView path is removed, so the preview/confirm for the WebView-specific entry point is no longer needed.
**Migration**: The preview/confirm flow remains for file-based imports (`.xls`, `.html`, `.csv`, `.ics`) via the existing ImportActivity pipeline.

### Requirement: Clear error on parse failure
**Reason**: Replaced by the file import error handling in `course-import`, which shows a clear error when `.xls`/`.html` parsing fails.
**Migration**: Parse errors for `.xls`/`.html` imports already show an error toast and write nothing.
