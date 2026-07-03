package ai.os.cloneapp.clone_helper

import ai.os.cloneapp.base.BaseApp
import android.adservices.ondevicepersonalization.AppInfo
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import java.io.File

class AppScanner() {
    private val context: Context = BaseApp.instance
    private val packageManager = context.packageManager

    // ─── Get ALL installed apps ───────────────────────────────
    fun getAllApps(): List<ai.os.cloneapp.clone_helper.AppInfo> {
        val flags = PackageManager.GET_META_DATA or
                    PackageManager.GET_PERMISSIONS or
                    PackageManager.GET_SIGNING_CERTIFICATES

        return packageManager
            .getInstalledPackages(flags)
            .map { buildAppInfo(it) }
    }

    // ─── Get only USER installed apps ─────────────────────────
    fun getUserApps(): List<ai.os.cloneapp.clone_helper.AppInfo> {
        return getAllApps().filter { it.isUserApp }
    }

    // ─── Get only SYSTEM apps ─────────────────────────────────
    fun getSystemApps(): List<ai.os.cloneapp.clone_helper.AppInfo> {
        return getAllApps().filter { it.isSystemApp }
    }

    // ─── Get only BACKUPABLE apps ──────────────────────────────
    fun getBackupableApps(): List<ai.os.cloneapp.clone_helper.AppInfo> {
        return getAllApps().filter { it.isBackupSupported }
    }

    // ─── Get only NON-BACKUPABLE apps ─────────────────────────
    fun getNonBackupableApps(): List<ai.os.cloneapp.clone_helper.AppInfo> {
        return getAllApps().filter { !it.isBackupSupported }
    }

    // ─── Build AppInfo from PackageInfo ───────────────────────
    private fun buildAppInfo(pkg: PackageInfo): ai.os.cloneapp.clone_helper.AppInfo {
        val appInfo = pkg.applicationInfo!!
        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val allowBackup = (appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
        val hasBackupAgent = appInfo.backupAgentName != null

        return AppInfo(
            packageName     = pkg.packageName,
            appName         = packageManager.getApplicationLabel(appInfo).toString(),
            versionName     = pkg.versionName ?: "Unknown",
            versionCode     = PackageInfoCompat.getLongVersionCode(pkg),
            apkPath         = appInfo.sourceDir,
            installTime     = pkg.firstInstallTime,
            updateTime      = pkg.lastUpdateTime,
            appSize         = getAppSize(appInfo),
            isSystemApp     = isSystem,
            isUserApp       = !isSystem,
            allowBackup     = allowBackup,
            hasBackupAgent  = hasBackupAgent,
            backupAgentName = appInfo.backupAgentName,
            isBackupSupported = allowBackup,   // core check
            icon            = packageManager.getApplicationIcon(appInfo),
            splitApkPaths   = appInfo.splitSourceDirs?.toList() ?: emptyList(),
            dataDir         = appInfo.dataDir,
            targetSdkVersion = appInfo.targetSdkVersion,
            minSdkVersion   = appInfo.minSdkVersion,
        )
    }

    // ─── Get app size ─────────────────────────────────────────
    private fun getAppSize(appInfo: ApplicationInfo): Long {
        return try {
            File(appInfo.sourceDir).length()
        } catch (e: Exception) {
            0L
        }
    }

    // ─── Copy APK to your app's cache ─────────────────────────
    fun copyApkToCache(appInfo: ai.os.cloneapp.clone_helper.AppInfo): File? {
        return try {
            val sourceFile = File(appInfo.apkPath)
            val destFile = File(
                context.cacheDir,
                "${appInfo.packageName}.apk"
            )
            sourceFile.copyTo(destFile, overwrite = true)
            destFile
        } catch (e: Exception) {
            Log.e("AppScanner", "APK copy failed: ${e.message}")
            null
        }
    }

    // ─── Copy Split APKs ──────────────────────────────────────
    fun copySplitApksToCache(appInfo: ai.os.cloneapp.clone_helper.AppInfo): List<File> {
        val files = mutableListOf<File>()

        // base apk
        copyApkToCache(appInfo)?.let { files.add(it) }

        // split apks
        appInfo.splitApkPaths.forEachIndexed { index, path ->
            try {
                val sourceFile = File(path)
                val destFile = File(
                    context.cacheDir,
                    "${appInfo.packageName}_split_$index.apk"
                )
                sourceFile.copyTo(destFile, overwrite = true)
                files.add(destFile)
            } catch (e: Exception) {
                Log.e("AppScanner", "Split APK copy failed: ${e.message}")
            }
        }

        return files
    }

    // ─── Check backup status detail ───────────────────────────
    fun getBackupStatus(packageName: String): BackupStatus {
        val pkg = try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            return BackupStatus.NOT_FOUND
        }

        val appInfo = pkg.applicationInfo!!
        val allowBackup = (appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
        val hasBackupAgent = appInfo.backupAgentName != null

        return when {
            !allowBackup             -> BackupStatus.NOT_ALLOWED
            hasBackupAgent           -> BackupStatus.CUSTOM_AGENT
            allowBackup              -> BackupStatus.FULL_BACKUP
            else                     -> BackupStatus.UNKNOWN
        }
    }

    // ─── Get single app info ──────────────────────────────────
    fun getAppInfo(packageName: String): ai.os.cloneapp.clone_helper.AppInfo? {
        return try {
            val pkg = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_META_DATA
            )
            buildAppInfo(pkg)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val apkPath: String,
    val installTime: Long,
    val updateTime: Long,
    val appSize: Long,
    val isSystemApp: Boolean,
    val isUserApp: Boolean,
    val allowBackup: Boolean,
    val hasBackupAgent: Boolean,
    val backupAgentName: String?,
    val isBackupSupported: Boolean,
    val icon: Drawable?,
    val splitApkPaths: List<String>,    // for split apks
    val dataDir: String,
    val targetSdkVersion: Int,
    val minSdkVersion: Int,
){

    override fun toString(): String {
        return "PackageName: $packageName \n" +
                "   appName  ->  $appName\n" +
                "   versionName  ->  $versionName\n" +
                "   versionCode  ->  $versionCode\n" +
                "   apkPath  ->  $apkPath\n" +
                "   installTime  ->  $installTime\n" +
                "   updateTime  ->  $updateTime\n" +
                "   appSize  ->  $appSize\n" +
                "   isSystemApp  ->  $isSystemApp\n" +
                "   isUserApp  ->  $isUserApp\n" +
                "   allowBackup  -> $allowBackup\n" +
                "   hasBackupAgent  ->  $hasBackupAgent\n" +
                "   backupAgentName  ->  $backupAgentName\n" +
                "   isBackupSupported  ->  $isBackupSupported\n" +
                "   icon  ->  $icon\n" +
                "   splitApkPaths  ->  $splitApkPaths\n" +
                "   dataDir ->  $dataDir\n" +
                "   targetSdkVersion  ->  $targetSdkVersion\n" +
                "   minSdkVersion  ->  $minSdkVersion\n"
    }
}

enum class BackupStatus {
    FULL_BACKUP,      // allowBackup=true, no custom agent → OS backs up everything
    CUSTOM_AGENT,     // allowBackup=true, has BackupAgent → app controls what's backed up
    NOT_ALLOWED,      // allowBackup=false → cannot backup at all
    NOT_FOUND,        // package not found
    UNKNOWN
}