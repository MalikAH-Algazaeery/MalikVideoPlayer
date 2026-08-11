package com.example.malikvideoplayer

import uniffi.my_multicast_test.MulticastReceiver
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

/**
 * This class manages the background work:
 * 1. Pulling data from the Rust Multicast Receiver.
 * 2. Writing it to a temporary Cache File for Seeking.
 * 3. Writing it to a permanent Recording File if requested.
 */
class MulticastStreamManager(private val receiver: MulticastReceiver, private val cacheFile: File) {

    private val fileWriter = RandomAccessFile(cacheFile, "rw")
    private var isRunning = true

    // Total bytes written to the cache file so far
    val writePosition = AtomicLong(0)

    // The permanent recording stream (can be null if not recording)
    @Volatile var recordStream: FileOutputStream? = null

    init {
        // Start the dedicated Network-to-Disk thread
        thread(name = "MulticastWorker") {
            try {
                while (isRunning) {
                    val data = receiver.readPacketsBatch()
                    if (data.isNotEmpty()) {
                        // 1. Write to the Time-Shift cache file
                        synchronized(fileWriter) {
                            fileWriter.seek(writePosition.get())
                            fileWriter.write(data)
                            writePosition.addAndGet(data.size.toLong())
                        }

                        // 2. Write to recording file if the user hit "REC"
                        recordStream?.write(data)
                    } else {
                        // Tiny sleep if no data to save battery
                        Thread.sleep(10)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Stop everything safely
    fun release() {
        isRunning = false
        fileWriter.close()
        recordStream?.close()
    }
}