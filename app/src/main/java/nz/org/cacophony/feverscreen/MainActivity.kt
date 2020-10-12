package nz.org.cacophony.feverscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.net.nsd.NsdManager
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.thread


const val TAG = "feverscreen"

class MainActivity : AppCompatActivity() {

    private val deviceListAdapter = MutableLiveData<DeviceListAdapter>()
    private lateinit var deviceManager: DeviceManager
    private lateinit var deviceList: DeviceList
    private var openWebViewAt: Calendar? = null
    private var downloadNewSoftwareLink: String = ""

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

        enableChargeDetectDialog()
        checkIfCharging(applicationContext)
        checkConnectionLoop()
    }

    private fun enableChargeDetectDialog() {
        val filter = IntentFilter()
        filter.addAction("android.intent.action.ACTION_POWER_CONNECTED")
        filter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED")
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                checkIfCharging(context)
            }
        }
        registerReceiver(receiver, filter)
    }

    private var receiver: BroadcastReceiver? = null

    fun checkIfCharging(context: Context) {
        thread {
            Thread.sleep(1000) // wait for USB power state to be updated
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val charging = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB
            if (!charging) {
                val i = Intent(context, AlertDialogActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(i)
            }
        }
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
                    var openedDevice = false
                    for ((_, device) in deviceList.getConnectedMap()) {
                        if (device.connectionInterface == "usb") {
                            device.openFeverPage()
                            openedDevice = true
                            break
                        }
                    }
                    if (!openedDevice) {
                        when (deviceList.size()) {
                            0 -> {
                                findViewById<TextView>(R.id.auto_connect_text_view).text = "No cameras found after 10s"
                                findViewById<Button>(R.id.network_settings_button).visibility = View.VISIBLE
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
            if (deviceList.size() > 0) {
                findViewById<Button>(R.id.network_settings_button).visibility = View.INVISIBLE
            }
        }
    }

    override fun onDestroy() {
        deviceManager.tearDown()
        applicationContext.unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onResume() {
        openWebViewIn()
        thread(start = true) {
            checkForUpdate()
        }
        super.onResume()
    }

    private fun checkForUpdate() {
        try {
            val url = HttpUrl.parse("https://api.github.com/repos/feverscreen/feverscreen-app/releases")
            val request = Request.Builder()
                .url(url)
                .build()
            val response = OkHttpClient().newCall(request).execute()
            val body = response.body()
            if (body == null) {
                Log.e(TAG, "failed to get response body when checking for an app update")
                return
            }
            val bodyString = body.string()
            val responseJSON = JSONArray(bodyString)
            var latestReleaseJSON = JSONObject()
            var latestReleaseTagName = ""
            val stableReleasePatter = Regex("v\\d+\\.\\d+\\.\\d+")
            for (i in 0 until responseJSON.length()) {
                val release = responseJSON.getJSONObject(i)
                val draft = release.getBoolean("draft")
                val prerelease = release.getBoolean("prerelease")
                val tagName = release.getString("tag_name")
                if (!draft && !prerelease && stableReleasePatter.matches(tagName)) {
                    latestReleaseTagName = tagName
                    latestReleaseJSON = release
                    break
                }
            }

            val assets = latestReleaseJSON.getJSONArray("assets")
            downloadNewSoftwareLink = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    downloadNewSoftwareLink = asset.getString("browser_download_url")
                }
            }
            if (downloadNewSoftwareLink == "") {
                Log.e(TAG, "unable to find link to download software")
                return
            }


            val downloadSoftwareButton = findViewById<Button>(R.id.button_download_new_software)
            if (latestReleaseTagName.contains(BuildConfig.VERSION_NAME)) {
                Log.i(TAG, "is latest version $latestReleaseTagName, ${BuildConfig.VERSION_NAME}")
                runOnUiThread {
                    downloadSoftwareButton.visibility = View.GONE
                }
            } else {
                Log.i(TAG, "is not latest version")
                runOnUiThread {
                    downloadSoftwareButton.text = "Download latest stable release ($latestReleaseTagName)"
                    downloadSoftwareButton.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    fun downloadNewSoftware(v: View) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadNewSoftwareLink))
        startActivity(browserIntent)
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

    fun openNetworkSettingsMenuItem(item: MenuItem) {
        openNetworkSettings()
    }

    fun openNetworkSettingsButton(v: View) {
        openNetworkSettings()
    }

    fun openNetworkSettings() {
        cancelAutoOpen()
        startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
    }

    private fun checkConnectionLoop() {
        thread {
            while (true) {
                for ((_, device) in deviceList.getAllMap()) {
                    device.checkConnectionStatus()
                    deviceList.setDeviceConnected(device.hostAddress, device.sm.state == DeviceState.CONNECTED)
                }
                onDeviceUpdate()
                Thread.sleep(5_000)
            }
        }
    }
}