# xDrip+ — Personal Fork

Personal fork of [xDrip+](https://github.com/NightscoutFoundation/xDrip) (by NightscoutFoundation), customized for use with the **Ottai CGM sensor** and redesigned with **Material Design 3**.

---

## What's different from upstream

### ✅ Ottai CGM Integration
Full support for the Ottai CGM sensor as a passive data source:
- `OttaiAppReceiver` — receives glucose broadcasts from the Ottai companion app
- `OttaiCollectionService` — Kotlin-based collection service
- Noise suppression: `faux_bgr.put("noise", 1)` prevents `???` annotation
- Registered as a passive receiver — no BLE scanning needed
- Ottai icon added to the sensor list

### ✅ Nightscout Cloud Upload
Configured and tested with a Railway-hosted Nightscout instance:
- URL format: `https://<token>@<host>/api/v1/` (trailing `/api/v1/` required)
- Profile: DIA=4h, I:C=10g/U, ISF=2 mmol/L/U, Target=4.0–8.0 mmol/L

### 🎨 UI Redesign — Material Design 3
Full dark theme redesign using the M3 palette. All screens use a custom Toolbar with a navigation drawer that overlays from the top of the screen.

**M3 Color Palette:**
| Token | Hex | Role |
|---|---|---|
| `md_background` | `#0F1117` | Screen background |
| `md_surface` | `#1A1E26` | Cards, drawer |
| `md_primary` | `#4DD9AC` | Teal accent, active elements |
| `md_on_surface` | `#E2E6EF` | Primary text |
| `md_on_surface_variant` | `#9CA8B8` | Secondary text, icons |
| `md_error_container` | `#93000A` | Destructive button background |

**Screens redesigned:**
- Navigation Drawer — M3 rows with icons, overlay from screen top
- Home (`activity_home.xml`) — chart background matches theme
- Initial Calibration (`activity_double_calibration.xml`)
- Stop Sensor (`activity_stop_sensor.xml`)
- Start Sensor (`activity_start_new_sensor.xml`)
- Bluetooth Scan (`activity_bluetooth_scan.xml`)
- System Status (`activity_mega_status.xml` + `activity_system_status.xml`)
- Alert Level (`activity_edit_alert.xml`)
- Snooze Alerts (`activity_snooze.xml`)
- Statistics (`activity_statistics.xml` + `stats_general.xml`)

**Shared infrastructure:**
- `themes.xml` + `colors.xml` — M3 dark theme, full palette
- `ActivityWithMenu` — auto-sets custom Toolbar as SupportActionBar
- `NavigationDrawerFragment` — 5-arg `ActionBarDrawerToggle` + `topMargin=0` fix for full-screen drawer overlay
- `NavDrawerAdapter` + 17 vector icons for all drawer items

---

## Screens remaining (planned)

- [ ] Search Notes
- [ ] History (`BGHistory`)
- [ ] Settings (PreferenceFragment hierarchy)

---

## Build

```bash
# Build
./gradlew :app:assembleFast

# Install
adb install -r app/build/outputs/apk/fast/debug/app-fast-debug.apk
```

**Flavor:** `fast` — `resConfigs "en", "ru", "xxhdpi"`  
**Target device:** Android, USB ADB  
**Package:** `com.eveningoutpost.dexdrip`

---

## Based on

[NightscoutFoundation/xDrip](https://github.com/NightscoutFoundation/xDrip) — the upstream project. All original functionality is preserved; this fork adds Ottai support and visual improvements only.
