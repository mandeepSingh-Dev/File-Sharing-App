package ai.os.cloneapp.clone_helper.AppTransffer.Apk

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File

class AppBackupAgent : BackupAgent(){

    companion object{
        const val TAG = "AppBackupAgent"
        const val KEY_PREFS = "shared_prefs"
        const val KEY_FILES = "internal_files"
        const val KEY_DB = "databases"
    }



    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?
    ) {

        Log.d(TAG, "onBackup Called")

        val prefsDir = File(dataDir,"shared_prefs")

        if(prefsDir.exists()){
            prefsDir.listFiles()?.forEach { prefsFile ->
                Log.d("dkvnjkbnfjk", prefsFile.name.toString())
            }
        }
    }

    override fun onRestore(
        data: BackupDataInput,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?
    ) {

        Log.d(TAG, "onRestore called")

        while(data.readNextHeader()){
            val key = data.key
            val size = data.dataSize
            val bytes = ByteArray(size)
            data.readEntityData(bytes,0, size)

            try{
                when{
                    key.startsWith(KEY_PREFS) -> {
                        val fileName = key.removePrefix("$KEY_PREFS/")
                        val prefsDir = File(dataDir,KEY_PREFS)
                        prefsDir.mkdirs()
                        File(prefsDir, fileName).writeBytes(bytes)
                    }
                    key.startsWith(KEY_FILES) -> {
                        val relativePath = key.removePrefix("$KEY_FILES/")
                        val targetFile = File(filesDir, relativePath)
                        Log.d(TAG, "Target File: ${targetFile.name} , parent: ${targetFile.parentFile?.name}")

                        targetFile.parentFile?.mkdirs()
                        targetFile.writeBytes(bytes)

                        Log.d(TAG, "Restored file: $relativePath")
                    }
                    key.startsWith(KEY_DB) -> {

                        val dbName = key.removePrefix("$KEY_DB/")

                        val dbDir = File(dataDir, KEY_DB)

                        dbDir.mkdirs()

                        File(dbDir, dbName).writeBytes(bytes)

                        Log.d(TAG, "Restored db: $dbName")
                    }

                }
            }catch (e: Exception){
                Log.e(TAG, "Restore failed for $key: ${e.message}")
            }


        }
    }

    override fun onFullBackup(data: FullBackupDataOutput?) {
        Log.d(TAG, "onFullBackup called")

        val prefsDir = File(dataDir, KEY_PREFS)

        if(prefsDir.exists()){
            prefsDir.listFiles()?.forEach { file ->
                fullBackupFile(file,data)
            }
        }

        filesDir?.walkTopDown()?.filter { it.isFile }?.forEach { file ->
            fullBackupFile(file, data)
        }

        val dbDir = File(dataDir, KEY_DB)
        if(dbDir.exists()){
            dbDir.listFiles()
                ?.filter { !it.name.endsWith("-journal") }
                ?.forEach { file -> fullBackupFile(file, data) }

        }

    }






    private fun writeFileToBackUp(data : BackupDataOutput, key : String, file : File){
        try{
            val bytes = file.readBytes()

            data.writeEntityHeader(key, bytes.size)
            data.writeEntityData(bytes, bytes.size)
            Log.d(TAG, "Backed up: $key (${bytes.size} bytes)")
        }catch (e: Exception){
            Log.e(TAG, "Failed to backup $key: ${e.message}")
        }
    }

    override fun onRestoreFile(
        data: ParcelFileDescriptor,
        size: Long,
        destination: File,
        type: Int,
        mode: Long,
        mtime: Long
    ) {
        Log.d(TAG, "onRestoreFile: ${destination.absolutePath}")
        destination.parentFile?.mkdirs()
        super.onRestoreFile(data, size, destination, type, mode, mtime)
    }



}


