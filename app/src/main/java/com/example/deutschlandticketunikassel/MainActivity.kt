package com.example.deutschlandticketunikassel

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLDecoder
import java.time.LocalDate


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val text: TextView = findViewById(R.id.no_ticket_text)
        text.visibility = INVISIBLE

        val lastMonth = getMonth(this)
        val currentMonth = LocalDate.now().month.toString()

        val downloadFolder = this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString()
        val offlineTicket = File("$downloadFolder/ticket.html")

        if (offlineTicket.exists() && lastMonth == currentMonth) {
            openOffline("$downloadFolder/ticket.html")
        } else if (isOnline(this)){
            openWebsite()
        } else {
            // no internet and no current offline ticket available
            text.visibility = VISIBLE
            val mainHandler = Handler(Looper.getMainLooper())
            val connectivityManager : ConnectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCallback = object: ConnectivityManager.NetworkCallback(){

                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    val myRunnable = Runnable {
                        openWebsite()
                        text.visibility = INVISIBLE
                    }
                    mainHandler.post(myRunnable)
                }
            }
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        }
    }

    private fun openOffline(file: String) {
        val webview: WebView = findViewById<WebView>(R.id.webview)
        webview.webChromeClient = WebChromeClient()
        val webviewSetting = webview.settings
        webviewSetting.javaScriptEnabled = true
        webviewSetting.domStorageEnabled = true
        webviewSetting.allowFileAccess = true
        webviewSetting.loadsImagesAutomatically = true
        webview.loadUrl(file)
    }

    private fun openWebsite() {
        val webview: WebView = findViewById<WebView>(R.id.webview)
        webview.webChromeClient = WebChromeClient()
        val webviewSetting = webview.settings
        webviewSetting.javaScriptEnabled = true
        webviewSetting.domStorageEnabled = true
        webviewSetting.allowFileAccess = true
        webviewSetting.loadsImagesAutomatically = true
        var currentUrl = ""
        val currentMonth = LocalDate.now().month.toString()

        webview.setDownloadListener{ url, userAgent, contentDisposition, mimeType, _ ->
            val htmlData = url.substring(url.indexOf(",") + 1)
            saveDataToFile(this, htmlData, currentMonth)
        }

        webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
                view?.loadUrl(url)
                return true
            }

            override fun onPageFinished(view: WebView?, url: String) {
                // do your stuff here
                if (url == currentUrl) {
                    super.onPageFinished(view, url)
                    webview.evaluateJavascript("exportOffline()", null)
                    println("exportOffline")
                }
            }

            override fun onPageStarted(view: WebView?, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if ("https://ticket.astakassel.de/" in url) {
                    currentUrl = url
                }
            }
        }
        webview.loadUrl("https://ticket.astakassel.de/")
    }


    private fun saveDataToFile(context: Context, data: String, month: String) {
        val decodedData = URLDecoder.decode(data, "UTF-8")
        try {
            val file = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "ticket.html"
            )
            val outputStream = FileOutputStream(file)
            outputStream.write(decodedData.toByteArray())
            outputStream.close()
            saveMonth(this, month)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }

    // Function to save a string to SharedPreferences
    private fun saveMonth(context: Context, value: String) {
        val sharedPreferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("month", value)
        editor.apply()
    }

    // Function to retrieve a string from SharedPreferences
    private fun getMonth(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("month", null)
    }
}