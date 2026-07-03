package ai.os.cloneapp.clone_helper.AppTransffer.Apk

import ai.os.cloneapp.clone_helper.AppTransffer.ImportService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ImportBroadcastReceiver : BroadcastReceiver(){

    companion object{
        const val TAG = "ImportBroadcastReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Action: ${intent?.action}")
        val action = intent?.action ?: return

        if (intent.action == "com.google.android.gms.auth.ACCOUNT_IMPORT_DATA_AVAILABLE") {
            Log.d("ImportReceiver", "Account data available — starting import")

            // Start import service
            val serviceIntent = Intent(context, ImportService::class.java)
            context?.startService(serviceIntent)
        }
    }

}