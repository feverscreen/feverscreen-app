package nz.org.cacophony.feverscreen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.URL
import kotlin.concurrent.thread

class Device(
    @Volatile var name: String,
    val hostAddress: String,
    private val port: Int,
    private val activity: Activity) {
    @Volatile
    var sm = StateMachine()

    init {
        Log.i(TAG, "Created new device: $name")
        thread(start = true) {
            for (i in 3.downTo(0)) {
                checkConnectionStatus()
                if (sm.state == DeviceState.CONNECTED) {
                    break
                }
                if (i > 0) {
                    Log.i(TAG, "failed to connect to interface, trying $i more times")
                } else {
                    Log.e(TAG, "failed to connect to interface")
                }
            }
        }
    }

    fun openFeverPage() {
        openPage("/static/html/fever.html")
    }

    fun openHomePage() {
        openPage("/")
    }

    private fun openPage(path: String) {
        val uri = Uri.parse("http://$hostAddress:$port$path")
        Log.i(TAG, "open interface for $name at $uri")
        thread(start = true) {
            if (checkConnectionStatus(timeout = 1000, retries = 1)) {
                val i = Intent(activity, FeverWebView::class.java)
                i.putExtra("uri", uri.toString())
                activity.startActivity(i)
            }
        }
    }

    private fun checkConnectionStatus(timeout: Int = 3000, retries: Int = 3): Boolean {
        var connected = false
        for (i in 1..retries) {
            try {
                val conn = URL("http://$hostAddress").openConnection() as HttpURLConnection
                conn.connectTimeout = timeout
                conn.readTimeout = timeout
                conn.responseCode
                conn.disconnect()
                sm.connected()
                connected = true
                break
            } catch (e: SocketException) {
                Log.i(TAG, "failed to connect to device $name")
                sm.connectionFailed()
            } catch (e: ConnectException) {
                sm.connectionToDeviceOnly()
                Log.i(TAG, "failed to connect to $name interface")
            } catch (e: Exception) {
                Log.e(TAG, "failed connecting to device $name: $e")
                sm.connectionFailed()
            }
            if (i != retries) {
                Thread.sleep(3000)
            }
        }
        return connected
    }
}

class StateMachine {

    var state = DeviceState.FOUND
    private var hasDeviceInfo = false
    private var hasConnected = false

    fun connected() {
        hasConnected = true
        if (!state.connected) {
            if (hasDeviceInfo) {
                updateState(DeviceState.READY)
            } else {
                updateState(DeviceState.CONNECTED)
            }
        }
    }

    fun connectionToDeviceOnly() {
        hasConnected = true
        updateState(DeviceState.ERROR_CONNECTING_TO_INTERFACE)
    }

    fun connectionFailed() {
        updateState(DeviceState.ERROR_CONNECTING_TO_DEVICE)
    }

    private fun updateState(newState: DeviceState) {
        if (state == newState) return
        val validSwitch = when (state) {
            DeviceState.FOUND -> {
                true
            }
            DeviceState.RECONNECT -> {
                true
            }
            DeviceState.CONNECTED -> {
                newState in arrayListOf(
                        DeviceState.READY,
                        DeviceState.ERROR_CONNECTING_TO_INTERFACE,
                        DeviceState.ERROR_CONNECTING_TO_DEVICE,
                        DeviceState.RECONNECT
                )
            }
            DeviceState.READY -> {
                newState in arrayListOf(
                        DeviceState.ERROR_CONNECTING_TO_INTERFACE,
                        DeviceState.ERROR_CONNECTING_TO_DEVICE,
                        DeviceState.RECONNECT
                )
            }
            DeviceState.ERROR_CONNECTING_TO_DEVICE -> {
                newState in arrayListOf(
                        DeviceState.CONNECTED,
                        DeviceState.ERROR_CONNECTING_TO_DEVICE,
                        DeviceState.RECONNECT
                )
            }
            DeviceState.ERROR_CONNECTING_TO_INTERFACE -> {
                newState in arrayListOf(
                        DeviceState.CONNECTED,
                        DeviceState.ERROR_CONNECTING_TO_DEVICE,
                        DeviceState.RECONNECT
                )
            }
        }
        if (validSwitch) {
            state = newState
        }
        if (!validSwitch) {
            Log.e(TAG, "Invalid state switch from $state to $newState")
        }
    }
}

enum class DeviceState(val message: String, val connected: Boolean) {
    FOUND("Found device. Trying to connect", false),
    CONNECTED("Connected.", true),
    RECONNECT("Trying to reconnect", false),
    READY("Got device info.", true),
    ERROR_CONNECTING_TO_DEVICE("Error connecting.", false),
    ERROR_CONNECTING_TO_INTERFACE("Error connecting to interface.", false),
}
