package ai.os.cloneapp.utils

import com.google.gson.Gson


 fun  Any?.toGson() : String? = try{
        Gson().toJson(this)
    }catch (e: Exception){
        null
    }

inline fun <reified T> String?.fromJsn()  = try{
    Gson().fromJson(this,T::class.java)
}catch (e: Exception){
    null
}
