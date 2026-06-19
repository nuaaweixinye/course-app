## Why

The in-app file picker (SAF `ACTION_OPEN_DOCUMENT`) on Huawei devices defaults to a confusing "recent" view, making it hard for users to navigate to their `.xls` file. Users want to either browse the filesystem freely or share a file directly from WeChat/file manager into the app. The direct EAMS login (WebView) path is unreliable and should be de-emphasized in favor of a reliable file-import flow.

## What Changes

- Add open/share intent-filters so the app appears as a target when opening `.xls`/`.html` files from any file manager or sharing from WeChat
- Replace the SAF picker with a permissive file browser experience (SAF with initial directory hint, or explain picker navigation)
- Keep the existing preview/confirm pipeline unchanged
- Deprecate the WebView login import path (keep code, hide from UI or move to secondary position)
- Ensure files from content:// and file:// schemes are both handled

## Capabilities

### New Capabilities
- `open-share-import`: Handle Android `ACTION_VIEW` / `ACTION_SEND` intents for `.xls`, `.html`, `.csv`, `.ics` files, so the app receives files opened from file managers or shared from other apps

### Modified Capabilities
- `course-import`: Enhance the import flow to accept incoming intents (VIEW/SEND) in addition to the SAF picker; support `content://` and `file://` URI schemes
- `eams-import`: Deprecate the WebView login path — keep the parser (`NuaaEamsParser`) but hide the "从教务导入" button or move it behind a secondary menu entry

## Impact

- `AndroidManifest.xml`: add intent-filter on ImportActivity for VIEW/SEND actions
- `ImportActivity.java`: handle incoming intents in `onCreate`, route to existing import pipeline
- `MainActivity.java` (bottom nav): hide or re-label the "从教务导入" button
- `WebViewLoginActivity.java`: remains but no longer prominently exposed
- SA: no new dependencies, no API changes, no DAO/model changes
