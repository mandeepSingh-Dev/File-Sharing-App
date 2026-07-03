package ai.os.cloneapp.clone_helper.AppTransffer.Apk

import ai.os.cloneapp.base.BaseApp
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import java.io.File
import java.io.Serializable


enum class TransferType {
    APK,
    BACKUP_DATA,
    EXTERNAL_DATA
}

data class TransferItem(
    val type: TransferType,
    val packageName: String,
    val fileName: String,
    val filePath: String,
    val fileSize: Long
) : Serializable

object ApkCollector {

    val context = BaseApp.instance

    val packageManager : PackageManager
        get() = context.packageManager

    fun collectUserApks(context : Context): MutableList<TransferItem> {

        val items = mutableListOf<TransferItem>()

        packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { applicationInfo ->
                applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
            }
            .forEach { applicationInfo ->
                val baseApk = File(applicationInfo.sourceDir)
                if(baseApk.exists()){
                    items.add(
                        TransferItem(
                            type = TransferType.APK,
                            packageName = applicationInfo.packageName,
                            fileName = "${applicationInfo.packageName}.apk",
                            filePath = baseApk.absolutePath,
                            fileSize = baseApk.length()
                        )
                    )
                }


                applicationInfo.splitPublicSourceDirs?.forEachIndexed { index, splitPath ->
                    val splitFile = File(splitPath)

                    if(splitFile.exists()){
                        items.add(
                            TransferItem(
                                type = TransferType.APK,
                                packageName = applicationInfo.packageName,
                                fileName = "${applicationInfo.packageName}_split_$index.apk",
                                filePath = splitPath,
                                fileSize = splitFile.length()
                            )
                        )
                    }

                }
            }

        return items
    }

    fun getApkForPackage(packageName: String): TransferItem? {
        return try{
            val appInfo = packageManager.getApplicationInfo(packageName,0)

            val file = File(appInfo.sourceDir)

            TransferItem(
                type = TransferType.APK,
                packageName = packageName,
                fileName = "$packageName.apk",
                filePath = file.absolutePath,
                fileSize = file.length()
            )

        }catch (e: Exception){
null
        }
    }

}