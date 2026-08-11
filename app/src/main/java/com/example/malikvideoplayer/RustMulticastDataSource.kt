package com.example.malikvideoplayer

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import java.io.File
import java.io.RandomAccessFile

@OptIn(UnstableApi::class)
class RustMulticastDataSource(
    private val manager: MulticastStreamManager,
    private val cacheFile: File
) : BaseDataSource(true) {

    private val fileReader = RandomAccessFile(cacheFile, "r")
    private var readPosition: Long = 0

    override fun open(dataSpec: DataSpec): Long {
        readPosition = dataSpec.position
        return C.LENGTH_UNSET.toLong() // Unknown length because it's a live stream
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        // Find out how much data is actually available in the file right now
        val available = manager.writePosition.get() - readPosition

        if (available <= 0) {
            return 0 // Tell ExoPlayer to wait for more data
        }

        // Read the data from the disk
        val bytesToRead = minOf(available.toInt(), length)
        fileReader.seek(readPosition)
        val bytesRead = fileReader.read(buffer, offset, bytesToRead)

        if (bytesRead > 0) {
            readPosition += bytesRead
            return bytesRead
        }
        return 0
    }

    override fun getUri(): Uri = Uri.fromFile(cacheFile)

    override fun close() {
        fileReader.close()
    }
}