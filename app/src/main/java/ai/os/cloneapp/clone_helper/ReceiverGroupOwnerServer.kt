package ai.os.cloneapp.clone_helper

import ai.os.cloneapp.base.BaseApp
import android.provider.Settings
import android.util.JsonWriter
import android.util.Log
import androidx.compose.ui.res.dimensionResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.Writer
import java.net.ServerSocket
import java.net.Socket

class ReceiverGroupOwnerServer {

    val context= BaseApp.instance


        companion object {
            private const val PORT = 8888
            private const val TAG = "P2P_SERVER"
        }

    var isTranferActive = true

        fun startServer(onMessageReceived: (String) -> Unit) {
            Thread {
                try {
                    val serverSocket = ServerSocket(PORT)

                    Log.d(TAG, "Waiting for sender connection...")

                    val socket = serverSocket.accept()

                    Log.d(TAG, "Sender connected")

                    val reader = BufferedReader(
                        InputStreamReader(socket.getInputStream())
                    )

                    while(isTranferActive) {
                        val message = reader.readLine()

                        Log.d(TAG, "Received: $message")

                        onMessageReceived(message)
                    }
                    reader.close()
//                    socket.close()
//                    serverSocket.close()

                } catch (e: Exception) {
                    Log.e(TAG, "Server error: ${e.message}")
                }
            }.start()
        }


fun startCoroutinesServer(port: Int = 8888) {
    // We use a CoroutineScope tied to the Lifecycle of your Activity or ViewModel
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val serverSocket = ServerSocket(port)
            Log.d(TAG,"Server started on port $port, waiting for connection...")

            // accept() blocks until a client (Sender) connects
            val clientSocket = serverSocket.accept()
            Log.d(TAG,"Sender connected: ${clientSocket.inetAddress.hostAddress}")

            // Handle the actual data communication in a dedicated function
            handleClientCommunication(clientSocket)

        } catch (e: Exception) {
            Log.d(TAG,"Server Error: ${e.message}")
        }
    }
}

    val deviceSettingsHelper = DeviceSettingsHelper(context)

private suspend fun handleClientCommunication(socket: Socket) {
    withContext(Dispatchers.IO) {
        try {
//            val inputStream: InputStream = socket.getInputStream()
//            val buffer = ByteArray(4096)
//            var bytesRead: Int
//

            // This loop keeps the connection open
            // inputStream.read() returns -1 when the sender closes the socket
            while (socket.isConnected && !socket.isClosed) {

                val dIs = DataInputStream(socket.getInputStream())

                val size = dIs.readInt()

                val bArr = ByteArray(size = size)
                dIs.readFully(bArr)

                val str = String(bArr, Charsets.UTF_8)
                val jsonObj = JSONArray(str)

                Log.d("fklbknbjkfnbfk", jsonObj.toString())

                deviceSettingsHelper.setPrefSettings(jSONArray = jsonObj)

              /*  bytesRead = inputStream.read(buffer)

                if (bytesRead == -1) {
                    Log.d(TAG,"Sender disconnected gracefully.")
//                    break
                }

                val receivedData = String(buffer, 0, bytesRead)
                Log.d(TAG,"Received: $receivedData")

                val jsonArray = JSONArray(receivedData)
                jsonArray.toString().split("}")
                    .forEach {
                        Log.d("kfnvbkvkdnvd", it)
                    }

                // Switch to Main thread if you need to update UI
                withContext(Dispatchers.Main) {
                    // updateUi(receivedData)
                }*/
            }
        } catch (e: Exception) {
            Log.d(TAG,"Connection lost/Error: ${e.message}")
        } finally {
            socket.close()
            Log.d(TAG,"Socket resources cleared.")
        }
    }
}
}
