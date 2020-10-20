package nz.org.cacophony.feverscreen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread

class DeviceSettingsActivity : AppCompatActivity() {

    private lateinit var client: OkHttpClient

    private lateinit var deviceName: String
    private lateinit var deviceIP: String
    private var version: String = ""
    private var releaseChannel: String = ""
    private var usb0Addr: String = ""
    private var updateCandidate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_settings)

        client = OkHttpClient.Builder().build()

        deviceName = intent.extras?.getString("deviceName") ?: ""
        deviceIP = intent.extras?.getString("deviceIP") ?: ""

        if (deviceName == "" || deviceIP == "") {
            Log.e(
                TAG,
                "failed to get device ip or name from intent. Name: $deviceName, IP: $deviceIP"
            )
            super.onBackPressed()
            return
        }

        updateNameText()
        updateAddressText()

        thread(start = true) {
            getVersionData()
            getUSBAddr()
            updateVersionText()
            updateChannelText()
        }
    }

    private fun getVersionData() {
        try {
            val url = getUrl("/api/version")
            val request = getAuthReq(url)
            val response = client.newCall(request).execute()
            val versionData = JSONObject(getResponseBody(response).string())
            version = if (versionData.has("appVersion")) versionData.getString("appVersion")
            else "could not find version"
            releaseChannel = if (versionData.has("channel")) versionData.getString("channel")
            else "could not find release channel"
            updateCandidate = if (versionData.has("appUpdateCandidate")) versionData.getString("appUpdateCandidate")
            else ""
        } catch (e :Exception) {
            Log.e(TAG, e.toString())
        }
    }
    
    private fun getUSBAddr() {
        val url = getUrl("/api/USB0Addr")
        val request = getAuthReq(url)
        val response = client.newCall(request).execute()
        try {
            usb0Addr = getResponseBody(response).string()
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    private fun putReleaseChannel(newChannel: String) {
        Log.i(TAG, "changing release channel to $newChannel")
        runOnUiThread {
            Toast.makeText(
                applicationContext,
                "Changing to release channel $newChannel",
                Toast.LENGTH_SHORT
            ).show()
        }
        val url = getUrl("/api/release-channel")
        val body = FormBody.Builder()
            .addEncoded("channel", newChannel)
            .build()
        val request = getAuthReq(url)
            .newBuilder()
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        response.close()

        if (!response.isSuccessful) {
            runOnUiThread {
                Toast.makeText(
                    applicationContext,
                    "Failed to change release channel to $newChannel",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            runOnUiThread {
                Toast.makeText(
                    applicationContext,
                    "Changed to release channel $newChannel",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getResponseBody(response: Response): ResponseBody {
        if (!response.isSuccessful) {
            response.close()
            throw Exception("call failed. Error: ${response.message()}")  // TODO Add more useful info in exception
        }
        val body = response.body()
        if (body == null) {
            response.close()
            throw Exception("failed to get body from response")
        } else {
            return body
        }
    }

    private fun getUrl(path: String): HttpUrl {
        val urlStr = URL("http", deviceIP, 80, path).toString()
        return HttpUrl.parse(urlStr) ?: throw Exception("failed to parse URL: $urlStr")
    }

    private fun getAuthReq(url: HttpUrl): Request {
        //TODO Add better security...
        return Request.Builder()
            .url(url)
            .addHeader("Authorization", "Basic YWRtaW46ZmVhdGhlcnM=")
            .build()
    }


    @Suppress("UNUSED_PARAMETER")
    fun updateFeverscreen(view: View) {
        Log.i(TAG, "update feverscreen")
        thread(start = true) {
            val url = getUrl("/api/update")
            val request = getAuthReq(url)
                .newBuilder()
                .put(FormBody.Builder().build())
                .build()
            val response = client.newCall(request).execute()
            response.close()
            val message = if (response.isSuccessful) {"Updating feverscreen. Might take a few minutes."} else {"failed to trigger update"}
            runOnUiThread {
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
            }

        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun checkForUpdate(view: View) {
        Log.i(TAG, "checking for available feverscreen updates")
        thread(start = true) {
            try {
                val updateButton = findViewById<Button>(R.id.check_for_update_button)
                runOnUiThread {
                    updateButton.isEnabled = false
                    updateButton.text = "checking..."
                    Toast.makeText(applicationContext, "Checking for updates", Toast.LENGTH_LONG).show()
                }
                val url = getUrl("/api/apt-update")
                val request = getAuthReq(url)
                    .newBuilder()
                    .put(FormBody.Builder().build())
                    .build()
                //TODO try catch
                val response = client.newCall(request).execute()
                response.close()
                getVersionData()
                updateVersionText()
                val message = if (response.isSuccessful) {"Latest version available is $updateCandidate"} else {"failed to check for updates"}
                runOnUiThread {
                    updateButton.isEnabled = true
                    updateButton.text = "check for update"
                    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                }
            } catch (e :Exception) {
                Log.e(TAG, e.toString())
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun openInBrowser(view: View) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://$deviceIP"))
        startActivity(browserIntent)
    }


    @Suppress("UNUSED_PARAMETER")
    fun openChangeReleaseChannel(v: View) {
        Log.i(TAG, "open change release channel option")
        val releaseChannels = arrayOf("stable", "beta", "nightly")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose release channel. Currently on $releaseChannel")
        builder.setItems(releaseChannels) { _, which ->
            thread {
                putReleaseChannel(releaseChannels[which])
                getVersionData()
                updateChannelText()
            }
        }
        builder.setNegativeButton("Cancel") { _, _ -> }
        builder.show()
    }


    private fun updateNameText() {
        runOnUiThread {
            findViewById<TextView>(R.id.device_name_text_view).text = deviceName
        }
    }

    private fun updateAvailable(): Boolean {
        return (version != updateCandidate && updateCandidate != "")
    }

    private fun updateVersionText() {
        runOnUiThread {
            if (updateAvailable()) {
                findViewById<TextView>(R.id.device_version_text_view).text = "$version, update available to $updateCandidate"
            } else {
                findViewById<TextView>(R.id.device_version_text_view).text = "$version"
            }
        }
    }

    private fun updateAddressText() {
        runOnUiThread {
            findViewById<TextView>(R.id.device_ip_text_view).text = deviceIP
        }
    }

    private fun updateChannelText() {
        runOnUiThread {
            findViewById<TextView>(R.id.device_channel_text_view).text = releaseChannel
        }
    }
}