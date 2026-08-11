package com.example.malikvideoplayer

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private lateinit var ipInput: EditText
    private lateinit var portInput: EditText

    companion object {
        init { System.loadLibrary("my_multicast_test") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ipInput = findViewById(R.id.editTextSource)
        portInput = findViewById(R.id.editTextPort)
        val btnStart = findViewById<Button>(R.id.btn_start)
        val btnDefault = findViewById<Button>(R.id.btn_default)

        // Multicast Lock
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifi.createMulticastLock("multicast_lock")
        lock.setReferenceCounted(true)
        lock.acquire()

        btnStart.setOnClickListener {
            val ip = ipInput.text.toString()
            val port = portInput.text.toString().toIntOrNull() ?: 5000
            launch(ip, port)
        }

        btnDefault.setOnClickListener {
            launch("239.1.2.3", 5000)
        }
    }

    private fun launch(ip: String, port: Int) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("multicastIp", ip)
            putExtra("multicastPort", port)
            putExtra("phoneIp", getWifiIpAddress())
        }
        startActivity(intent)
    }

    private fun getWifiIpAddress(): String {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wm.connectionInfo.ipAddress
        return String.format("%d.%d.%d.%d", (ip and 0xff), (ip shr 8 and 0xff), (ip shr 16 and 0xff), (ip shr 24 and 0xff))
    }
}