package nz.org.cacophony.feverscreen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.nsd.NsdManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import kotlin.concurrent.thread


const val TAG = "feverscreen"

class MainActivity : AppCompatActivity() {

    private val deviceListAdapter = MutableLiveData<DeviceListAdapter>()
    private var autoOpen = true
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
        runAutoOpen()
        deviceManager.startScan()
    }

    private fun scanningView(scanning: Boolean) {
        if (scanning) {
            findViewById<LinearLayout>(R.id.device_scanning_layout).visibility = View.VISIBLE
            findViewById<LinearLayout>(R.id.device_layout).visibility = View.GONE
        } else {
            findViewById<LinearLayout>(R.id.device_scanning_layout).visibility = View.GONE
            findViewById<LinearLayout>(R.id.device_layout).visibility = View.VISIBLE
        }
    }

    private fun runAutoOpen() {
        thread {
            Thread.sleep(5000)
            runOnUiThread {
                scanningView(false)
                if (autoOpen) {
                    when (deviceList.size()) {
                        0 -> Toast.makeText(applicationContext,"No cameras found", Toast.LENGTH_SHORT).show()
                        1 -> deviceList.elementAt(0).openFeverPage()
                        else -> {
                            Toast.makeText(applicationContext,"Multiple cameras found. Select what one to view", Toast.LENGTH_SHORT).show()
                        }
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
        autoOpen = false
        thread {
            deviceManager.tearDown()
            deviceList.clear()
            Thread.sleep(1000)   // Need to wait for device manager to tear down.
            deviceManager.startScan()
        }
    }

    fun openReleasesPage(item: MenuItem) {
        autoOpen = false
        val releasesPage = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/feverscreen/feverscreen-app/releases"))
        startActivity(releasesPage)
    }

    fun openNetworkSettings(item: MenuItem) {
        autoOpen = false
        startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
    }
}