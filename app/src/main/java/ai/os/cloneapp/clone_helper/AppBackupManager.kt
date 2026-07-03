package ai.os.cloneapp.clone_helper

import ai.os.cloneapp.base.BaseApp
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object AppBackupManager {

    val context : Context = BaseApp.instance

    val pm : PackageManager
        get() = context.packageManager

    init {

        getAllApps()
    }

    fun createAppBackup(packageName: String): File? {
        return try {
            val sourceRoot = File("/data/user/0/$packageName")

            if (!sourceRoot.exists()) {
                Log.e("BACKUP", "Package folder not found")
                return null
            }

            val tempBackupDir = File("/data/local/tmp/clone_backup/$packageName")
            if (tempBackupDir.exists()) {
                tempBackupDir.deleteRecursively()
            }
            tempBackupDir.mkdirs()

            // Copy selected folders
            copyFolderIfExists(
                File(sourceRoot, "shared_prefs"),
                File(tempBackupDir, "shared_prefs")
            )

            copyFolderIfExists(
                File(sourceRoot, "databases"),
                File(tempBackupDir, "databases")
            )

            copyFolderIfExists(
                File(sourceRoot, "files"),
                File(tempBackupDir, "files")
            )

            // Create final zip
            val zipFile = File("/data/local/tmp/${packageName}_backup.zip")
            zipFolder(tempBackupDir, zipFile)

            Log.d("BACKUP", "Zip created at ${zipFile.absolutePath}")

            zipFile

        } catch (e: Exception) {
            Log.d("fkbnfkbnfkbf", e.message.toString())
            e.printStackTrace()
            null
        }
    }

    private fun copyFolderIfExists(source: File, target: File) {
        if (!source.exists()) return

        source.copyRecursively(target, overwrite = true)

        Log.d("BACKUP", "Copied ${source.absolutePath}")
    }

    private fun zipFolder(sourceDir: File, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            sourceDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = file.relativeTo(sourceDir).path

                    zipOut.putNextEntry(ZipEntry(entryName))

                    FileInputStream(file).use { input ->
                        input.copyTo(zipOut)
                    }

                    zipOut.closeEntry()
                }
            }
        }
    }

    fun getAllApps(): List<ApplicationInfo?> {
        return pm.getInstalledApplications(0).filter {
            Log.d("All_App_packages_Log", it.packageName.toString())
            (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        }
    }
}