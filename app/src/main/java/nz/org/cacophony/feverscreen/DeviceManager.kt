package nz.org.cacophony.feverscreen

import android.app.Activity
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import java.net.*

const val SERVICE_TYPE = "_cacophonator-management._tcp"
class DeviceManager(
        private val nsdManager: NsdManager,
        private val devices: DeviceList,
        private val activity: Activity) {

    fun startScan() {
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun getResolveListener(): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {

            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed. Error code: $errorCode, service: $serviceInfo")
                if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE) {
                    // Wait for a little bit then try again
                    Thread.sleep(200)
                    nsdManager.resolveService(serviceInfo, getResolveListener())
                }
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.i(TAG, "Resolve Succeeded. $serviceInfo")

                val port: Int = serviceInfo.port
                val host: InetAddress = serviceInfo.host
                Log.i(TAG, "found device $host on port $port")

                val newDevice = Device(
                    serviceInfo.serviceName,
                    serviceInfo.host.hostAddress,
                    port,
                    activity)
                devices.add(newDevice)
            }
        }
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {

        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d(TAG, "Service discovery success: $service")
            nsdManager.resolveService(service, getResolveListener())
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            devices.remove(service.host.hostAddress)
            Log.e(TAG, "service lost: $service")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(TAG, "Discovery stopped: $serviceType")

        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }
    }

    fun tearDown() {
        nsdManager.stopServiceDiscovery(discoveryListener)
    }
}
