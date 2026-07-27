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
class RustMulticastDataSource(private val receiver: MulticastReceiver) : BaseDataSource(true) {
    private var internalBuffer: ByteArray? = null
    private var bufferPosition = 0

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        // If our tank is empty, refill it from Rust
        if (internalBuffer == null || bufferPosition >= internalBuffer!!.size) {
            val newData = receiver.readPacketsBatch()
            if (newData.isEmpty()) return 0

            internalBuffer = newData
            bufferPosition = 0
        }

        // How much can we give ExoPlayer right now?
        val available = internalBuffer!!.size - bufferPosition
        val toCopy = minOf(available, length)

        System.arraycopy(internalBuffer!!, bufferPosition, buffer, offset, toCopy)
        bufferPosition += toCopy

        return toCopy
    }

    override fun open(dataSpec: DataSpec): Long = C.LENGTH_UNSET.toLong()
    override fun close() { internalBuffer = null }
    override fun getUri(): Uri? = null
}