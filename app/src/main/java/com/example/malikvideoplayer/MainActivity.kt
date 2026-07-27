package com.example.malikvideoplayer

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
// Note: We no longer import runReceiver.
// We now use the MulticastReceiver class directly.
import uniffi.my_multicast_test.MulticastReceiver

class MainActivity : ComponentActivity() {
    lateinit var editText: EditText
    lateinit var button: Button
    lateinit var btnAljazeera: Button

    private var multicastLock: WifiManager.MulticastLock? = null
    private var isListening = false

    companion object {
        init {
            System.loadLibrary("my_multicast_test")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("multicast_lock")
        multicastLock?.setReferenceCounted(true)
        multicastLock?.acquire()

        editText = findViewById(R.id.editTextSource)
        button = findViewById(R.id.btn_start)
        btnAljazeera = findViewById(R.id.buttonAljazeera)

        button.setOnClickListener {
            val phoneIp = getWifiIpAddress()
            val intent = Intent(this@MainActivity, PlayerActivity::class.java)
            intent.putExtra("phoneIp", phoneIp)
            startActivity(intent)
        }
    }

    private fun getWifiIpAddress(): String {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wifiManager.connectionInfo.ipAddress
        return String.format(
            "%d.%d.%d.%d",
            (ip and 0xff),
            (ip shr 8 and 0xff),
            (ip shr 16 and 0xff),
            (ip shr 24 and 0xff)
        )
    }

    private fun startMulticastListener() {
        if (isListening) return
        isListening = true

        Thread {
            val phoneIp = getWifiIpAddress()
            try {
                // --- THIS IS THE FIX ---
                // 1. Create the Rust Object (Constructor)
                val receiver = MulticastReceiver("239.1.2.3", 5000.toUShort(), phoneIp)

                println("RUST: Class created. Waiting for 1 packet...")

                // 2. Call the method on the object
                val packetData = receiver.readPacketsBatch()

                if (packetData.isNotEmpty()) {
                    val size = packetData.size
                    println("RUST RESULT: Success! Received $size bytes")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Test Success: $size bytes", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                println("RUST ERROR: ${e.message}")
            } finally {
                isListening = false
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        multicastLock?.release()
    }
}