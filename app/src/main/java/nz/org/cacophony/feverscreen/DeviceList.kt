package nz.org.cacophony.feverscreen

import android.content.Context
import android.util.Log

class DeviceList {
    private val _connectedDevices = sortedMapOf<String, Device>()
    private val allDevices = sortedMapOf<String, Device>()
    private var onChanged: (() -> Unit)? = null
    private val connectedDevices
        get() = _connectedDevices
            .filter { (_,device) -> device.isFavourite || device.connectionInterface == "usb" || !filterDevices }
            .ifEmpty { _connectedDevices }

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
        val hadItems = _connectedDevices.isNotEmpty()
        _connectedDevices.clear()
        allDevices.clear()
        if (hadItems) notifyChange()
    }

    @Synchronized
    fun elementAt(i: Int): Device {
        return connectedDevices.values.elementAt(i)
    }

    @Synchronized
    fun size(): Int {
        return connectedDevices.values.size
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
            _connectedDevices[deviceHostAddress] = allDevices[deviceHostAddress]
            notifyChange()
        } else if (!connected) {
            _connectedDevices.remove(deviceHostAddress)
            notifyChange()
        }
    }
}

