package ai.os.cloneapp.clone_helper.AppTransffer.Apk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log

class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )
        val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)

        when (status) {
            PackageInstaller.STATUS_SUCCESS ->
                Log.d("InstallReceiver", "✅ Installed: $packageName")
            PackageInstaller.STATUS_FAILURE_ABORTED ->
                Log.w("InstallReceiver", "Aborted: $packageName")
            else ->
                Log.e("InstallReceiver", "Failed status=$status pkg=$packageName")
        }
    }
}