# xDrip+ Personal Fork — Claude Context

## Project Overview
Personal fork of xDrip+ (NightscoutFoundation) for use with Ottai CGM sensor.

Three active tracks:
1. Ottai CGM integration — DONE
2. Nightscout cloud upload — DONE
3. UI redesign to Material Design 3 — IN PROGRESS

---

## Build & Device

```bash
# Build
./gradlew :app:assembleFast

# Install
adb install -r app/build/outputs/apk/fast/debug/app-fast-debug.apk

# Verify screen content (no screenshot needed)
adb shell uiautomator dump && adb shell cat /sdcard/window_dump.xml | grep -o 'text="[^"]*"' | head -30

# Navigate to a screen (example)
adb shell am start -n com.eveningoutpost.dexdrip/.StopSensor
```

Device: Huawei, 1260x2844 resolution, USB ADB
Flavor: `fast` — resConfigs "en", "ru", "xxhdpi"
Package: `com.eveningoutpost.dexdrip`

---

## UI Redesign — Strategy & Rules

Goal: Make all screens look like a modern M3 dark app without breaking any functionality.

Rules (strictly follow):
- Modify existing XML elements in-place — do NOT restructure layouts
- Do NOT add Compose — all previous Compose code was removed
- Preserve all element IDs — Java code references them
- Preserve all data binding expressions (`@{...}`) and `<data>` blocks
- One screen at a time, build after each, ADB-verify before moving on

M3 Palette (defined in `res/values/colors.xml`):
- `md_background` #0F1117 — screen background
- `md_surface` #1A1E26 — cards, drawer
- `md_primary` #4DD9AC — teal accent
- `md_on_surface` #E2E6EF — primary text
- `md_on_surface_variant` #9CA8B8 — secondary text, icons
- `md_error_container` #93000A — destructive button background
- `md_on_error_container` #FFDAD6 — destructive button text

Per-screen checklist:
1. Root background: `android:background="@color/md_background"`
2. Title: `android:textAppearance="@style/TextAppearance.Material3.HeadlineMedium"` + `android:textColor="@color/md_on_surface"`
3. Body text: `android:textAppearance="@style/TextAppearance.Material3.BodyMedium"` + `android:textColor="@color/md_on_surface_variant"`
4. Primary button: replace `<Button>` with `<com.google.android.material.button.MaterialButton app:cornerRadius="20dp"/>` (preserve ID)
5. Outlined secondary button: add `style="@style/Widget.Material3.Button.OutlinedButton"`
6. Destructive button: `style="@style/Widget.Material3.Button.TonalButton"` + `android:backgroundTint="@color/md_error_container"` + `android:textColor="@color/md_on_error_container"`
7. Input fields: `TextInputLayout` (style OutlinedBox, 16dp corner) + `TextInputEditText` (preserve IDs)

---

## Completed Work

### Ottai CGM Integration
- `OttaiAppReceiver.java`: `faux_bgr.put("noise", 1)` — prevents "???" annotation
- `DexCollectionType.Ottai` in `isPassive` set — passive receiver, no BLE scan needed

### Nightscout
- URL: `https://<token>@web-production-7baa4d.up.railway.app/api/v1/` (trailing `/api/v1/` is required — without it NightscoutUploader.java:486 throws)
- Profile: DIA=4h, I:C=10g/U, ISF=2 mmol/L/U, Target=4.0-8.0 mmol/L

### UI Shared Components
- `res/values/themes.xml` + `colors.xml` — M3 dark theme, full palette
- `res/values/styles.xml` — TimeRangeButton style (pill, surface_variant bg)
- `NavDrawerAdapter.java` — Material drawer rows with icon + label
- `NavDrawerBuilder.java` — 17 icons mapped to all drawer items
- `NavigationDrawerFragment.java` — uses NavDrawerAdapter; fixed: setItemChecked must be AFTER setAdapter
- `res/layout/item_nav_drawer.xml` — M3 nav item row layout
- `res/drawable/nav_item_background.xml`, `nav_drawer_background.xml` — M3 backgrounds
- `res/color/nav_item_icon_tint.xml`, `nav_item_text_color.xml` — activated state colors
- 17 vector drawables: `ic_nav_home/graph/table/add/tune/warning/flag/stop/play/bluetooth/pulse/bell/snooze/search/bar_chart/history/settings`
- `res/drawable/time_range_button_background.xml` — surface_variant pill ripple

### UI Screens Redesigned
- Navigation Drawer (fragment_navigation_drawer.xml + NavDrawerAdapter) — DONE
- Home (activity_home.xml) — DONE
- Начальная калибровка (activity_double_calibration.xml) — DONE
- Остановить сенсор (activity_stop_sensor.xml) — DONE (2026-06-17)
- Запустить сенсор (activity_start_new_sensor.xml) — DONE (2026-06-17)
- Искать устройства Bluetooth (activity_bluetooth_scan.xml) — DONE (2026-06-17)
- Состояние системы (activity_mega_status.xml + fragment_mega_status.xml + listitem_megastatus.xml) — DONE (2026-06-17)
- Уровень оповещений (activity_edit_alert.xml) — DONE (2026-06-17)

---

## Screens Remaining (Drawer Order)

Continue from here in the NEXT SESSION. Do these one at a time with build+verify after each:

1. Отложить оповещения — find activity + layout (likely SnoozePicker or similar)
2. Поиск заметок — find activity + layout
3. Статистика — find activity + layout
4. История — BGHistory.java → find layout
5. Настройки — PreferenceFragment hierarchy (largest, many sub-screens)

---

## Known Constraints

- No screenshots in current sessions — hit API image limit; use `uiautomator dump` for verification
- Maestro not installed — could install via `brew install maestro` for YAML text-assertion testing
- Data binding layouts — files with `<layout>` wrapper: preserve `<data>` block, add `xmlns:app` if using `app:` attributes
- Time range buttons — may be hidden by `vs.included[time_buttons]` binding; style applied regardless
- Compose removed — `org.jetbrains.kotlin.plugin.compose` removed from build.gradle; do NOT re-add
