package nz.org.cacophony.feverscreen

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class FeverWebView : AppCompatActivity() {

    private var mDetector: GestureDetector? = null
    private lateinit var myWebView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        val extras = intent.extras
        if (extras != null) {
            val uri = extras.getString("uri")
            myWebView = findViewById(R.id.fever_web_view)
            myWebView.settings.domStorageEnabled = true
            myWebView.settings.javaScriptEnabled = true
            myWebView.settings.mediaPlaybackRequiresUserGesture = false
            myWebView.settings.safeBrowsingEnabled = false
            myWebView.settings.mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW
            myWebView.settings.userAgentString = "feverscreen-app"
            myWebView.webViewClient = WebViewClient()
            myWebView.loadUrl(uri)
        }

        hideSystemUI()

        val myView = findViewById<View>(R.id.fever_web_view)
        mDetector = GestureDetector(this, HideSystemUiGestureListener {hideSystemUI()})
        myView.setOnTouchListener { _, event -> mDetector!!.onTouchEvent(event) }
    }

    class HideSystemUiGestureListener(val hideSystemUi: (() -> Unit)) : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            hideSystemUi()
            return true
        }
    }

    override fun onBackPressed() {
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
