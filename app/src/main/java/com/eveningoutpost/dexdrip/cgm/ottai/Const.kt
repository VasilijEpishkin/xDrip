package com.eveningoutpost.dexdrip.cgm.ottai

import java.util.UUID

/**
 * Ottai CGM BLE constants.
 *
 * UUIDs below are placeholders — obtain real values by sniffing BLE traffic
 * from the official Ottai app (use nRF Sniffer or Wireshark with BLE adapter).
 *
 * How to capture:
 *   1. Enable BLE HCI logging on Android: developer options → "Enable Bluetooth HCI snoop log"
 *   2. Connect to Ottai sensor with official app
 *   3. Pull log: adb pull /sdcard/btsnoop_hci.log
 *   4. Open in Wireshark, filter: btatt
 *   5. Replace UUIDs below with actual values
 */
internal object Const {

    // TODO: replace with real manufacturer ID from Ottai BLE advertisement
    const val MANUFACTURER_ID = 0x0000

    // TODO: replace with real service UUID
    val SERVICE_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

    // TODO: replace with real characteristic UUIDs
    val GLUCOSE_DATA_CHAR: UUID = UUID.fromString("00000001-0000-0000-0000-000000000000")
    val WRITE_CHAR: UUID = UUID.fromString("00000002-0000-0000-0000-000000000000")
    val NOTIFY_CHAR: UUID = UUID.fromString("00000003-0000-0000-0000-000000000000")

    val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-0000-0000-000000000000")

    // BLE scan settings
    const val SCAN_TIMEOUT_MS = 30_000L
    const val RECONNECT_DELAY_MS = 5_000L
    const val READING_INTERVAL_MS = 300_000L // 5 min
}
