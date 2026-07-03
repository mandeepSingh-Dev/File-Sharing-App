package ai.os.cloneapp.clone_helper
import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanOptions

class QrCodeGenerator {


    fun generateQrCode(ssid: String, password: String): Bitmap {

        val qrContent = """
        {
          "ssid":"$ssid",
          "password":"$password"
        }
    """.trimIndent()

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(
            qrContent,
            BarcodeFormat.QR_CODE,
            512,
            512
        )

        val width = bitMatrix.width
        val height = bitMatrix.height

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK
                    else android.graphics.Color.WHITE
                )
            }
        }

        return bitmap
    }


    fun startQrScanner(barcodeLauncher : ActivityResultLauncher<ScanOptions>) {
        val options = ScanOptions()
        options.setPrompt("Scan hotspot QR")
        options.setBeepEnabled(true)
        options.setOrientationLocked(false)

        barcodeLauncher.launch(options)
    }

}