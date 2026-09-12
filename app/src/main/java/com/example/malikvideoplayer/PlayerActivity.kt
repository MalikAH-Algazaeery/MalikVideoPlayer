package com.example.malikvideoplayer

import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import uniffi.my_multicast_test.MulticastReceiver
import java.io.File
import java.io.FileOutputStream

@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private var dataSource: RustMulticastDataSource? = null
    private var rustReceiver: MulticastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val playerView = findViewById<PlayerView>(R.id.video_view)
        val btnRecord = findViewById<Button>(R.id.btn_record)

        val mIp = intent.getStringExtra("mIp") ?: "239.1.2.3"
        val mPort = intent.getIntExtra("mPort", 5000)
        val phoneIp = intent.getStringExtra("phoneIp") ?: ""

        // 1. Establish Rust connection and set up Data Source
        rustReceiver = MulticastReceiver(mIp, mPort.toUShort(), phoneIp)
        dataSource = RustMulticastDataSource(rustReceiver!!)

        // 2. Setup ExoPlayer
        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        val mediaSource = ProgressiveMediaSource.Factory { dataSource!! }
            .createMediaSource(MediaItem.fromUri("udp://live"))

        player?.setMediaSource(mediaSource)
        player?.prepare()
        player?.play()

        // 3. UI LOGIC: REC button appears only on screen tap
        playerView.setOnClickListener {
            if (btnRecord.visibility == View.GONE) {
                btnRecord.visibility = View.VISIBLE
                btnRecord.alpha = 0f
                btnRecord.animate().alpha(0.5f).setDuration(300).start()

                // Automatically hide after 4 seconds
                btnRecord.postDelayed({
                    btnRecord.animate().alpha(0f).setDuration(500).withEndAction {
                        btnRecord.visibility = View.GONE
                    }.start()
                }, 4000)
            }
        }

        // 4. RECORDING Logic
        var currentFile: File? = null
        btnRecord.setOnClickListener {
            if (dataSource?.recordStream == null) {
                // START RECORDING
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                currentRecordFile = File(dir, "Record_${System.currentTimeMillis()}.ts")
                dataSource?.recordStream = FileOutputStream(currentRecordFile)
                btnRecord.text = "STOP"
                Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show()
            } else {
                // STOP RECORDING
                val path = currentRecordFile?.absolutePath
                dataSource?.recordStream?.close()
                dataSource?.recordStream = null
                btnRecord.text = "REC"

                // Notify system to show file in "My Files" immediately
                MediaScannerConnection.scanFile(this, arrayOf(path), null, null)
                Toast.makeText(this, "Saved to Downloads", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var currentRecordFile: File? = null

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        dataSource?.recordStream?.close()
        rustReceiver = null // Important to close Rust UDP socket
    }
}