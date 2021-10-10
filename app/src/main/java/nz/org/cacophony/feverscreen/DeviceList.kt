package nz.org.cacophony.feverscreen

import android.content.Context
import android.util.Log

class DeviceList {
    private val connectedDevices = sortedMapOf<String, Device>()
    private val allDevices = sortedMapOf<String, Device>()
    private var onChanged: (() -> Unit)? = null
    var filterDevices = true

    @Synchronized
    fun add(d: Device) {
        Log.i(TAG, "adding new device")
        allDevices[d.hostAddress] = d
        setDeviceConnected(d.hostAddress, d.sm.state == DeviceState.CONNECTED)
        notifyChange()
    }

    @Synchronized
    fun clear() {
        val hadItems = connectedDevices.size > 0
        connectedDevices.clear()
        allDevices.clear()
        if (hadItems) notifyChange()
    }

    @Synchronized
    fun elementAt(i: Int): Device {
        return connectedDevices.values.elementAt(i)
    }

    @Synchronized
    fun size(): Int {
        return getConnectedMap().size
    }

    @Synchronized
    fun setOnChanged(onChanged: (() -> Unit)?) {
        this.onChanged = onChanged
    }

    private fun notifyChange() {
        onChanged?.invoke()
    }

    @Synchronized
    fun getConnectedMap(): Map<String, Device> {
        if (filterDevices) {
            val devices = connectedDevices.filter { (_,device) -> device.isFavourite || device.connectionInterface == "usb" }
            if (devices.isNotEmpty()) {
                return devices
            }
        }
        return connectedDevices
    }

    @Synchronized
    fun getAllMap(): Map<String, Device> {
        return allDevices
    }

    fun setDeviceConnected(deviceHostAddress: String, connected: Boolean) {
        if (!allDevices.containsKey(deviceHostAddress)) {
            Log.e(TAG, "trying to set device connected '$deviceHostAddress' that has not been added yet")
            return
        }

        if (connected) {
            connectedDevices[deviceHostAddress] = allDevices[deviceHostAddress]
            notifyChange()
        } else if (!connected) {
            connectedDevices.remove(deviceHostAddress)
            notifyChange()
        }
    }
}

