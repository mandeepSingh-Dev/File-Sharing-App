package ai.os.cloneapp.clone_helper.AppTransffer

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.android.gms.auth.api.accounttransfer.AccountTransfer
import com.google.android.gms.auth.api.accounttransfer.AuthenticatorTransferCompletionStatus
import com.google.android.gms.tasks.Tasks
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.Authenticator
import java.util.concurrent.TimeUnit

class ExportService : Service() {

    companion object {
        const val TAG = "ExportService"
        const val ACCOUNT_TYPE = "ai.os.cloneapp.account"
        const val TIMEOUT_SECONDS = 30L
    }


    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            AccountTransferConstants.ACTION_EXPORT -> handleExport()
            AccountTransferConstants.ACTION_SEND_DATA -> sendAccountData()
        }
        return START_NOT_STICKY
    }

    private fun handleExport() {
        Log.d(TAG, "No accounts to export")

        val accountManager = AccountManager.get(this)
        val accounts = accountManager.getAccountsByType(ACCOUNT_TYPE)

        if (accounts.isEmpty()) {
            Log.w(TAG, "No accounts to export")
            return
        }

        val exportData = serializeAccounts(accountManager = accountManager, accounts = accounts)
        Log.d(TAG, "Prepared ${accounts.size} accounts for export")


        // Send to new device via Account Transfer API
        sendDataToNewDevice(exportData, context = this)
    }

    fun sendAccountData(){
     Log.d(TAG,"Sending account data to new device")
     val accountManager = AccountManager.get(this)
val accounts =  accountManager.getAccountsByType(ACCOUNT_TYPE)
val exportData = serializeAccounts(accountManager, accounts)
        sendDataToNewDevice(exportData, this)
    }

    fun sendDataToNewDevice(exportData : ByteArray,
                            context: Context
    ){
        CoroutineScope(Dispatchers.IO).launch {
            try{
                val client = AccountTransfer.getAccountTransferClient(context)

                // This sends data through Device Setup app's secure channel
                val task = client.sendData(ACCOUNT_TYPE, exportData)


                Tasks.await(task, TIMEOUT_SECONDS, TimeUnit.SECONDS)

                // Notify GMS that export completed successfully

                client.notifyCompletion(ACCOUNT_TYPE, AuthenticatorTransferCompletionStatus.COMPLETED_SUCCESS)

                Log.d(TAG, "Account data sent successfully")
            }catch (e: Exception){
                Log.e(TAG,"Export failed: ${e.message}")

                // Notify GMS that export failed
                AccountTransfer.getAccountTransferClient(context).notifyCompletion(ACCOUNT_TYPE, AuthenticatorTransferCompletionStatus.COMPLETED_FAILURE)
            }finally {
                stopSelf()
            }
        }
    }

    private fun serializeAccounts(
        accountManager: AccountManager,
        accounts: Array<Account>
    ): ByteArray {

        val jsonArray = JSONArray()

        accounts.forEach { account ->
            val accountJson = JSONObject().apply {
                put("name", account.name)
                put("type", account.type)


                try {
                    val tokenFuture =
                        accountManager.getAuthToken(account, "default", null, false, null, null)
                    val bundle = tokenFuture.result

                    val token = bundle.getString(AccountManager.KEY_AUTHTOKEN)

                    put("authToken", token ?: "")

                } catch (e: Exception) {
                    put("authToken", "")
                }

            val userData = JSONObject()

            try{
                accountManager.getUserData(account,"userId")?.let { userData.put("userId", it) }
                accountManager.getUserData(account,"username")?.let { userData.put("username", it) }
                accountManager.getUserData(account,"email")?.let { userData.put("email", it) }
            }catch (e: Exception){
                put("userData", userData)
            }
            }
            jsonArray.put(accountJson
            )
        }

        return jsonArray.toString().toByteArray(Charsets.UTF_8)
    }
}