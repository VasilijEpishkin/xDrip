package com.eveningoutpost.dexdrip.cgm.ottai

import com.eveningoutpost.dexdrip.models.UserError
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore

/**
 * Ottai domain class — persistent state and device identity.
 */
object Ottai {

    private const val TAG = "Ottai"
    private const val PREF_DEVICE_ADDRESS = "ottai-device-address"
    private const val PREF_LAST_READING_TS = "ottai-last-reading-ts"

    var deviceAddress: String?
        get() = PersistentStore.getString(PREF_DEVICE_ADDRESS).takeIf { it.isNotEmpty() }
        set(value) = PersistentStore.setString(PREF_DEVICE_ADDRESS, value ?: "")

    var lastReadingTimestamp: Long
        get() = PersistentStore.getLong(PREF_LAST_READING_TS)
        set(value) = PersistentStore.setLong(PREF_LAST_READING_TS, value)

    fun isDevicePaired(): Boolean = deviceAddress != null

    internal fun onPacketReceived(packet: OttaiPacket) {
        if (!packet.isValid) {
            UserError.Log.w(TAG, "Invalid packet: status=${packet.sensorStatus}, glucose=${packet.glucoseMgDl}")
            return
        }
        UserError.Log.d(TAG, "Glucose=${packet.glucoseMgDl} mg/dL, raw=${packet.rawValue}")
        lastReadingTimestamp = packet.timestampMs
    }
}
