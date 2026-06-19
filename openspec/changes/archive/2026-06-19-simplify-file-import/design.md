## Context

The app currently uses the SAF `ACTION_OPEN_DOCUMENT` picker for file import. On some devices (Huawei) the picker defaults to a confusing "recent" view. Users want to either browse their filesystem freely or share/open files directly from WeChat/file managers without going through the in-app picker.

The existing `ImportActivity` already has a robust parser pipeline: detect file type by extension → read bytes (with GBK charset detection) → parse via NuaaEamsParser / CsvScheduleParser / IcsScheduleParser → preview → confirm. The missing piece is receiving files from outside the app.

## Goals / Non-Goals

**Goals:**
- ImportActivity can receive files via Android `ACTION_VIEW` and `ACTION_SEND` intents
- The app appears in the "share sheet" / "open with" dialog for `.xls`, `.html`, `.csv`, `.ics` files
- The SAF picker remains as a fallback (accessible from within the app)
- The in-app "从教务导入" button is de-emphasized (WebView path remains for power users)

**Non-Goals:**
- No changes to the parser pipeline, preview/confirm flow, or persistence layer
- No changes to Room entities, repositories, or ViewModels
- No Kotlin conversion or Compose migration

## Decisions

1. **Intent-filters on ImportActivity instead of a separate receiver activity** — simpler, single entry point. When the app receives a VIEW/SEND intent, `onCreate` routes the URI through the same `onFileChosen()` path as the SAF picker.

2. **Accept broad mime types** — `.xls` files from Chinese edu systems often arrive as `application/octet-stream` or `text/html`. We list `*/*` and rely on extension detection in `onFileChosen`. This is the same approach used by the SAF picker.

3. **Reuse existing `onFileChosen(Uri)`** — that method already handles: name detection, extension-based parser routing, GBK charset detection, preview, and confirm. Incoming intents drop into the identical pipeline.

4. **URI permissions** — `content://` URIs from share intents usually grant read access. `file://` URIs are accessible directly. No `takePersistableUriPermission` needed since the user grants temporary access per open/share.

5. **Hide "从教务导入" behind the file chooser** — remove the separate `btnEams` button; the EAMS/WebView path is only for advanced users who know to use it.

## Risks / Trade-offs

- `*/*` mime filter makes the app appear for many unrelated file types — accept this as a minor nuisance since `onFileChosen` gracefully rejects non-parseable files with a clear error. Trade-off for reliable file reception.
- `file://` URIs may not work on Android 10+ scoped storage — primarily relying on `content://` which covers share-intent and SAF flows. On actual devices, WeChat and file managers both deliver `content://` URIs.
