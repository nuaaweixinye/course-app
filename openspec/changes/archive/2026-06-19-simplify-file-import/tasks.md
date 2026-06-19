## 1. Intent handling in ImportActivity

- [x] 1.1 Add intent handling in `ImportActivity.onCreate`: check `getIntent()` for `ACTION_VIEW`/`ACTION_SEND` with a valid URI; if present, call `onFileChosen(uri)` directly instead of waiting for SAF picker
- [x] 1.2 Verify the existing `onFileChosen(Uri)` method handles `content://` and `file://` URIs correctly (reads bytes via `getContentResolver().openInputStream`, plus direct File access for `file://`)

## 2. UI: deprecate WebView import path

- [x] 2.1 Remove `btnEams` from `activity_import.xml` layout (hide or delete the "从教务导入" button)
- [x] 2.2 Remove `btnEams` click handler and `eamsLauncher` from ImportActivity.java (keep `NuaaEamsParser` for file-based .xls/.html parsing)

## 3. Manifest intent-filter

- [x] 3.1 Verify the intent-filter on `ImportActivity` covers `ACTION_VIEW` + `ACTION_SEND` + `content://` + `file://` schemes + relevant mime types (`application/vnd.ms-excel`, `text/html`, `text/csv`, `text/plain`, `application/octet-stream`)

## 4. Verification

- [x] 4.1 Build the app, ensure it compiles without errors
- [x] 4.2 Code verified: `handleIncomingIntent()` correctly routes VIEW/SEND intents to `onFileChosen`; `openInputStream()` handles both `content://` and `file://` URIs; manifest declares `*/*` intent-filter for maximum compatibility
- [x] 4.3 SAF picker opens successfully (verified `com.android.documentsui` launches on tap)
- [x] 4.4 btnEams no longer present in UI (verified: dumps show no `btnEams` element)
