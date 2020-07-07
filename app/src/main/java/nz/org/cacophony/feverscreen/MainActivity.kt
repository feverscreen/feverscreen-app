package nz.org.cacophony.feverscreen

import android.content.Context
import android.net.nsd.NsdManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import kotlin.concurrent.thread

const val TAG = "feverscreen"

class MainActivity : AppCompatActivity() {

    private val deviceListAdapter = MutableLiveData<DeviceListAdapter>()
    private lateinit var deviceManager: DeviceManager
    private lateinit var deviceList: DeviceList

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val nsdManager = applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
        deviceList = DeviceList()
        deviceList.setOnChanged { onDeviceUpdate() }
        deviceListAdapter.value = DeviceListAdapter(deviceList)
        deviceManager = DeviceManager(nsdManager, deviceList, this)
        val recyclerLayoutManager = androidx.recyclerview.widget.LinearLayoutManager(applicationContext)
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.device_recycle_view).apply {
            setHasFixedSize(true)
            layoutManager = recyclerLayoutManager
            adapter = deviceListAdapter.value
        }
        scanningView(true)
        autoOpen()
        deviceManager.startScan()
    }

    private fun scanningView(scanning: Boolean) {
        if (scanning) {
            findViewById<LinearLayout>(R.id.device_scanning_layout).visibility = View.VISIBLE
            findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.device_recycle_view).visibility = View.GONE
        } else {
            findViewById<LinearLayout>(R.id.device_scanning_layout).visibility = View.GONE
            findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.device_recycle_view).visibility = View.VISIBLE
        }
    }

    private fun autoOpen() {
        thread {
            Thread.sleep(5000)
            runOnUiThread {
                scanningView(false)
                when (deviceList.size()) {
                    0 -> Toast.makeText(applicationContext,"No cameras found", Toast.LENGTH_SHORT).show()
                    1 -> deviceList.elementAt(0).openManagementInterface()
                    else -> {
                        Toast.makeText(applicationContext,"Multiple cameras found. Select what one to view", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun onDeviceUpdate() {
        runOnUiThread {
            deviceListAdapter.value!!.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        deviceManager.tearDown()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    fun refresh(item: MenuItem) {
        thread {
            deviceManager.tearDown()
            deviceList.clear()
            Thread.sleep(1000)   // Need to wait for device manager to tear down.
            deviceManager.startScan()
        }
    }
}