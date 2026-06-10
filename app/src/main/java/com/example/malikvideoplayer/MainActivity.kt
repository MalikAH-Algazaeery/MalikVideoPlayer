package com.example.malikvideoplayer

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import uniffi.my_multicast_test.runReceiver

class MainActivity : ComponentActivity() {
    lateinit var editText: EditText
    lateinit var button: Button
    lateinit var btnAljazeera: Button

    // Multicast Lock to prevent Android from filtering packets
    private var multicastLock: WifiManager.MulticastLock? = null
    private var isListening = false

    companion object {
        init {
            // This loads the libmy_multicast_test.so file
            System.loadLibrary("my_multicast_test")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Acquire Multicast Lock
        // This is necessary on FritzBox/Home networks to let the phone see IGMP traffic
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("multicast_lock")
        multicastLock?.setReferenceCounted(true)
        multicastLock?.acquire()

        editText = findViewById(R.id.editTextSource)
        button = findViewById(R.id.btn_start)
        btnAljazeera = findViewById(R.id.buttonAljazeera)

        button.setOnClickListener {
            // 2. Start the Rust Listener in the background
            startMulticastListener()

            // 3. Start Video Player (Existing logic)
            val link = editText.text.toString()
            val intent = Intent(this@MainActivity, PlayerActivity::class.java)
            intent.putExtra("link", link)
            startActivity(intent)
        }

        btnAljazeera.setOnClickListener {
            val intent = Intent(this@MainActivity, AljazeeraActivity::class.java)
            startActivity(intent)
        }
    }

    // Helper: Converts phone WiFi info into an IP string (e.g., "192.168.178.68")
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
            println("RUST: Starting 5-second wait on IP: $phoneIp")

            try {
                // Call the Rust bridge.
                // Because of our new Rust code, this function will now "hang"
                // for 5 seconds to give the FritzBox time to send the packet.
                val bytes = runReceiver("239.1.2.3", 5000.toUShort(), phoneIp)

                if (bytes > 0u) {
                    println("RUST RESULT: Success! Received $bytes bytes")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "SUCCESS: Received $bytes bytes!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    println("RUST RESULT: Timed out (0 bytes). Did you send the packet?")
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
        // Release the lock when the app is closed to save battery
        multicastLock?.release()
    }
}