package nz.org.cacophony.feverscreen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.nsd.NsdManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import java.util.*
import kotlin.concurrent.thread


const val TAG = "feverscreen"

class MainActivity : AppCompatActivity() {

    private val deviceListAdapter = MutableLiveData<DeviceListAdapter>()
    private lateinit var deviceManager: DeviceManager
    private lateinit var deviceList: DeviceList
    private var openWebViewAt: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.version_text).text = "Feverscreen version: " + BuildConfig.VERSION_NAME

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
        deviceManager.startScan()
        openWebViewIn()
    }

    private fun openWebViewIn(delayMill: Int = 10000) {
        val cal: Calendar = Calendar.getInstance()
        cal.add(Calendar.MILLISECOND, delayMill)
        openWebViewAt = cal
        runOnUiThread {
            findViewById<TextView>(R.id.auto_connect_text_view).text =
                "Will connect automatically after 10s"
        }
        thread {
            Thread.sleep(delayMill.toLong()+10)
            val now = Calendar.getInstance()
            if (openWebViewAt != null && openWebViewAt!! < now) {
                runOnUiThread {
                    when (deviceList.size()) {
                        0 -> {
                            findViewById<TextView>(R.id.auto_connect_text_view).text = "No cameras found after 10s"
                        }
                        1 -> deviceList.elementAt(0).openFeverPage()
                        else -> {
                            findViewById<TextView>(R.id.auto_connect_text_view).text = "Multiple cameras found. Select what one to view"
                        }
                    }
                }
            }
        }
    }

    private fun cancelAutoOpen() {
        openWebViewAt = null
        runOnUiThread {
            findViewById<TextView>(R.id.auto_connect_text_view).text = ""
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

    override fun onResume() {
        openWebViewIn()
        super.onResume()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (!hasFocus) {
            cancelAutoOpen()
        }
        super.onWindowFocusChanged(hasFocus)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    fun refresh(view: View) {
        thread {
            openWebViewIn()
            val refreshButton = findViewById<Button>(R.id.refresh_button)
            runOnUiThread {
                refreshButton.text = "Refreshing..."
                refreshButton.isClickable = false
            }
            deviceManager.tearDown()
            deviceList.clear()
            Thread.sleep(1000)   // Need to wait for device manager to tear down.
            deviceManager.startScan()
            Thread.sleep(5000)
            runOnUiThread {
                refreshButton.text = "Refresh"
                refreshButton.isClickable = true
            }
        }
    }

    fun openReleasesPage(item: MenuItem) {
        cancelAutoOpen()
        val releasesPage = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/feverscreen/feverscreen-app/releases"))
        startActivity(releasesPage)
    }

    fun openSupportPage(item: MenuItem) {
        cancelAutoOpen()
        val releasesPage = Intent(Intent.ACTION_VIEW, Uri.parse("https://tekahuora.com/pages/support"))
        startActivity(releasesPage)
    }

    fun openNetworkSettings(item: MenuItem) {
        cancelAutoOpen()
        startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
    }
}