package ai.os.cloneapp.clone_helper.AppTransffer

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.google.android.gms.auth.api.accounttransfer.AccountTransfer
import com.google.android.gms.auth.api.accounttransfer.AuthenticatorTransferCompletionStatus
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class ImportService : Service(){

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val TAG = "ImportService"
        const val ACCOUNT_TYPE = "com.your.app.account"
        const val TIMEOUT_SECONDS = 30L
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        retrieveAndRestoreAccounts()
        return START_NOT_STICKY
    }

    fun retrieveAndRestoreAccounts(){
        CoroutineScope(Dispatchers.IO).launch {
            try{
                val client = AccountTransfer.getAccountTransferClient(this@ImportService)

                // Retrieve data sent from old device
                val task = client.retrieveData(ACCOUNT_TYPE)
                val transferBytes = Tasks.await(task,TIMEOUT_SECONDS, TimeUnit.SECONDS)


                if(transferBytes != null && transferBytes.isNotEmpty()){

                    Log.d(TAG, "Received ${transferBytes.size} bytes")

                    // Deserialize and restore accounts
                    val restored = deserializeAndRestoreAccounts(transferBytes)

                    if(restored){
                        client.notifyCompletion(ACCOUNT_TYPE,
                            AuthenticatorTransferCompletionStatus.COMPLETED_SUCCESS)
                        Log.d(TAG, "Accounts restored successfully")
                    }else{
                        client.notifyCompletion(
                            ACCOUNT_TYPE,
                            AuthenticatorTransferCompletionStatus.COMPLETED_FAILURE
                        )
                    }
                }else{
                    Log.w(TAG, "No transfer data received")
                    client
                        .notifyCompletion(ACCOUNT_TYPE, AuthenticatorTransferCompletionStatus.COMPLETED_FAILURE)
                }
            }catch (e: Exception){
                Log.e(TAG,"Import failed: ${e.message}")
                AccountTransfer.getAccountTransferClient(this@ImportService)
                    .notifyCompletion(ACCOUNT_TYPE, AuthenticatorTransferCompletionStatus.COMPLETED_FAILURE)
            }finally {
                stopSelf()
            }
        }
    }


    private fun deserializeAndRestoreAccounts(bytes: ByteArray) : Boolean{
       return try{
            val accountManager = AccountManager.get(this)
            val jsonArray = JSONArray(String(bytes, Charsets.UTF_8))

            for(i in 0 until jsonArray.length()){

                val accountJson = jsonArray.getJSONObject(i)

                val name = accountJson.getString("name")
                val type = accountJson.getString("type")
                val authToken = accountJson.getString("authToken")
                val userData = accountJson.getJSONObject("userData")

                val account = Account(name, type)
                val added = accountManager.addAccountExplicitly(account, null,
                    Bundle().apply {
                        userData.keys().forEach { key ->
                            putString(key, userData.optString(key))
                        }
                    })

                if(added){
                    //set auth token
                    if(authToken.isNotEmpty()){
                        accountManager.setAuthToken(account, "default", authToken)
                    }
                    Log.d(TAG,"Restored account: $name")

                }else{
                    Log.w(TAG,"Account already exists or failed: $name")
                }
            }
           true
        }catch (e: Exception){
            false
        }

    }


}