package ai.os.cloneapp

import ai.os.cloneapp.clone_helper.AppTransffer.AccountTransferConstants
import ai.os.cloneapp.clone_helper.AppTransffer.Apk.ApkCollector
import ai.os.cloneapp.clone_helper.AppTransffer.Apk.TransferManager
import ai.os.cloneapp.clone_helper.AppTransffer.ExportService
import ai.os.cloneapp.clone_helper.AppTransffer.ExportService.Companion.ACCOUNT_TYPE
import ai.os.cloneapp.clone_helper.AppTransffer.ExportService.Companion.TAG
import ai.os.cloneapp.clone_helper.ConnectionHelper
import ai.os.cloneapp.clone_helper.DeviceSettingsHelper
import ai.os.cloneapp.clone_helper.QrCodeGenerator
import ai.os.cloneapp.clone_helper.SenderServer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ai.os.cloneapp.ui.theme.CloneAppTheme
import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.JsonWriter
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.DatePickerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.text.toSpannable
import androidx.lifecycle.lifecycleScope
import com.android.internal.protolog.common.LogDataType
import com.android.server.criticalevents.nano.CriticalEventProto
import com.google.android.gms.auth.api.accounttransfer.AccountTransfer
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.internal.GoogleServices
import com.journeyapps.barcodescanner.ScanContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.descriptors.StructureKind
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects
import java.util.concurrent.TimeoutException

enum class DeviceRole {
    RECEIVER,
    SENDER
}


class MainActivity : ComponentActivity() {


    companion object {
        private const val RC_GOOGLE_SIGN_IN = 1001
        private const val RC_DEVICE_COPY = 1002
    }


    val qrCodeGenerator = QrCodeGenerator()
    var senderServer: SenderServer? = null

    var connectionHelper: ConnectionHelper? = null

    val role: DeviceRole = DeviceRole.SENDER

    val deviceSettingsHelper = DeviceSettingsHelper(this)


    private val barcodeLauncher =
        registerForActivityResult(ScanContract()) { result ->
            if (result.contents != null) {
                parseQrData(result.contents)
            }
        }

    fun parseQrData(qrText: String) {
        val json = JSONObject(qrText)

        val ssid = json.getString("ssid")
        val password = json.getString("password")

        Log.d("QR", "SSID = $ssid")
        Log.d("QR", "PASS = $password")

        connectionHelper?.connect(ssid, password)
    }

