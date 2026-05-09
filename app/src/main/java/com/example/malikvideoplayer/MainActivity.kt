package com.example.malikvideoplayer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.media3.common.Player

class MainActivity : ComponentActivity(), Player.Listener {
    lateinit var editText: EditText
    lateinit var button: Button
    lateinit var btnAljazeera: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        editText = findViewById(R.id.editTextSource)
        button = findViewById(R.id.btn_start)
        btnAljazeera = findViewById(R.id.buttonAljazeera)
        button.setOnClickListener {
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
}