# xDrip+ — Personal Fork

Personal fork of [xDrip+](https://github.com/NightscoutFoundation/xDrip) (by NightscoutFoundation).

---

## Changes from upstream

### Ottai CGM Support
Added support for the Ottai CGM sensor as a passive data source:
- `OttaiAppReceiver` receives glucose broadcasts from the Ottai companion app
- No BLE scanning required — works as a passive receiver
- Noise suppression to prevent `???` annotation on ambiguous readings

### UI — Material Design 3
Redesigned screens to a consistent M3 dark theme:
- Custom dark palette (`#0F1117` background, `#4DD9AC` teal accent)
- Navigation drawer overlays from the top of the screen (full-height)
- M3 nav drawer rows with vector icons for all sections
- Screens redesigned: Home, Calibration, Stop/Start Sensor, Bluetooth Scan, System Status, Alert Level, Snooze, Statistics

### Build
- Updated to compileSdk/targetSdk 35, Java 17
- Added Russian locale to `fast` flavor (`resConfigs "en", "ru", "xxhdpi"`)

---

## Based on

[NightscoutFoundation/xDrip](https://github.com/NightscoutFoundation/xDrip)
