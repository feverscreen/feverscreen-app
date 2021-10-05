package nz.org.cacophony.feverscreen

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.webkit.*
import android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import okhttp3.FormBody
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executor
import kotlin.concurrent.thread


class FeverWebView : AppCompatActivity() {

    private var mDetector: GestureDetector? = null
    private lateinit var myWebView: WebView
    private lateinit var proxyConfig: ProxyConfig
    private var uri: String = ""
    @Volatile
    private var open: Boolean = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        WebView.setWebContentsDebuggingEnabled(true)
        val extras = intent.extras
        if (extras != null) {
            checkCameraPermissions()
            loadWebUI(extras)

        }
    }

    private fun checkCameraPermissions() {
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    Log.i("Permission: ", "Granted")
                } else {
                    Log.i("Permission: ", "Denied")
                }
            }
        when (PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) -> {
                Log.i("Permission: ", "Granted")
            }
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    private fun loadWebUI(extras: Bundle) {
        val connection = extras.getString("deviceConnection")
        uri = if (connection == "wifi") {
            extras.getString("uri") ?: ""
        } else {
            "http://localhost:8080/static/html/fever.html"
        }

        var finishedLoading = true;
        var redirect = false;

        myWebView = findViewById(R.id.fever_web_view)
        myWebView.settings.domStorageEnabled = true
        myWebView.settings.javaScriptEnabled = true
        myWebView.settings.mediaPlaybackRequiresUserGesture = false
        myWebView.settings.safeBrowsingEnabled = false
        myWebView.settings.mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW
        myWebView.settings.userAgentString = "feverscreen-app"
        myWebView.settings.allowFileAccessFromFileURLs = true
        myWebView.settings.allowUniversalAccessFromFileURLs = true
        myWebView.webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.clearHistory()
            }
        }
        myWebView.webChromeClient = object: WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }

        myWebView.loadUrl(uri)
        hideSystemUI()

        val myView = findViewById<View>(R.id.fever_web_view)
        mDetector = GestureDetector(this, HideSystemUiGestureListener {hideSystemUI()})
        myView.setOnTouchListener { _, event -> mDetector!!.onTouchEvent(event) }
        checkConnectionLoop(extras)
    }

    private fun checkConnectionLoop(extras: Bundle) {
        thread(start = true) {
            var didRetry = false
            while (open) {
                Log.d(TAG, "Checking connection to '$uri'")
                try {
                    val conn = URL(uri).openConnection() as HttpURLConnection
                    conn.connectTimeout = 3000
                    conn.readTimeout = 3000
                    conn.responseCode
                    conn.disconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "failed connecting to: $e")
                    runOnUiThread {
                        if (!didRetry) {
                            uri = extras.getString("uri") ?: ""
                            myWebView.loadUrl(uri)
                            didRetry = true
                        } else {
                            onBackPressed()
                        }
                    }
                }
                Thread.sleep(5_000)
            }
        }
    }

    class HideSystemUiGestureListener(val hideSystemUi: (() -> Unit)) : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            hideSystemUi()
            return true
        }
    }

    override fun onBackPressed() {
        open = false
        if (myWebView.copyBackForwardList().currentIndex > 0) {
            myWebView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }


}
