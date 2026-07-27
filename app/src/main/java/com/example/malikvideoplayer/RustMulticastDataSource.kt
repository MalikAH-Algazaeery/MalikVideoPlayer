package com.example.malikvideoplayer

import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.common.C
import android.net.Uri
import uniffi.my_multicast_test.MulticastReceiver

@OptIn(UnstableApi::class)
class RustMulticastDataSource(
    private val receiver: MulticastReceiver
) : BaseDataSource(true) {

    override fun open(dataSpec: DataSpec): Long {
        return C.LENGTH_UNSET.toLong() // We don't know the video length (it's a live stream)
    }

    override fun close() {
        // Connection is closed when the player stops
    }

    override fun getUri(): Uri? = null

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        // Ask Rust for the next packet of video data
        val rawData = receiver.readPacket()

        if (rawData.isEmpty()) return 0

        // Copy the bytes into ExoPlayer's buffer
        println("DEBUG: Rust fed ${rawData.size} bytes into ExoPlayer")
        val bytesToCopy = minOf(rawData.size, length)
        rawData.copyInto(buffer, offset, 0, bytesToCopy)

        return bytesToCopy
    }
}