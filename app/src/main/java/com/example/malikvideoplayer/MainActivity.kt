package com.example.malikvideoplayer

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private var multicastLock: WifiManager.MulticastLock? = null

    companion object {
        init {
            // Load the Rust shared library (.so) from jniLibs
            System.loadLibrary("my_multicast_test")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ipInput = findViewById<EditText>(R.id.editTextSource)
        val portInput = findViewById<EditText>(R.id.editTextPort)
        val btnStart = findViewById<Button>(R.id.btn_start)

        // Acquire Multicast Lock: Prevents the phone from dropping UDP packets to save battery
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("multicast_lock")
        multicastLock?.setReferenceCounted(true)
        multicastLock?.acquire()

        btnStart.setOnClickListener {
            val ip = ipInput.text.toString()
            val port = portInput.text.toString().toIntOrNull() ?: 5000

            // Pass configuration to the PlayerActivity
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("mIp", ip)
                putExtra("mPort", port)
                putExtra("phoneIp", getWifiIpAddress())
            }
            startActivity(intent)
        }
    }

    // Convert internal WiFi IP address to String format for Rust binding
    private fun getWifiIpAddress(): String {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wm.connectionInfo.ipAddress
        return String.format("%d.%d.%d.%d", (ip and 0xff), (ip shr 8 and 0xff), (ip shr 16 and 0xff), (ip shr 24 and 0xff))
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release lock to avoid unnecessary battery drain when app is closed
        multicastLock?.release()
    }
}