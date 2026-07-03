package ai.os.cloneapp.clone_helper

import android.provider.Settings
import android.util.JsonWriter
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket

enum class PacketType{
    TXT,
    JSON,
    FILE,
    ZIP,
}

class SenderServer(val groupOwnerIp : String) {


        companion object {
            private const val PORT = 8888
            private const val TAG = "P2P_CLIENT"
        }

    var socket : Socket?=null
    init {

    }


        fun sendMessage(
            message: String
        ) {

            Thread {

                if(socket == null){
                        socket = Socket(groupOwnerIp, PORT)
                }
                if(socket?.isConnected == true) {
                    try {
                        val writer = BufferedWriter(
                            OutputStreamWriter(socket!!.getOutputStream())
                        )


                        writer.write(message)

                        writer.flush()
                        Log.d(TAG, "Message sent")

                        writer.close()
                        socket?.close()

                    } catch (e: Exception) {
                        Log.e(TAG, "Client error: ${e.message}")
                    }
                }else{
                    Log.d(TAG,"Socket not connected")
                }
            }.start()

        }


    fun setJsonsTREAM(
        dos : DataOutputStream,
        json : String){

        val byteArr = json.toByteArray()

        dos.writeInt(byteArr.size)
        dos.write(byteArr)
        dos.flush()
    }


    fun sendData(data : String){
        CoroutineScope(Dispatchers.IO).launch {
            if (socket == null) {
                socket = Socket(groupOwnerIp, PORT)
            }

            if (socket?.isConnected == true) {
                val dos = DataOutputStream(socket?.getOutputStream())
                setJsonsTREAM(dos, json = data)

            } else {
                Log.d(TAG, "Socket not connected: Sender side")
            }
        }
    }

    fun sendFile(){


        try {
            val writer = BufferedWriter(
                OutputStreamWriter(socket!!.getOutputStream())
            )


        }catch (e: Exception){
            Log.d(TAG,e.message.toString())
        }




    }





    }
