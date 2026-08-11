package com.example.malikvideoplayer

import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import uniffi.my_multicast_test.MulticastReceiver
import java.io.File
import java.io.FileOutputStream

@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private var streamManager: MulticastStreamManager? = null
    private var cacheFile: File? = null
    private var currentRecordFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val playerView = findViewById<PlayerView>(R.id.video_view)
        val btnRecord = findViewById<Button>(R.id.btn_record)

        val phoneIp = intent.getStringExtra("phoneIp") ?: ""
        val mIp = intent.getStringExtra("multicastIp") ?: "239.1.2.3"
        val mPort = intent.getIntExtra("multicastPort", 5000)

        // 1. Create a clean temp file for this session's seeking
        cacheFile = File(externalCacheDir, "stream_cache.ts")
        if (cacheFile!!.exists()) cacheFile!!.delete()
        cacheFile!!.createNewFile()

        // 2. Start the Manager (Rust -> Disk)
        val receiver = MulticastReceiver(mIp, mPort.toUShort(), phoneIp)
        streamManager = MulticastStreamManager(receiver, cacheFile!!)

        // 3. Setup ExoPlayer to read from that Manager
        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        val dataSourceFactory = DataSource.Factory {
            RustMulticastDataSource(streamManager!!, cacheFile!!)
        }

        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.Builder()
                .setUri("file:///live_stream")
                .setMimeType(MimeTypes.VIDEO_MP2T) // Forces seeking support for .ts
                .build())

        player?.setMediaSource(mediaSource)
        player?.prepare()
        player?.play()

        // 4. Recording Logic (No player restart required!)
        btnRecord.setOnClickListener {
            if (streamManager?.recordStream == null) {
                // START RECORDING
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                currentRecordFile = File(downloads, "Multicast_${System.currentTimeMillis()}.ts")
                streamManager?.recordStream = FileOutputStream(currentRecordFile)
                btnRecord.text = "STOP"
                Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show()
            } else {
                // STOP RECORDING
                val path = currentRecordFile?.absolutePath
                streamManager?.recordStream?.close()
                streamManager?.recordStream = null
                btnRecord.text = "REC"

                // Make the file visible in "My Files" immediately
                MediaScannerConnection.scanFile(this, arrayOf(path), null) { _, _ ->
                    runOnUiThread { Toast.makeText(this, "Saved to Downloads!", Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        streamManager?.release()
        cacheFile?.delete()
    }
}