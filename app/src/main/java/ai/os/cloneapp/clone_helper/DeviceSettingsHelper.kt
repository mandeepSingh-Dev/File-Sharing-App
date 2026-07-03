package ai.os.cloneapp.clone_helper

import ai.os.cloneapp.utils.fromJsn
import ai.os.cloneapp.utils.toGson
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.xml.namespace.QName

enum class SettingsType{
    SYSTEM,
    SECURE,
    GLOBAL
}

data class SettingItem(
    val settingType : SettingsType?,
    val name : String?,
    val value : Any?
)

class DeviceSettingsHelper(private val context : Context) {


    val contentResolver: ContentResolver by lazy {
        context.contentResolver
    }

    suspend fun getAllSettings() : JSONArray = withContext(Dispatchers.IO){



        val jSONArray = JSONArray()

            val r = Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 216)
            Log.d("fkbfkbnfjk", r.toString())

            val cursorSystem = contentResolver.query(
                Settings.System.CONTENT_URI,
                arrayOf("name", "value"),
                null,
                null,
                null
            )

            val cursorGlobal = contentResolver.query(
                Settings.Global.CONTENT_URI,
                arrayOf("name", "value"),
                null,
                null,
                null
            )

            val cursorSecure = contentResolver.query(
                Settings.Secure.CONTENT_URI,
                arrayOf("name", "value"),
                null,
                null,
                null
            )

       val finalList =  getCursorData(cursor = cursorSystem, SettingsType.SYSTEM) + getCursorData(cursor = cursorGlobal, SettingsType.GLOBAL) + getCursorData(cursor = cursorSecure, SettingsType.SECURE)
        finalList.forEach {
            jSONArray.put(it)
        }
        return@withContext jSONArray
        }

    fun getCursorData(cursor : Cursor?, settingType: SettingsType?): List<String?> {
        val itemsList = mutableListOf<String?>()
        cursor?.use {
            val nameColumn = it.getColumnIndex("name")
            val valueColumn = it.getColumnIndex("value")

            while (it.moveToNext()) {
                val name = it.getString(nameColumn)
                val value = it.getString(valueColumn)

                val settingItem = SettingItem(
                    settingType = settingType,
                    name = name,
                    value = value
                )
                    itemsList.add(settingItem.toGson())
                Log.d("Fbmfkbnfbfn", "$name $value")
            }
        }
        return itemsList.toList()
    }

    companion object{
        const val TAG = "DeviceSettingsHelper"
    }

fun setPrefSettings(jSONArray : JSONArray){

    val length = jSONArray.length()
    for(i in 0 until length){
        try {

            val settingsItem = jSONArray.get(i).toString().fromJsn<SettingItem>()

            Log.d("kfnbjfnbjf", settingsItem.toString())

            when(settingsItem?.settingType){
                SettingsType.SYSTEM -> setSystemSettingValue(settingsItem.name, settingsItem.value)
                SettingsType.GLOBAL -> setGlobalSettingValue(settingsItem.name, settingsItem.value)
                SettingsType.SECURE -> setSecureSettingValue(settingsItem.name, settingsItem.value)
                else -> Unit
            }

            Log.d("SUCCESS_KEY", settingsItem?.name.toString())

        }catch (e: Exception){
            Log.d(TAG,"Set Settings error: ${e.message.toString()}")
        }
    }
}

    fun setSystemSettingValue(name : String? , value : Any?){
        when (value) {
            is Int -> {
                Settings.System.putInt(contentResolver, name, value)
            }

            is String -> {
                Settings.System.putString(contentResolver, name, value)
            }

            is Float -> {
                Settings.System.putFloat(contentResolver, name, value)
            }

            is Long -> {
                Settings.System.putLong(contentResolver, name, value)
            }
        }
    }
    fun setGlobalSettingValue(name : String ?, value : Any?){
        when (value) {
            is Int -> {
                Settings.System.putInt(contentResolver, name, value)
            }

            is String -> {
                Settings.System.putString(contentResolver, name, value)
            }

            is Float -> {
                Settings.System.putFloat(contentResolver, name, value)
            }

            is Long -> {
                Settings.System.putLong(contentResolver, name, value)
            }
        }
    }
    fun setSecureSettingValue(name : String? , value : Any?){
        when (value) {
            is Int -> {
                Settings.System.putInt(contentResolver, name, value)
            }

            is String -> {
                Settings.System.putString(contentResolver, name, value)
            }

            is Float -> {
                Settings.System.putFloat(contentResolver, name, value)
            }

            is Long -> {
                Settings.System.putLong(contentResolver, name, value)
            }
        }
    }
}
