package com.eveningoutpost.dexdrip.cgm.ottai

/**
 * Parses raw BLE notification bytes from the Ottai sensor.
 *
 * Packet layout is a placeholder — update after BLE reverse engineering.
 * Typical CGM BLE packet contains: glucose value, timestamp, status flags.
 */
internal data class OttaiPacket(
    val glucoseMgDl: Int,
    val sensorStatus: SensorStatus,
    val timestampMs: Long,
    val rawValue: Int,
) {
    enum class SensorStatus { OK, WARMING_UP, SIGNAL_LOSS, ERROR }

    val isValid: Boolean
        get() = sensorStatus == SensorStatus.OK && glucoseMgDl in 40..400

    companion object {
        // TODO: update offsets/masks after capturing real BLE traffic
        fun parse(bytes: ByteArray): OttaiPacket? {
            if (bytes.size < 4) return null
            return try {
                val glucose = ((bytes[1].toInt() and 0xFF) shl 8) or (bytes[0].toInt() and 0xFF)
                val status = when (bytes[2].toInt() and 0x0F) {
                    0x00 -> SensorStatus.OK
                    0x01 -> SensorStatus.WARMING_UP
                    0x02 -> SensorStatus.SIGNAL_LOSS
                    else -> SensorStatus.ERROR
                }
                val raw = ((bytes[3].toInt() and 0xFF) shl 8) or (bytes[2].toInt() and 0xFF shr 4)
                OttaiPacket(
                    glucoseMgDl = glucose,
                    sensorStatus = status,
                    timestampMs = System.currentTimeMillis(),
                    rawValue = raw,
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
