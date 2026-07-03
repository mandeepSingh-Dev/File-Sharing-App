package ai.os.cloneapp.clone_helper.AppTransffer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ExportBroadcastReceiver : BroadcastReceiver(){

    companion object{
        const val TAG = "ExportBroadcastReceiver"
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Action: ${intent?.action}")

        intent?.action ?: return

        when(intent.action){
            // Step 1: Device Setup app tells us to start exporting
            "com.google.android.gms.auth.START_ACCOUNT_EXPORT" -> {
                Log.d("ExportReceiver", "Start export signal received")
                // Start our export service
                val serviceIntent = Intent(context, ExportService::class.java).apply { action = AccountTransferConstants.ACTION_EXPORT }
                context?.startService(serviceIntent)
            }
            // Step 2: New device is ready — send the data
            "com.google.android.gms.auth.ACCOUNT_EXPORT_DATA_AVAILABLE" -> {
                Log.d("ExportReceiver", "New device ready — sending data")
                val serviceIntent = Intent(context, ExportService::class.java).apply {
                    action = AccountTransferConstants.ACTION_SEND_DATA
                }
                context?.startService(serviceIntent)
            }
        }

    }
}