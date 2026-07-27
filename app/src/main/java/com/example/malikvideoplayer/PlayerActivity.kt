package com.example.malikvideoplayer

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import uniffi.my_multicast_test.MulticastReceiver

@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private var rustReceiver: MulticastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        playerView = findViewById(R.id.video_view)

        val phoneIp = intent.getStringExtra("phoneIp") ?: ""

        // 1. Initialize the Rust Object (Stay Joined)
        rustReceiver = MulticastReceiver("239.1.2.3", 5000.toUShort(), phoneIp)

        // 2. Setup ExoPlayer with our Rust Source
        val dataSourceFactory = DataSource.Factory {
            RustMulticastDataSource(rustReceiver!!)
        }

        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri("udp://239.1.2.3:5000"))

        player?.setMediaSource(mediaSource)
        player?.prepare()
        player?.playWhenReady = true
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        // Rust object is cleaned up automatically here
    }
}