package ai.os.cloneapp.clone_helper.AppTransffer

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log

class AccountTransferService : Service() {

    private lateinit var authenticator: CloneAppAuthenticator

    override fun onCreate() {
        super.onCreate()
        Log.d("AccountTransferService", "Service created")
        authenticator = CloneAppAuthenticator(this)
    }

    // GMS calls this to get the authenticator binder
    // Must return authenticator.iBinder — nothing else
    override fun onBind(intent: Intent?): IBinder {
        return authenticator.iBinder
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Minimal Authenticator
// We don't need login/signup — just needs to exist so GMS can
// use it as a trusted channel for sendData() / retrieveData()
// ─────────────────────────────────────────────────────────────────────────────
class CloneAppAuthenticator(context: Context)
    : AbstractAccountAuthenticator(context) {

    // Not used — we don't add accounts manually
    override fun addAccount(
        response: AccountAuthenticatorResponse,
        accountType: String,
        authTokenType: String?,
        requiredFeatures: Array<out String>?,
        options: Bundle?
    ): Bundle = Bundle().apply {
        putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION)
        putString(AccountManager.KEY_ERROR_MESSAGE, "addAccount not supported")
    }

    // Not used — we don't manage tokens
    override fun getAuthToken(
        response: AccountAuthenticatorResponse,
        account: Account,
        authTokenType: String,
        options: Bundle?
    ): Bundle = Bundle().apply {
        putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION)
        putString(AccountManager.KEY_ERROR_MESSAGE, "getAuthToken not supported")
    }

    // Not used
    override fun confirmCredentials(
        response: AccountAuthenticatorResponse,
        account: Account,
        options: Bundle?
    ): Bundle? = null

    // Not used
    override fun editProperties(
        response: AccountAuthenticatorResponse,
        accountType: String
    ): Bundle? = null

    // Not used
    override fun getAuthTokenLabel(authTokenType: String): String = ""

    // Not used
    override fun updateCredentials(
        response: AccountAuthenticatorResponse,
        account: Account,
        authTokenType: String?,
        options: Bundle?
    ): Bundle? = null

    // Not used
    override fun hasFeatures(
        response: AccountAuthenticatorResponse,
        account: Account,
        features: Array<out String>
    ): Bundle = Bundle().apply {
        putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false)
    }
}