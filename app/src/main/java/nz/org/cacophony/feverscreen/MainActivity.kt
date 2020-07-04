package nz.org.cacophony.feverscreen

import android.content.Context
import android.net.nsd.NsdManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.MutableLiveData

const val TAG = "feverscreen"

class MainActivity : AppCompatActivity() {

    private val deviceListAdapter = MutableLiveData<DeviceListAdapter>()
    private lateinit var deviceManager: DeviceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val nsdManager = applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
        val deviceList = DeviceList()
        deviceList.setOnChanged { onDeviceUpdate() }
        deviceListAdapter.value = DeviceListAdapter(deviceList)
        deviceManager = DeviceManager(nsdManager, deviceList, this)
        val recyclerLayoutManager = androidx.recyclerview.widget.LinearLayoutManager(applicationContext)
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.device_recycle_view).apply {
            setHasFixedSize(true)
            layoutManager = recyclerLayoutManager
            adapter = deviceListAdapter.value
        }
    }

    private fun onDeviceUpdate() {
        runOnUiThread {
            deviceListAdapter.value!!.notifyDataSetChanged()
        }
    }

    override fun onPause() {
        deviceManager.tearDown()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        deviceManager.startScan()
    }

    override fun onDestroy() {
        deviceManager.tearDown()
        super.onDestroy()
    }
}