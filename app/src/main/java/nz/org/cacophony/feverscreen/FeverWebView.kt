package nz.org.cacophony.feverscreen

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class FeverWebView : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        val extras = intent.extras
        if (extras != null) {
            val uri = extras.getString("uri")
            val myWebView: WebView = findViewById(R.id.fever_web_view)
            myWebView.settings.domStorageEnabled = true
            myWebView.settings.javaScriptEnabled = true
            myWebView.webViewClient = WebViewClient()
            myWebView.loadUrl("$uri/static/html/fever.html")
        }
    }
}
