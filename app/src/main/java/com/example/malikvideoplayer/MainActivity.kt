package com.example.malikvideoplayer

import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.malikvideoplayer.ui.theme.MalikVideoPlayerTheme

lateinit var player: ExoPlayer
lateinit var playerView: PlayerView
lateinit var progressBar: ProgressBar
lateinit var titleTv: TextView
class MainActivity : ComponentActivity(), Player.Listener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        progressBar = findViewById(R.id.progressBar)
        titleTv = findViewById(R.id.title)
        playerView = findViewById(R.id.video_view)
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        player.addListener(this)
        val mediaItem = MediaItem.fromUri(getString(R.string.media_url_mp4))
        player.addMediaItem(mediaItem)
        player.prepare()

        if (savedInstanceState != null){
            savedInstanceState.getInt("mediaItem").let { restoredMedia ->
                val seekTime = savedInstanceState.getLong("seekTime")
                player.seekTo(restoredMedia, seekTime)
                player.play()
            }
        }


    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        when(playbackState){
            Player.STATE_BUFFERING -> {
                progressBar.visibility = View.VISIBLE
            }
            Player.STATE_READY -> {
                progressBar.visibility = View.INVISIBLE
            }
        }

    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        super.onMediaMetadataChanged(mediaMetadata)
        titleTv.text = mediaMetadata.title ?: mediaMetadata.displayTitle ?: "no title"
        titleTv.visibility = View.VISIBLE
        titleTv.alpha = 1f
        titleTv.postDelayed({
            titleTv.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    titleTv.visibility = View.GONE
                }
                .start()
        }, 4000)

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("seekTime", player.currentPosition)
        outState.putInt("mediaItem", player.currentMediaItemIndex)
    }

    override fun onStop() {
        super.onStop()
        player.release()
    }
}
