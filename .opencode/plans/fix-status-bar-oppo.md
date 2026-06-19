# Fix: Oppo status bar overlap

**Root cause**: Android 15+ (API 35) forces edge-to-edge mode. App content draws behind the transparent status bar. Oppo ColorOS 15 enforces this; Huawei EMUI (older Android) does not.

**Fix**: Add `android:windowOptOutEdgeToEdgeEnforcement` to the theme.

**File**: `app/src/main/res/values/themes.xml`
**Change**: Add one line after `android:windowLightStatusBar`:

```xml
<item name="android:windowOptOutEdgeToEdgeEnforcement" tools:targetApi="35">true</item>
```

**Effect**: Android 15+ devices (including Oppo ColorOS 15) will not force edge-to-edge. The status bar will draw with `?attr/colorSurface` background, and all content will start below it (same as pre-API-35 behavior). No Java code changes needed; works for all Activities.