    override fun onPause() {
        super.onPause()
        Log.d("ACTIVITY_LIFECYCLE", "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("ACTIVITY_LIFECYCLE", "onStop")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("knvkdnvkdvd", newConfig.fontScale.toString())
        Log.d("kmbknkfbf", "onConfiguration")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()



        findTheMaximumElemInAray()
//        CoroutineScope(Dispatchers.IO).launch {
//            delay(2000)
//        moveTaskToBack(true)
//        }

        val sp = getSharedPreferences("Hello", Context.MODE_PRIVATE)
        sp.edit().apply {
            putString("YoYo", "Mandeep")
        }.apply()



        CoroutineScope(Dispatchers.IO).launch {
//            executeApi().collect { Log.d("EXECUTE_API_LOGS", it.toString()) }

            Class.forName(MutexChecker::class.java.name).methods.forEach {
                Log.d("jfnbjfnbjf", it.name.toString())
            }
            MutexChecker().let { MutexChecker ->

                 launch {  MutexChecker.increment(1, { Log.d("MutexLogger", it.toString()) })}
              launch {    MutexChecker.increment(2, { Log.d("MutexLogger", it.toString()) })}
              launch {    MutexChecker.increment(3, { Log.d("MutexLogger", it.toString()) })}
              launch {    MutexChecker.increment(4, { Log.d("MutexLogger", it.toString()) })}
              launch {    MutexChecker.increment(5, { Log.d("MutexLogger", it.toString()) })}
              launch {    MutexChecker.increment(6, { Log.d("MutexLogger", it.toString()) })}
              launch {    MutexChecker.increment(7, { Log.d("MutexLogger", it.toString()) })}
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val appInfo = packageManager.getApplicationInfo(
                "ai.os.nxtiotwatchapp",
                0
            )

            Log.d("fkbnfkbnf", appInfo.sourceDir.toString())

            val prefsDir = File(appInfo.dataDir, "shared_prefs")
            val dbDir = File(appInfo.dataDir, "databases")

            Log.d("fkbnfkbnf", prefsDir.listFiles()?.size.toString())

            Log.d("fkbnfkbnf", prefsDir.toString())
            Log.d("fkbnfkbnf", dbDir.toString())
        }

        printPrimeNumbers()
        fibonacciSeries()

        val accountTransferClient = AccountTransfer.getAccountTransferClient(this)
        accountTransferClient.sendData(ACCOUNT_TYPE, "".toByteArray())


//        val intent = Intent(this, ExportBroadcastReceiver::class.java)
//        intent.action = AccountTransferConstants.ACTION_EXPORT
//        registerReceiver(intent, IntentFilter())
//

        ApkCollector.collectUserApks(this).forEach {
            Log.d("fknvkfnbkfbnv", it.fileName.toString())
        }

//        Log.d("fkbnfkbnknfb",isDeviceCopyAvailable(this).toString())
//        setupGoogleSignIn()

        lifecycleScope.launch(Dispatchers.IO) {
            val r = deviceSettingsHelper.getAllSettings()
            for (i in 0 until r.length()) {
                Log.d("flvnkbnkfvf", r.get(i).toString())
            }

//            delay(3000)
//            getGoogleAccounts()

        }


        if (!Settings.System.canWrite(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            100
        )


        /*
        lifecycleScope.launch(Dispatchers.IO) {
            AppScanner().getUserApps().forEach {
                Log.d("fkbnfkbnfk", it.toString())
            }
        }
                    // Standard path for User 0
                    val path = "/data/data/com.inspiredandroid.kai/shared_prefs/"
                    val directory = File(path)
                Log.d("fknbkfnb", directory.name.toString())

                val path2 = "/data/user/0/com.inspiredandroid.kai/"
                val file = File(path2)

                Environment.getExternalStorageDirectory()
                    .walkTopDown().forEach {
                        Log.d("gkbknkgb", it.name.toString())
                    }

                recursivelyRead("0")
                Log.d("FILE_READER","Finish\n\n")
                recursivelyRead("data")
                Log.d("FILE_READER","Finish\n\n")
                recursivelyRead("Android")

                File("0").walkTopDown().forEachIndexed { index, file ->
                    Log.d("flbnkfnbfk", file.name.toString())
                }

                File("android").walkTopDown().forEachIndexed { index, file ->
                    Log.d("flbn2442423213kfnbfk", file.name.toString())
                }

                File("data").walkTopDown().forEachIndexed { index, file ->
                    Log.d("flbnk12231312fnbfk", file.name.toString())
                }

                Log.d("fkbnfkbnfknbf", file.exists().toString() + "  : exist")
                Log.d("fkbnfkbnfknbf", file.isDirectory.toString() + " : isDir")
                Log.d("fkbnfkbnfknbf", file.length().toString() + " : length")

                try {
                    File("storage/emulated/0/Android/data/com.example.fotokad")
                        .run {
                            Log.d("fknvknbkfn", name)
                            Log.d("fknvknbkfn", exists().toString())
                            Log.d("fknvknbkfn", length().toString())

                            listFiles().forEach {
                            Log.d("fkfvnknbkf", it.name.toString())
                                }
                        }
                }catch (e:Exception){
                    Log.d("fknbkfnbkf", "1 ${e.message}}")
                }
                try{
                File("data/user/0/com.example.fotokad")
                    .run {
                        Log.d("fknvknbkfn", name)
                        Log.d("fknvknbkfn", exists().toString())
                        Log.d("fknvknbkfn", length().toString())
                        Log.d("fknvknbkfn", listFiles()?.size.toString())


                        listFiles()?.forEach {
                        Log.d("fkfvnknbkf_Fotokad", it.name.toString())
                    }
                    }

                }catch (e:Exception){
                    Log.d("fknbkfnbkf", "3 ${e.message}}")
                }

        //        /data/user/0/ai.os.cloneapp
        //        /storage/emulated/0/Android/data/ai.os.cloneapp
        //        /storage/emulated/0/Android/obb/ai.os.cloneapp


                    if (directory.exists()) {
                        val files = directory.listFiles()
                        Log.d("fknbkfnb", files.size.toString())
                        // Implement your copy/stream logic here
                    }

        //        val r = AppBackupManager.createAppBackup("com.inspiredandroid.kai")
        //            Log.d("fblnfkbnfkbnf",r?.length().toString())
                val senderServer = SenderServer("192.168.49.1")
        */
        getGoogleAccounts()

        setContent {
            CloneAppTheme {

                var isConnected by remember { mutableStateOf<Boolean?>(null) }
                var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

                LaunchedEffect(Unit) {


                    connectionHelper = ConnectionHelper(
                        context = this@MainActivity,
                        onReceiveIPAddressOfReceiver = {
                            it?.let {
                                senderServer = SenderServer(it)
                                isConnected = true
                            }
                        })

                    connectionHelper?.initialize(mainLooper, role = role)

                    if (role == DeviceRole.RECEIVER) {

                        connectionHelper?.createGroup(onFetchDetails = { ssid, pass ->
                            qrBitmap = qrCodeGenerator.generateQrCode(ssid, password = pass)
                        })
                    } else {
                        isConnected = false
//                        qrCodeGenerator.startQrScanner(barcodeLauncher = barcodeLauncher)
                        //connect calling in qr-launcher
                    }

                    if (role == DeviceRole.RECEIVER) {
//                        ReceiverGroupOwnerServer().startCoroutinesServer()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
//                        .clickable{
//
//                        }
                    /* .clickable{
                         if(role == DeviceRole.SENDER) {
                             lifecycleScope.launch {
                                 startApksSending()
 //                                val settingsData = deviceSettingsHelper.getAllSettings().toString()
 //                                senderServer?.sendData(settingsData)
                             }
                         }else{
                             receiveApks()
 //                            openDeviceSetupTransfer(this@MainActivity)
                         }

                     }*/
                ) { innerPadding ->

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {

                        Text("Clone app")


                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Check Debounce techniwue",
                            modifier = Modifier
                                .padding(20.dp)
                                .clickedAnimation {

                                }
                        )
                        AnimatedSlider()


                        isConnected.let {
                            Text(
                                if (isConnected == true) "Connected" else "Connecting",
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                        qrBitmap?.let {
                            Image(bitmap = it.asImageBitmap(), contentDescription = null)
                        }
                    }

                }
            }
        }


    }


    fun findTheMaximumElemInAray() {
        val arr = arrayOf(1, 2, 3, 4, 5, 6, 7, 8)

        var check = false

        for (i in 1..<arr.size) {
            if (arr[i - 1] < arr[i]) {
                check = true
            } else {
                check = false
            }
            if (!check) {
                break
            }
        }
        if (!check) {
            Log.d("fkvbkfbnvkf", "Not in descing")
        } else {
            Log.d("fkvbkfbnvkf", "In Ascending")
        }


        val str = "I am happybeautiful ever in this beautiful world"
        val strArr = str.split(" ")

        var longestWord = strArr[0]

        for (i in 1..<strArr.size) {
            if (strArr[i].length > longestWord.length) {
                longestWord = strArr[i]
            }
        }
    }


    fun executeApi() = flow {
        val result = hitApi(1)
        emit(result)
    }

        .retryWhen { throwable, attempt ->
            if (throwable is TimeoutException) {
                Log.d("EXECUTE_API_LOGS", "retry" + attempt.toString())
                delay((attempt * 2) * 1000)
                true
            } else {
                false
            }
        }.catch {
            Log.d("EXECUTE_API_LOGS", "retry" + it.toString())
        }

    var count = 1
    suspend fun hitApi(attempt: Int): Int? = withContext(Dispatchers.IO) {
        delay(1000)
        if (count < 4) {
            count += 1
            throw TimeoutException("Time out error...")
        }
        return@withContext 1
    }


    fun printPrimeNumbers() {
        val upTo = 100

        for (n in 2..upTo) {
            var count = 0
            for (i in 2..upTo) {
                if (count == 2) {
                    break
                }
                if (n % i == 0) {
                    count++
                }
            }
            if (count == 1) {
                Log.d("fkbnkfbnjkf", n.toString())
            }
        }
    }

    //0,1,1,2,3,5,8,13,21
    fun fibonacciSeries() {


        var prev1 = 1
        var prev2 = 0

        for (i in 1..10) {
            val current = prev1 + prev2
            prev2 = prev1
            prev1 = current
        }
    }

//       *
//      ***
//     *****
//    *******
//


    @Composable
    fun Modifier.debouncingTechnique(onClick: () -> Unit): Modifier {

        var previousTime by rememberSaveable { mutableLongStateOf(0L) }
        return clickable {
            val currentTime = System.currentTimeMillis()
            if ((currentTime - previousTime) > 1000) {
                Log.d("kfnbkfnbkf", "$currentTime and $previousTime")
                val spf = SimpleDateFormat("hh:mm:ss")
                spf.format(Date())
                previousTime = currentTime
                clickable { onClick() }
            } else {
                this
            }
        }
    }

    @Composable
    fun Modifier.clickedAnimation(onClick: () -> Unit): Modifier {


        var eventAction by remember { mutableStateOf<Int?>(null) }

        var isDown by remember(eventAction) { mutableStateOf(eventAction == MotionEvent.ACTION_DOWN) }
        var isMoving by remember(eventAction) { mutableStateOf(eventAction == MotionEvent.ACTION_MOVE) }
        var isUp by remember(eventAction) { mutableStateOf(eventAction == MotionEvent.ACTION_UP) }

        var pressX by remember { mutableStateOf<Float?>(null) }

        var width by remember { mutableStateOf<Int?>(null) }

        val scaleClickedAnim by animateFloatAsState(targetValue = if (isDown || isMoving) 0.8f else 1f)
        val transYClickedAnim by animateFloatAsState(targetValue = if (isDown || isMoving) 10f else 1f)
        val dropShadowHoveredColor by animateColorAsState(if (isMoving && !isDown) Color.Red else if (!isMoving && isDown) Color.Blue else Color.Transparent)

        LaunchedEffect(isUp) {
            onClick()
        }

        return graphicsLayer {
            scaleY = scaleClickedAnim
            scaleX = scaleClickedAnim
            translationY = transYClickedAnim

        }
            .onSizeChanged {
                width = it.width
            }
            .pointerInteropFilter(onTouchEvent = { event ->
                Log.d("ljvkfnvbjkfnbjvfk", event.toString())
                eventAction = event.action


                Log.d(
                    "kbkfbnfjkjknb",
                    "RawX -> ${event.rawX} and X -> ${event.x} and precision X -> ${event.xPrecision}"
                )

//                when(event.action){
//                    MotionEvent.ACTION_DOWN -> isDown = true
//                    MotionEvent.ACTION_MOVE -> isMoving = true
//                    MotionEvent.ACTION_UP -> isPressed = true
//                    else ->
//                }

                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                    width?.let {
                        if (event.x > 0f && event.x < it) {
                            Log.d("kbnfkbnfkbnf", "${event.x.toString()} and Width is $width")

                            pressX = event.x / it.toFloat()
                            Log.d("kfnbknfbkf", pressX.toString())
                        }
                    }

                    true
                } else {
                    pressX = 0f
                    false
                }
            })
            .dropShadow(
                shape = RoundedCornerShape(20.dp),
                shadow = Shadow(color = dropShadowHoveredColor, radius = 10.dp)
            )

    }


    fun startApksSending() {
        // ── On SENDER device (client side) ───────────────────────────────────────────
        TransferManager.startSending(
            context = this,
            goIpAddress = "192.168.49.1",
            includeApks = true,
            includeOwnData = true,
            includeExternalData = true,
            onProgress = { progress ->
                Log.d(TAG, "Sending ${progress.currentFile}: ${progress.overallPercent}%")

//                    progressBar.progress = progress.overallPercent
//                    statusText.text = "Sending ${progress.fileIndex}/${progress.totalFiles}: ${progress.currentFile}"
            },
            onComplete = {
                Log.d("TAG", "All files sent!")
            },
            onError = { e ->
                Log.e("TAG", "Transfer failed: ${e.message}")
            }
        )
    }

    fun receiveApks() {
// ── On RECEIVER device (Group Owner side) ────────────────────────────────────
        TransferManager.startReceiving(
            context = this,
            onProgress = { progress ->
                Log.d(TAG, "Receiving ${progress.currentFile}: ${progress.percent}%")
//                progressBar.progress = progress.percent
            },
            onComplete = { apks, backups ->
                Log.d(TAG, "Got ${apks.size} APKs and ${backups.size} backup files")
//                statusText.text = "Received ${apks.size} apps!"
                // APKs are auto-installed, backups auto-restored
            },
            onError = { e ->
                Log.e(TAG, "Receive failed: ${e.message}")
            }
        )
    }

    // NEW DEVICE — Just open Device Setup transfer screen
    fun openDeviceSetupTransfer(context: Context) {

        val intent = Intent(this, ExportService::class.java).apply {
            action = AccountTransferConstants.ACTION_EXPORT
        }
        startService(intent)

        // Option A: Samsung devices (most likely your OEM client)
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.samsung.android.devicesetup",
                    "com.samsung.android.devicesetup.steps.magiccable.MagicCableActivity"
                )
            }
            context.startActivity(intent)
            Log.d("OPEN_DEVICE_SETUP", "Started Activity")
            return
        } catch (e: Exception) {
            Log.e("OPEN_DEVICE_SETUP", "Samsung setup not found")
        }

        // Option B: Stock Android / Google
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.google.android.setupwizard",
                    "com.google.android.setupwizard.transfer.TransferActivity"
                )
            }
            context.startActivity(intent)
            Log.d("OPEN_DEVICE_SETUP", "Stock Android: Started Activity")
            return
        } catch (e: Exception) {
            Log.e("OPEN_DEVICE_SETUP", "Google setup not found")
        }

        // Option C: Generic restore action (works on most devices)
        try {
            val intent = Intent("com.google.android.setupwizard.TRANSFER_DATA")
            context.startActivity(intent)
            Log.d("OPEN_DEVICE_SETUP", "Generic restore action : Started Activity")
        } catch (e: Exception) {
            Log.e("OPEN_DEVICE_SETUP", "No device setup app found at all")
        }
    }

    fun getGoogleAccounts(): Array<Account> {

        Log.d(
            "fknbkfnbkfnb",
            (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.GET_ACCOUNTS_PRIVILEGED
            ) == PackageManager.PERMISSION_GRANTED).toString()
        )

        val accountManager = AccountManager.get(this)

        val accounts = accountManager.getAccountsByType(ACCOUNT_TYPE)

        Log.d("fkbnfknfk", accountManager.accounts.size.toString())
        accountManager.accounts.forEach {
            Log.d("klmvkfnbjkfbf", it.name.toString())
        }

        if (accounts.isEmpty()) {
            Log.w(TAG, "No accounts to export")
        }
        Log.d("fknbkfnbkf", "-----")

        accounts?.forEach {
            Log.d("fknbkfnbkf", it.name.toString())
        }
