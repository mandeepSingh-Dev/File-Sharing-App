package ai.os.cloneapp.clone_helper.AppTransffer.Apk

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.your.app.transfer.backup.BackupDataCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.ServerSocket

object FileReceiver {

    private const val TAG = "FileReceiver"
    private const val BUFFER_SIZE = 8192

    data class ReceiveProgress(
        val currentFile: String,
        val fileIndex: Int,
        val totalFiles: Int,
        val bytesReceived: Long,
        val totalBytes: Long
    ) {
        val percent: Int get() =
            if (totalBytes == 0L) 0
            else ((bytesReceived * 100) / totalBytes).toInt()
    }

    suspend fun startReceiving(
        context: Context,
        port: Int,
        saveDir: File,
        onProgress: (ReceiveProgress) -> Unit,
        onComplete: (List<TransferItem>) -> Unit,
        onError: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {

        try {
            val serverSocket = ServerSocket(port)
            Log.d(TAG, "Waiting on port $port...")

            val client = serverSocket.accept()
            Log.d(TAG, "Client connected: ${client.inetAddress.hostAddress}")

            val dis = DataInputStream(client.getInputStream())
            val receivedItems = mutableListOf<TransferItem>()

            // ── Step 1: Read total file count ─────────────────────────────────
            val totalFiles = dis.readInt()
            Log.d(TAG, "Expecting $totalFiles files")

            var totalBytesReceived = 0L
            var fileIndex = 0

            // ── Step 2: Read each file ────────────────────────────────────────
            while (true) {
                val signal = dis.readUTF()

                when (signal) {
                    "END" -> {
                        Log.d(TAG, "Received END signal")
                        break
                    }
                    "SKIP" -> {
                        Log.w(TAG, "Sender skipped a file")
                        fileIndex++
                        continue
                    }
                    "FILE" -> {
                        // Read metadata
                        val typeStr = dis.readUTF()
                        val packageName = dis.readUTF()
                        val fileName = dis.readUTF()
                        val fileSize = dis.readLong()
                        fileIndex++

                        Log.d(TAG, "Receiving [$typeStr] $fileName ($fileSize bytes)")

                        // Determine save path
                        val subDir = when (TransferType.valueOf(typeStr)) {
                            TransferType.APK -> File(saveDir, "apks")
                            TransferType.BACKUP_DATA -> File(saveDir, "backups/$packageName")
                            TransferType.EXTERNAL_DATA -> File(saveDir, "external/$packageName")
                        }
                        subDir.mkdirs()
                        val outFile = File(subDir, fileName)

                        // Read file bytes
                        var fileBytesReceived = 0L
                        FileOutputStream(outFile).use { fos ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            while (fileBytesReceived < fileSize) {
                                val toRead = minOf(
                                    BUFFER_SIZE.toLong(),
                                    fileSize - fileBytesReceived
                                ).toInt()
                                val bytesRead = dis.read(buffer, 0, toRead)
                                if (bytesRead == -1) break

                                fos.write(buffer, 0, bytesRead)
                                fileBytesReceived += bytesRead
                                totalBytesReceived += bytesRead

                                withContext(Dispatchers.Main) {
                                    onProgress(
                                        ReceiveProgress(
                                            currentFile = fileName,
                                            fileIndex = fileIndex,
                                            totalFiles = totalFiles,
                                            bytesReceived = totalBytesReceived,
                                            totalBytes = fileSize * totalFiles // estimate
                                        )
                                    )
                                }
                            }
                        }

                        Log.d(TAG, "✅ Saved: ${outFile.absolutePath}")

                        receivedItems.add(
                            TransferItem(
                                type = TransferType.valueOf(typeStr),
                                packageName = packageName,
                                fileName = fileName,
                                filePath = outFile.absolutePath,
                                fileSize = outFile.length()
                            )
                        )
                    }
                }
            }

            dis.close()
            client.close()
            serverSocket.close()

            // ── Step 3: Process received files ────────────────────────────────
            withContext(Dispatchers.Main) {
                onComplete(receivedItems)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Receive failed: ${e.message}")
            withContext(Dispatchers.Main) { onError(e) }
        }
    }

    // ── Install received APK ──────────────────────────────────────────────────
    fun installApk(context: Context, apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            installApkViaPackageInstaller(context, apkFile)
        } else {
            installApkViaIntent(context, apkFile)
        }
    }

    // Silently install (needs INSTALL_PACKAGES permission — privileged only)
    private fun installApkViaPackageInstaller(context: Context, apkFile: File) {
        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        )

        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        try {
            session.openWrite(apkFile.name, 0, apkFile.length()).use { out ->
                apkFile.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }

            val intent = Intent(context, InstallReceiver::class.java)
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context, sessionId, intent,
                android.app.PendingIntent.FLAG_IMMUTABLE
            )

            session.commit(pendingIntent.intentSender)
            Log.d(TAG, "APK install committed: ${apkFile.name}")

        } catch (e: Exception) {
            session.abandon()
            Log.e(TAG, "Install failed: ${e.message}")
        }
    }

    // Fallback: show system install dialog
    private fun installApkViaIntent(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    // ── Restore received backup zips ──────────────────────────────────────────
    fun restoreBackupData(
        context: Context,
        backupItems: List<TransferItem>,
        onRestored: (packageName: String) -> Unit
    ) {
        backupItems
            .filter { it.type == TransferType.BACKUP_DATA }
            .groupBy { it.packageName }
            .forEach { (packageName, items) ->
                items.forEach { item ->
                    val zipFile = File(item.filePath)
                    val isOwnApp = packageName == context.packageName

                    if (isOwnApp) {
                        // Restore to our own app's directories
                        val label = item.fileName
                            .removePrefix("${packageName}_")
                            .removeSuffix(".zip")

                        val targetDir = when {
                            label.contains("shared_prefs") ->
                                File(context.dataDir, "shared_prefs")
                            label.contains("databases") ->
                                File(context.dataDir, "databases")
                            else -> context.filesDir
                        }
                        BackupDataCollector.unzipBackupData(zipFile, targetDir)
                        Log.d(TAG, "Restored own app data: $label")

                    } else {
                        // For other apps: unzip to external storage
                        // (internal data restore needs root)
                        val extTarget = context.getExternalFilesDir(null)
                            ?.let { File(it.parentFile?.parentFile, packageName) }
                        extTarget?.let {
                            BackupDataCollector.unzipBackupData(zipFile, it)
                            Log.d(TAG, "Restored external data for: $packageName")
                        }
                    }
                }
                onRestored(packageName)
            }
    }
}