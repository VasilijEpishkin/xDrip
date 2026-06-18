package com.eveningoutpost.dexdrip.cgm.ottai

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import com.eveningoutpost.dexdrip.models.BgReading
import com.eveningoutpost.dexdrip.models.UserError
import com.eveningoutpost.dexdrip.services.JamBaseBluetoothService
import com.eveningoutpost.dexdrip.utils.DexCollectionType
import com.eveningoutpost.dexdrip.xdrip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ottai CGM BLE collection service.
 *
 * Lifecycle: started by CollectionServiceStarter when DexCollectionType == Ottai.
 * Uses coroutines on IO dispatcher for BLE communication;
 * Android BLE callbacks are dispatched to the main thread by the OS.
 *
 * BLE UUIDs are placeholders in Const.kt — replace after reverse-engineering
 * the Ottai app (see Const.kt header for capture instructions).
 */
@SuppressLint("MissingPermission")
class OttaiCollectionService : JamBaseBluetoothService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var gatt: BluetoothGatt? = null
    private var scanJob: Job? = null
    private var reconnectJob: Job? = null

    private var bluetoothAdapter: BluetoothAdapter? = null

    // ---- static state (visible to xDrip status screen) ----
    companion object {
        @Volatile var lastState: String = "Not running"
        @Volatile var lastErrorState: String = ""

        @JvmStatic fun isRunning(): Boolean =
            lastState != "Not running" && !lastState.startsWith("Stop")
    }

    // ------------------------------------------------------------------ lifecycle

    override fun onCreate() {
        super.onCreate()
        service = this
        xdrip.checkAppContext(applicationContext)
        val bm = getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bm?.adapter
        status("Created")
        UserError.Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        xdrip.checkAppContext(applicationContext)
        if (!shouldServiceRun()) {
            UserError.Log.d(TAG, "Should not run — stopping")
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground()
        startScanOrConnect()
        return START_STICKY
    }

    override fun onDestroy() {
        status("Stopped")
        scope.cancel()
        disconnectGatt()
        super.onDestroy()
        UserError.Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?) = null

    // ------------------------------------------------------------------ automata (required by base)

    override fun automata(): Boolean {
        startScanOrConnect()
        return true
    }

    // ------------------------------------------------------------------ BLE scan

    private fun startScanOrConnect() {
        val saved = Ottai.deviceAddress
        if (saved != null && bluetoothAdapter != null) {
            connectToDevice(saved)
        } else {
            startScan()
        }
    }

    private fun startScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: run {
            status("BLE not available")
            return
        }
        status("Scanning…")
        scanJob?.cancel()
        scanJob = scope.launch {
            val filters = listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(Const.SERVICE_UUID))
                    .build()
            )
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            mainHandler.post { scanner.startScan(filters, settings, scanCallback) }

            delay(Const.SCAN_TIMEOUT_MS)

            mainHandler.post { scanner.stopScan(scanCallback) }
            if (Ottai.deviceAddress == null) {
                status("Scan timeout — retry in ${Const.RECONNECT_DELAY_MS / 1000}s")
                scheduleReconnect()
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            UserError.Log.d(TAG, "Found device: ${device.address}")
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(this)
            scanJob?.cancel()
            Ottai.deviceAddress = device.address
            connectToDevice(device.address)
        }

        override fun onScanFailed(errorCode: Int) {
            status("Scan failed: $errorCode")
            scheduleReconnect()
        }
    }

    // ------------------------------------------------------------------ GATT connection

    private fun connectToDevice(address: String) {
        status("Connecting to $address")
        val adapter = bluetoothAdapter ?: return
        val device = try {
            adapter.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            UserError.Log.e(TAG, "Bad address: $address")
            Ottai.deviceAddress = null
            startScan()
            return
        }

        disconnectGatt()

        mainHandler.post {
            gatt = device.connectGatt(applicationContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        }
    }

    private fun disconnectGatt() {
        gatt?.let {
            it.disconnect()
            it.close()
            gatt = null
        }
    }

    // ------------------------------------------------------------------ GATT callbacks

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    this@OttaiCollectionService.status("Connected — discovering services")
                    UserError.Log.d(TAG, "GATT connected, discovering services")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    this@OttaiCollectionService.status("Disconnected")
                    UserError.Log.w(TAG, "GATT disconnected (status=$status)")
                    gatt.close()
                    this@OttaiCollectionService.gatt = null
                    scheduleReconnect()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                UserError.Log.e(TAG, "Service discovery failed: $status")
                scheduleReconnect()
                return
            }
            val service = gatt.getService(Const.SERVICE_UUID) ?: run {
                UserError.Log.e(TAG, "Ottai service UUID not found — check Const.kt")
                lastErrorState = "Service UUID not found"
                scheduleReconnect()
                return
            }
            val notifyChar = service.getCharacteristic(Const.NOTIFY_CHAR) ?: run {
                UserError.Log.e(TAG, "Notify characteristic not found")
                lastErrorState = "Notify char not found"
                scheduleReconnect()
                return
            }
            enableNotifications(gatt, notifyChar)
        }

        @Suppress("DEPRECATION") // characteristic.value deprecated in API 33; kept for minSdk compat
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            val bytes = characteristic.value ?: return
            UserError.Log.d(TAG, "Notification received: ${bytes.size} bytes")
            handleGlucoseData(bytes)
        }

        @Suppress("DEPRECATION") // required for API < 33
        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                this@OttaiCollectionService.status("Listening for glucose data")
                UserError.Log.d(TAG, "Notifications enabled successfully")
            } else {
                UserError.Log.e(TAG, "Descriptor write failed: $status")
                scheduleReconnect()
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    @Suppress("DEPRECATION")
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(Const.CLIENT_CHARACTERISTIC_CONFIG)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        } else {
            UserError.Log.w(TAG, "CCC descriptor not found — notifications may not work")
        }
    }

    private fun handleGlucoseData(bytes: ByteArray) {
        val packet = OttaiPacket.parse(bytes) ?: run {
            UserError.Log.w(TAG, "Failed to parse packet (${bytes.size} bytes)")
            return
        }
        Ottai.onPacketReceived(packet)
        if (packet.isValid) {
            scope.launch(Dispatchers.Main) {
                BgReading.bgReadingInsertMedtrum(
                    packet.glucoseMgDl.toDouble(),
                    packet.timestampMs,
                    "Ottai",
                    packet.rawValue.toDouble(),
                )
            }
            status("Last reading: ${packet.glucoseMgDl} mg/dL")
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(Const.RECONNECT_DELAY_MS)
            startScanOrConnect()
        }
    }

    private fun shouldServiceRun(): Boolean =
        DexCollectionType.getDexCollectionType() == DexCollectionType.Ottai

    private fun status(msg: String) {
        lastState = msg
        UserError.Log.d(TAG, "Status: $msg")
    }
}
