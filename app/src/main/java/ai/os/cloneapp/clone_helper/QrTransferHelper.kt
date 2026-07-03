package ai.os.cloneapp.clone_helper

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject
import java.io.File
import java.net.ServerSocket
import java.net.Socket

object QrTransferHelper {

    private const val TAG = "QrTransferHelper"
    private const val PORT = 8988

    // -----------------------------------
    // 1. Generate QR JSON payload
    // -----------------------------------
    fun createQrPayload(
        ip: String,
        fileName: String = "notes.txt"
    ): String {
        return JSONObject().apply {
            put("ip", ip)
            put("port", PORT)
            put("fileName", fileName)
        }.toString()
    }

    // -----------------------------------
    // 2. Generate QR bitmap
    // -----------------------------------
    fun generateQrBitmap(
        data: String,
        size: Int = 600
    ): Bitmap {
        val matrix = QRCodeWriter().encode(
            data,
            BarcodeFormat.QR_CODE,
            size,
            size
        )

        val bitmap = Bitmap.createBitmap(
            size,
            size,
            Bitmap.Config.RGB_565
        )

        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x,
                    y,
                    if (matrix[x, y]) Color.BLACK
                    else Color.WHITE
                )
            }
        }

        return bitmap
    }

    // -----------------------------------
    // 3. Start QR scanner
    // -----------------------------------
    fun startQrScanner(activity: Activity) {
        IntentIntegrator(activity).initiateScan()
    }

    // -----------------------------------
    // 4. Start sender socket server
    // -----------------------------------
    fun sendTextFile(
        file: File,
        onConnected: (() -> Unit)? = null,
        onComplete: (() -> Unit)? = null
    ) {
        Thread {
            try {
                val serverSocket = ServerSocket(PORT)

                Log.d(TAG, "Waiting for receiver...")

                val socket = serverSocket.accept()

                onConnected?.invoke()

                file.inputStream().use { input ->
                    socket.getOutputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                socket.close()
                serverSocket.close()

                onComplete?.invoke()

                Log.d(TAG, "File sent successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Send failed", e)
            }
        }.start()
    }

    // -----------------------------------
    // 5. Parse QR data
    // -----------------------------------
    fun parseQrPayload(
        qrData: String
    ): Triple<String, Int, String> {

        val json = JSONObject(qrData)

        val ip = json.getString("ip")
        val port = json.getInt("port")
        val fileName = json.getString("fileName")

        return Triple(ip, port, fileName)
    }

    // -----------------------------------
    // 6. Receive text file
    // -----------------------------------
    fun receiveTextFile(
        context: Context,
        qrData: String,
        onReceived: (File) -> Unit
    ) {
        val (ip, port, fileName) = parseQrPayload(qrData)

        Thread {
            try {
                val socket = Socket(ip, port)

                val receivedFile = File(
                    context.filesDir,
                    "received_$fileName"
                )

                socket.getInputStream().use { input ->
                    receivedFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                socket.close()

                Log.d(TAG, "Received = ${receivedFile.absolutePath}")

                Handler(Looper.getMainLooper()).post {
                    onReceived(receivedFile)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Receive failed", e)
            }
        }.start()
    }
}