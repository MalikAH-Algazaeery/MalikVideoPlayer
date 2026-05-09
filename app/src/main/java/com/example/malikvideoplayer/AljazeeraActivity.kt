package com.example.malikvideoplayer

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity

class AljazeeraActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aljazeera)
        val webView: WebView = findViewById<WebView>(R.id.wv)
        webView.settings.javaScriptEnabled = true
        val link = "<iframe width=\"100%\" height=\"100%\" src=\"https://www.youtube-nocookie.com/embed/bNyUyrR0PHo?si=WX05eyvBlvhJmkV9\" title=\"YouTube video player\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen></iframe>"
        webView.webChromeClient = WebChromeClient()
        webView.setBackgroundColor(android.graphics.Color.BLACK)
        webView.loadDataWithBaseURL("https://www.youtube-nocookie.com", link, "text/html", "UTF-8", null)



    }
}