//        val exportData = serializeAccounts(accountManager = accountManager, accounts = accounts)
//        Log.d(TAG, "Prepared ${accounts.size} accounts for export")


        val r = accountManager.getAccountsByType("com.google")

        r.forEach {
            Log.d("fklbjfjkbnfjn", it.name.toString())
        }
        return r
    }


    // Step A: Check if Google Play Services device copy is available
    fun isDeviceCopyAvailable(context: Context): Boolean {
        val intent = buildDeviceCopyIntent()
        return intent.resolveActivity(context.packageManager) != null || isGmsAvailable(context)
    }

    private fun isGmsAvailable(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo("com.google.android.gms", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }


    // Step B: Build the intent that triggers "Copy to your new device" screen
    fun buildDeviceCopyIntent(): Intent {
        // Primary intent — direct component
        return Intent().apply {
            component = ComponentName(
                "com.google.android.gms",
                "com.google.android.gms.devicecopy.DeviceCopyFromPhoneActivity"
            )
            // Flags needed for cross-app launch
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    // Step C: Fallback intent if primary doesn't work
    fun buildFallbackIntent(): Intent {
        return Intent("android.intent.action.MAIN").apply {
            component = ComponentName(
                "com.google.android.gms",
                "com.google.android.gms.nearby.transfer.ui.DeviceTransferActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }


    var googleSignInClient: GoogleSignInClient? = null
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestId()
            .requestIdToken("YOUR_WEB_CLIENT_ID") // from Google Console
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Check if already signed in silently first
        googleSignInClient?.silentSignIn()
            ?.addOnSuccessListener { account ->
                // Already signed in — got account
//                onAccountDetected(account)
                Log.d("fkbfkfkkfnbf", account.displayName.toString())
            }
            ?.addOnFailureListener {
                // Not signed in — prompt user
                promptGoogleSignIn()
            }
    }

    private fun promptGoogleSignIn() {
        googleSignInClient?.signInIntent?.let {
            startActivityForResult(
                it,
                RC_GOOGLE_SIGN_IN
            )
        }
    }

    /* private fun onAccountDetected(account: GoogleSignInAccount) {
         detectedAccounts.add(account)

         Log.d("SENDER", "✅ Account detected:")
         Log.d("SENDER", "   Email: ${account.email}")
         Log.d("SENDER", "   Name: ${account.displayName}")
         Log.d("SENDER", "   ID: ${account.id}")
         Log.d("SENDER", "   Token: ${account.idToken?.take(20)}...")

         // Now show UI to user and offer to start transfer
         showTransferReadyUI(account)
     }

     // ─────────────────────────────────────────
     // PHASE 2: Launch Google Device Copy Screen
     // ─────────────────────────────────────────

     fun startAccountTransfer() {
         if (!TransferManager.isDeviceCopyAvailable(this)) {
             showError("Google Play Services not available")
             return
         }

         try {
             // Try primary Google device copy intent
             val intent = TransferManager.buildDeviceCopyIntent()
             startActivityForResult(intent, RC_DEVICE_COPY)

         } catch (e: ActivityNotFoundException) {
             try {
                 // Try fallback
                 startActivityForResult(
                     TransferManager.buildFallbackIntent(),
                     RC_DEVICE_COPY
                 )
             } catch (e2: Exception) {
                 // Last resort — open Google search for device setup
                 openGoogleSetupSearch()
             }
         }
     }
 */
    private fun openGoogleSetupSearch() {
        // This is what Google's own support page recommends for non-OEM apps
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://setup.google.com/transfer")
        }
        startActivity(intent)
    }

    // ─────────────────────────────────────────
    // PHASE 3: Handle Results
    // ─────────────────────────────────────────

    /*
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            when (requestCode) {

                RC_GOOGLE_SIGN_IN -> {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        onAccountDetected(account)
                    } catch (e: ApiException) {
                        Log.e("SENDER", "Sign in failed: ${e.statusCode}")
                        showError("Could not detect Google account: ${e.statusCode}")
                    }
                }

                RC_DEVICE_COPY -> {
                    when (resultCode) {
                        RESULT_OK -> {
                            Log.d("SENDER", "✅ Device copy completed successfully")
                            onTransferComplete()
                        }
                        RESULT_CANCELED -> {
                            Log.d("SENDER", "⚠️ User cancelled device copy")
                            onTransferCancelled()
                        }
                        else -> {
                            Log.e("SENDER", "❌ Device copy failed: $resultCode")
                            onTransferFailed(resultCode)
                        }
                    }
                }
            }
        }
    */

    private fun onTransferComplete() {
        // Google has handled account transfer
        // Notify your receiver app via your own transport (WiFi/QR etc.)
        notifyReceiverAppTransferDone()
    }

    private fun notifyReceiverAppTransferDone() {
        // Send status to your backend or direct to receiver device
        // via your own channel (WebSocket, Firebase, QR, etc.)
        Log.d("SENDER", "Transfer complete — notifying receiver")
    }

    private fun onTransferCancelled() {
        // Handle UI
    }

    private fun onTransferFailed(code: Int) {
        // Handle UI
    }

    private fun showTransferReadyUI(account: GoogleSignInAccount) {
        // Show account details in your UI
        // then let user tap "Start Transfer"
    }

    /* private fun showError(msg: String) {
         Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
     }
 */
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

fun recursivelyRead(path: String) {
    val file = File(path)

    if (!file.exists()) {
        Log.d("FILE_READER", "Path does not exist: $path")
        return
    }

    if (file.isDirectory) {
        Log.d("FILE_READER", "Directory: ${file.absolutePath}")



        file.listFiles()?.forEach { child ->
            recursivelyRead(child.absolutePath)
        }

    } else {
        Log.d("FILE_READER", "File: ${file.absolutePath}")
    }


}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CloneAppTheme {
        Greeting("Android")
    }
}

@Preview
@Composable
fun AnimatedSlider() {


    var width by remember { mutableStateOf<Int>(0) }


    var movingX by remember { mutableStateOf(0f) }
    val movingXAnimated by animateFloatAsState(
        movingX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val knobSize = 70.dp



    LaunchedEffect(movingX) {
        Log.d("fbfkbnfk", movingX.toString())
    }
    val density = LocalDensity.current

    val knobPx = with(density) { knobSize.toPx() }

    val maxX = width - knobPx

    val half = maxX / 2

    var isDragging by remember { mutableStateOf(false) }


    Box(
        modifier = Modifier
            .padding(horizontal = 40.dp)
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(
                brush = Brush.horizontalGradient(listOf(Color.Red, Color.Blue)),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(5.dp)
            .onSizeChanged({
                width = it.width
            })
    ) {

        Box(
            modifier = Modifier
                .size(70.dp)
                .offset { IntOffset(x = movingXAnimated.toInt(), y = 0) }
                .clip(CircleShape)
                .background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            isDragging = false
                            if (movingX < half) {
                                movingX = 0f
                            } else {
                                movingX = maxX
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            if (movingX < half) {
                                movingX = 0f
                            } else {
                                movingX = maxX
                            }
                        },
                        onDragStart = { isDragging = true },
                    ) { change, dragAmount ->
                        isDragging = true
                        change.consume()

                        val maxX = width - knobPx
                        movingX = (movingX + dragAmount.x).coerceIn(0f, maxX)

                    }
                }
        )
    }
}


class MutexChecker {

    val mutex = Mutex()

    var coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun increment(value: Int, result: (Int) -> Unit) {
        Log.d("kkjr3ir039e039", value.toString())
            mutex.withLock {
                delay(2000)
                result(value)
            }
    }

}




