package ai.os.cloneapp.clone_helper.AppTransffer.Apk

import android.content.Context
import android.util.Log
import com.your.app.transfer.backup.BackupDataCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

object TransferManager {

    private const val TAG = "TransferManager"
    const val PORT = 8888

    // ── SENDER side ───────────────────────────────────────────────────────────
    fun startSending(
        context: Context,
        goIpAddress: String,        // 192.168.49.1
        includeApks: Boolean = true,
        includeOwnData: Boolean = true,
        includeExternalData: Boolean = true,
        targetPackages: List<String>? = null, // null = all apps
        onProgress: (FileSender.SendProgress) -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val tempDir = File(context.cacheDir, "transfer_temp").also { it.mkdirs() }
            val allItems = mutableListOf<TransferItem>()

            // 1. Collect APKs
            if (includeApks) {
                val apks = if (targetPackages != null) {
                    targetPackages.mapNotNull {
                        ApkCollector.getApkForPackage(it)
                    }
                } else {
                    ApkCollector.collectUserApks(context)
                }
                allItems.addAll(apks)
                Log.d(TAG, "Collected ${apks.size} APKs")
            }

            // 2. Collect own app backup data
            if (includeOwnData) {
                val ownData = BackupDataCollector.collectOwnAppData(context, tempDir)
                allItems.addAll(ownData)
                Log.d(TAG, "Collected ${ownData.size} own data items")
            }

            // 3. Collect external data for target packages
            if (includeExternalData) {
                val packages = targetPackages
                    ?: context.packageManager
                        .getInstalledPackages(0)
                        .map { it.packageName }

                packages.forEach { pkg ->
                    val extData = BackupDataCollector
                        .collectExternalAppData(context, pkg, tempDir)
                    allItems.addAll(extData)
                }
                Log.d(TAG, "Collected external data for ${packages.size} packages")
            }

            Log.d(TAG, "Total items to send: ${allItems.size}")

            // 4. Send everything
            FileSender.sendItems(
                host = goIpAddress,
                port = PORT,
                items = allItems,
                onProgress = onProgress,
                onComplete = {
                    tempDir.deleteRecursively() // cleanup temp zips
                    onComplete()
                },
                onError = onError
            )
        }
    }

    // ── RECEIVER (Group Owner) side ───────────────────────────────────────────
    fun startReceiving(
        context: Context,
        onProgress: (FileReceiver.ReceiveProgress) -> Unit,
        onComplete: (apks: List<TransferItem>, backups: List<TransferItem>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val saveDir = File(context.getExternalFilesDir(null), "received").also {
            it.mkdirs()
        }

        CoroutineScope(Dispatchers.IO).launch {
            FileReceiver.startReceiving(
                context = context,
                port = PORT,
                saveDir = saveDir,
                onProgress = onProgress,
                onComplete = { items ->
                    val apks = items.filter { it.type == TransferType.APK }
                    val backups = items.filter {
                        it.type == TransferType.BACKUP_DATA ||
                        it.type == TransferType.EXTERNAL_DATA
                    }

                    Log.d(TAG, "Received ${apks.size} APKs, ${backups.size} backup items")

                    // Auto-install APKs
                    apks.forEach { item ->
                        FileReceiver.installApk(context, File(item.filePath))
                    }

                    // Restore backup data
                    FileReceiver.restoreBackupData(context, backups) { pkg ->
                        Log.d(TAG, "Restored: $pkg")
                    }

                    onComplete(apks, backups)
                },
                onError = onError
            )
        }
    }
}