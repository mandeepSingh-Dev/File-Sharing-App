package ai.os.cloneapp.clone_helper.AppTransffer.Apk

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.net.Socket

object FileSender {

    private const val TAG = "FileSender"
    private const val BUFFER_SIZE = 8192

    data class SendProgress(
        val currentFile: String,
        val fileIndex: Int,
        val totalFiles: Int,
        val bytesTransferred: Long,
        val totalBytes: Long
    ) {
        val overallPercent: Int get() =
            if (totalBytes == 0L) 0
            else ((bytesTransferred * 100) / totalBytes).toInt()
    }

    suspend fun sendItems(
        host: String,
        port: Int,
        items: List<TransferItem>,
        onProgress: (SendProgress) -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {

        val totalBytes = items.sumOf { it.fileSize }
        var totalBytesTransferred = 0L

        try {

            val socket = Socket(host, port)
            val dos = DataOutputStream(socket.getOutputStream())


            Log.d(TAG, "Connected to $host:$port")
            Log.d(TAG, "Sending ${items.size} items, total ${totalBytes} bytes")

            // ── Step 1: Send manifest (total count) ──────────────────────────
            dos.writeInt(items.size)
            dos.flush()

            // ── Step 2: Send each item ────────────────────────────────────────
            items.forEachIndexed { index, item ->
                val file = File(item.filePath)
                if (!file.exists()) {
                    Log.w(TAG, "File not found, skipping: ${item.filePath}")
                    // Send skip signal
                    dos.writeUTF("SKIP")
                    dos.flush()
                    return@forEachIndexed
                }

                Log.d(TAG, "Sending [${item.type}] ${item.fileName}")

                // Send metadata
                dos.writeUTF("FILE")
                dos.writeUTF(item.type.name)
                dos.writeUTF(item.packageName)
                dos.writeUTF(item.fileName)
                dos.writeLong(file.length())
                dos.flush()

                // Send file bytes
                var fileBytesTransferred = 0L
                FileInputStream(file).use { fis ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        dos.write(buffer, 0, bytesRead)
                        fileBytesTransferred += bytesRead
                        totalBytesTransferred += bytesRead

                        onProgress(
                            SendProgress(
                                currentFile = item.fileName,
                                fileIndex = index + 1,
                                totalFiles = items.size,
                                bytesTransferred = totalBytesTransferred,
                                totalBytes = totalBytes
                            )
                        )
                    }
                }
                dos.flush()
                Log.d(TAG, "✅ Sent: ${item.fileName} ($fileBytesTransferred bytes)")
            }

            // ── Step 3: Send END signal ───────────────────────────────────────
            dos.writeUTF("END")
            dos.flush()

            dos.close()
            socket.close()

            withContext(Dispatchers.Main) { onComplete() }
            Log.d(TAG, "Transfer complete")

        } catch (e: Exception) {
            Log.e(TAG, "Send failed: ${e.message}")
            withContext(Dispatchers.Main) { onError(e) }
        }
    }
}