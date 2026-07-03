package com.your.app.transfer.backup

import ai.os.cloneapp.clone_helper.AppTransffer.Apk.TransferItem
import ai.os.cloneapp.clone_helper.AppTransffer.Apk.TransferType
import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BackupDataCollector {

    private const val TAG = "BackupDataCollector"

    // ── Collect YOUR OWN app's internal data ─────────────────────────────────
    fun collectOwnAppData(context: Context, outputDir: File): List<TransferItem> {
        val items = mutableListOf<TransferItem>()
        val packageName = context.packageName

        // 1. Zip SharedPreferences
        val prefsDir = File(context.dataDir, "shared_prefs")
        zipAndAdd(prefsDir, outputDir, packageName, "shared_prefs", items)

        // 2. Zip internal files
        zipAndAdd(context.filesDir, outputDir, packageName, "files", items)

        // 3. Zip databases
        val dbDir = File(context.dataDir, "databases")
        zipAndAdd(dbDir, outputDir, packageName, "databases", items)

        // 4. External files (no root needed)
        context.getExternalFilesDir(null)?.let { extDir ->
            zipAndAdd(extDir, outputDir, packageName, "external_files", items)
        }

        return items
    }

    // ── Collect EXTERNAL data of any app (no root needed) ────────────────────
    fun collectExternalAppData(
        context: Context,
        targetPackage: String,
        outputDir: File
    ): List<TransferItem> {
        val items = mutableListOf<TransferItem>()

        // External storage is accessible without root
        val externalBase = context.getExternalFilesDir(null)
            ?.parentFile?.parentFile  // /sdcard/Android/data/

        val targetExternal = File(externalBase, targetPackage)
        if (targetExternal.exists()) {
            zipAndAdd(targetExternal, outputDir, targetPackage, "external", items)
        }

        return items
    }

    // ── Collect data from /data/data/<pkg> (needs root or same UID) ──────────
    fun collectInternalDataIfAccessible(
        packageName: String,
        outputDir: File
    ): List<TransferItem> {
        val items = mutableListOf<TransferItem>()
        val internalDataDir = File("/data/data/$packageName")

        if (!internalDataDir.exists() || !internalDataDir.canRead()) {
            Log.w(TAG, "Cannot read $packageName internal data — needs root")
            return items
        }

        // SharedPreferences
        val prefsDir = File(internalDataDir, "shared_prefs")
        zipAndAdd(prefsDir, outputDir, packageName, "shared_prefs", items)

        // Files
        val filesDir = File(internalDataDir, "files")
        zipAndAdd(filesDir, outputDir, packageName, "files", items)

        // Databases
        val dbDir = File(internalDataDir, "databases")
        zipAndAdd(dbDir, outputDir, packageName, "databases", items)

        return items
    }

    // ── Helper: zip a folder and add as TransferItem ─────────────────────────
    private fun zipAndAdd(
        sourceDir: File,
        outputDir: File,
        packageName: String,
        label: String,
        items: MutableList<TransferItem>
    ) {
        if (!sourceDir.exists() || sourceDir.listFiles().isNullOrEmpty()) {
            Log.d(TAG, "Skipping $label — empty or missing")
            return
        }

        outputDir.mkdirs()
        val zipFile = File(outputDir, "${packageName}_${label}.zip")

        try {
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                sourceDir.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        val entryName = file.relativeTo(sourceDir).path
                        zos.putNextEntry(ZipEntry(entryName))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                        Log.d(TAG, "Zipped: $entryName")
                    }
            }

            items.add(
                TransferItem(
                    type = TransferType.BACKUP_DATA,
                    packageName = packageName,
                    fileName = zipFile.name,
                    filePath = zipFile.absolutePath,
                    fileSize = zipFile.length()
                )
            )
            Log.d(TAG, "Created zip: ${zipFile.name} (${zipFile.length()} bytes)")

        } catch (e: Exception) {
            Log.e(TAG, "Zip failed for $label: ${e.message}")
        }
    }

    // ── Unzip received backup data ────────────────────────────────────────────
    fun unzipBackupData(zipFile: File, targetDir: File) {
        try {
            java.util.zip.ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outFile = File(targetDir, entry.name)
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                    Log.d(TAG, "Unzipped: ${entry.name}")
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unzip failed: ${e.message}")
        }
    }
}