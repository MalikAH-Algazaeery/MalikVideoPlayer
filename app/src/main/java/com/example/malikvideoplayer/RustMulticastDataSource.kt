package com.example.malikvideoplayer

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import uniffi.my_multicast_test.MulticastReceiver
import java.io.FileOutputStream
import kotlin.concurrent.thread

@OptIn(UnstableApi::class)
class RustMulticastDataSource(private val receiver: MulticastReceiver) : BaseDataSource(true) {

    private var internalBuffer: ByteArray? = null
    private var bufferPosition = 0

    // File stream for recording. Volatile ensures the background thread sees it immediately.
    @Volatile var recordStream: FileOutputStream? = null

    override fun open(dataSpec: DataSpec): Long = C.LENGTH_UNSET.toLong()

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        // If our current memory chunk is exhausted, pull a new batch from Rust
        if (internalBuffer == null || bufferPosition >= internalBuffer!!.size) {
            val newData = receiver.readPacketsBatch()
            if (newData.isEmpty()) return 0 // Wait for more network data

            // RECORDING: If user clicked REC, write the bytes to disk in a separate thread.
            // This "Fire and Forget" approach ensures disk speed doesn't affect video quality.
            val stream = recordStream
            if (stream != null) {
                thread { try { stream.write(newData) } catch (e: Exception) {} }
            }

            internalBuffer = newData
            bufferPosition = 0
        }

        // Copy bytes from memory to ExoPlayer
        val remaining = internalBuffer!!.size - bufferPosition
        val toCopy = minOf(remaining, length)
        System.arraycopy(internalBuffer!!, bufferPosition, buffer, offset, toCopy)
        bufferPosition += toCopy

        return toCopy
    }

    override fun getUri(): Uri? = Uri.parse("udp://live")
    override fun close() { internalBuffer = null }
}