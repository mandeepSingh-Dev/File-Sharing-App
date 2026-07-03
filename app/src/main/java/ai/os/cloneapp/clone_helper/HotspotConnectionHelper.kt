package ai.os.cloneapp.clone_helper

import ai.os.cloneapp.base.BaseApp
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.SoftApConfiguration
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.random.Random

object HotspotConnectionHelper {

    private const val TAG = "HotspotConnectionHelper"
    val context = BaseApp.instance



     @RequiresApi(Build.VERSION_CODES.R)
     fun getSSID_Password(){
         Log.d("HotspotInfo", "Getting ssid and password")
/*
         val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
         val method = wifiManager.javaClass.getMethod("getSoftApConfiguration")
         val softApConfig = method.invoke(wifiManager) as SoftApConfiguration

         val ssid = softApConfig.ssid
         val password = softApConfig.passphrase*/


         startLocalHotspot(onStarted = {a,r ->}){}


        /* enableHotspotPrivileged( ssid = ssid!!, password = password!!, onSuccess = {
             Log.d(TAG,"Started")
         }, onFailed = {
             Log.d(TAG,"Failed")
         })*/

//         enableHotspot()
//        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
//        val method = wifiManager.javaClass.getMethod("getWifiApConfiguration")
//        val config = method.invoke(wifiManager) as WifiConfiguration
//
//        val ssid = config.SSID
//        val password = config.preSharedKey

//        Log.d("HotspotInfo", "SSID: $ssid, Password: $password")
    }

    fun generateHotspotCredentials(): Pair<String, String> {
        val ssid = "CloneHotspot_" + (1000..9999).random()
        val password = List(8) { ('A'..'Z') + ('0'..'9') }.flatten().shuffled().take(8).joinToString("")
        Log.d("Generated HotspotInfo", "SSID: $ssid, Password: $password")
        return Pair(ssid, password)
    }


    // Only works for system apps with privileged permissions
    fun enableHotspot(ssid: String?, password: String?) {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val hotspotConfig = WifiConfiguration().apply {
            this.SSID = ssid
            preSharedKey = password
            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
        }

        try {

            wifiManager.javaClass.methods.forEach {
                Log.d("fklbkfbnkf", it.name.toString())
            }

            val method = wifiManager.javaClass.getMethod("setWifiApEnabled", WifiConfiguration::class.java, Boolean::class.java)
            method.invoke(wifiManager, hotspotConfig, true)
            Log.d(TAG, "Hotspot enabled: $ssid")
        } catch (e: Exception) {
            Log.d(TAG,"false")
            e.printStackTrace()
        }
    }
    fun enableHotspot(
        ssid: String = "MyHotspot",
        password: String = "12345678",
        onStarted: (ssid: String, pass: String) -> Unit,
        onFailed: (reason: String) -> Unit
    ) {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Step 1: Build WifiConfiguration for the hotspot
        val wifiConfig = WifiConfiguration().apply {
            SSID = ssid
            preSharedKey = password
            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
        }

        try {
            // Step 2: Call hidden API via reflection
            val method = wifiManager.javaClass.getMethod(
                "setWifiApEnabled",
                WifiConfiguration::class.java,
                Boolean::class.java
            )
            val result = method.invoke(wifiManager, wifiConfig, true) as Boolean

            if (result) {
                Log.d("Hotspot", "Hotspot started: $ssid / $password")
                onStarted(ssid, password)
            } else {
                onFailed("setWifiApEnabled returned false")
            }

        } catch (e: Exception) {
            onFailed("Reflection failed: ${e.message}")
            Log.e("Hotspot", "Error: ${e.message}")
        }
    }


    private var hotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null

    fun startLocalHotspot(
        onStarted: (ssid: String, password: String) -> Unit,
        onFailed: (reason: Int) -> Unit
    ) {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        wifiManager.startLocalOnlyHotspot(
            object : WifiManager.LocalOnlyHotspotCallback() {

                override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                    super.onStarted(reservation)
                    hotspotReservation = reservation

                    val config = reservation.wifiConfiguration  // API 26-29
                    // OR
                    // val config = reservation.softApConfiguration // API 30+

                    val ssid = config?.SSID ?: "Unknown"
                    val password = config?.preSharedKey ?: "Unknown"

                    Log.d(TAG, "Started SSID=$ssid PASS=$password")
                    onStarted(ssid, password)
                }

                override fun onStopped() {
                    super.onStopped()
                    Log.d(TAG, "Hotspot stopped")
                }

                override fun onFailed(reason: Int) {
                    super.onFailed(reason)
                    Log.e(TAG, "Failed reason=$reason")
                    onFailed(reason)
                }
            },
            Handler(Looper.getMainLooper())
        )
    }

    fun stopLocalHotspot() {
        hotspotReservation?.close()
        hotspotReservation = null
    }

   /* fun connectToHotspot(ssid: String, password: String, context: Context) {


// Build hotspot configuration
        val softApConfig = SoftApConfiguration.Builder()
            .setSsid("CloneHotspot")                               // Your chosen SSID
            .setPassphrase("Clone1234", SoftApConfiguration.SECURITY_TYPE_WPA2_PSK) // Password
            .setBand(SoftApConfiguration.BAND_2GHZ)               // Optional: 2.4GHz or 5GHz
            .build()

// Apply configuration
        val setConfigMethod = wifiManager.javaClass.getMethod("setSoftApConfiguration", SoftApConfiguration::class.java)
        setConfigMethod.invoke(wifiManager, softApConfig)


        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val wifiConfig = WifiConfiguration().apply {
            this.SSID = "\"$ssid\""
            this.preSharedKey = "\"$password\""
            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
        }

wifiManager.startLocalOnlyHotspot()

        val netId = wifiManager.addNetwork(wifiConfig)
        wifiManager.disconnect()
        wifiManager.enableNetwork(netId, true)
        wifiManager.reconnect()
        Log.d("HotspotClient", "Connected to hotspot: $ssid")
    }

*/


    @RequiresApi(Build.VERSION_CODES.R)
    fun enableHotspotPrivileged(
        ssid: String,
        password: String,
        onSuccess: () -> Unit,
        onFailed: (Int) -> Unit
    ) {
        try {
            // ----------------------------
            // Step 1: Build SoftApConfiguration via reflection
            // ----------------------------
            val softApBuilderClass = Class.forName("android.net.wifi.SoftApConfiguration\$Builder")
            val builder = softApBuilderClass.getConstructor().newInstance()

            // setSsid(String)
            val setSsidMethod = softApBuilderClass.getMethod("setSsid", String::class.java)
            setSsidMethod.invoke(builder, ssid)

            // setPassphrase(String, int)
            val setPassphraseMethod =
                softApBuilderClass.getMethod("setPassphrase", String::class.java, Int::class.javaPrimitiveType)
            setPassphraseMethod.invoke(builder, password, 2) // 2 = SECURITY_TYPE_WPA2_PSK

            // setBand(int)
            val setBandMethod = softApBuilderClass.getMethod("setBand", Int::class.javaPrimitiveType)
            setBandMethod.invoke(builder, 0) // 0 = BAND_2GHZ

            // setHiddenSsid(boolean)
            val setHiddenMethod = softApBuilderClass.getMethod("setHiddenSsid", Boolean::class.javaPrimitiveType)
            setHiddenMethod.invoke(builder, false)

            // build()
            val buildMethod = softApBuilderClass.getMethod("build")
            val softApConfig = buildMethod.invoke(builder)

            // ----------------------------
            // Step 2: Build TetheringRequest via reflection
            // ----------------------------
            val tetheringRequestBuilderClass = Class.forName("android.net.TetheringManager\$TetheringRequest\$Builder")
            val tetheringRequestBuilderConstructor = tetheringRequestBuilderClass.getConstructor(Int::class.java)
            val tetheringRequestBuilder = tetheringRequestBuilderConstructor.newInstance(0) // 0 = TETHERING_WIFI

            // setSoftApConfiguration(SoftApConfiguration)
            val setSoftApConfigMethod = tetheringRequestBuilderClass.getMethod(
                "setSoftApConfiguration",
                Class.forName("android.net.wifi.SoftApConfiguration")
            )
            setSoftApConfigMethod.invoke(tetheringRequestBuilder, softApConfig)

            // setShouldShowEntitlementUi(false)
            val setEntitlementMethod = tetheringRequestBuilderClass.getMethod(
                "setShouldShowEntitlementUi",
                Boolean::class.javaPrimitiveType
            )
            setEntitlementMethod.invoke(tetheringRequestBuilder, false)

            // build()
            val tetheringBuildMethod = tetheringRequestBuilderClass.getMethod("build")
            val tetheringRequest = tetheringBuildMethod.invoke(tetheringRequestBuilder)

            // ----------------------------
            // Step 3: Start tethering via reflection
            // ----------------------------
            val tetheringManager = context.getSystemService(Context.TETHERING_SERVICE) as Any
            val startMethod = tetheringManager.javaClass.getMethod(
                "startTethering",
                Class.forName("android.net.TetheringManager\$TetheringRequest"),
                Executor::class.java,
                Class.forName("android.net.TetheringManager\$StartTetheringCallback")
            )

            // Create callback proxy
            val callbackClass = Class.forName("android.net.TetheringManager\$StartTetheringCallback")
            val callback = java.lang.reflect.Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass)
            ) { _, method, args ->
                when (method.name) {
                    "onTetheringStarted" -> onSuccess()
                    "onTetheringFailed" -> onFailed(args[0] as Int)
                }
                null
            }

            // Execute
            startMethod.invoke(
                tetheringManager,
                tetheringRequest,
                Executors.newSingleThreadExecutor(),
                callback
            )

        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            onFailed(-1)
        }
    }


    fun enableHotspot(){
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        /*         val softApBuilderClass = Class.forName("android.net.wifi.SoftApConfiguration\$Builder")
             val builder = softApBuilderClass.getConstructor().newInstance()

     // Set SSID
             softApBuilderClass.getMethod("setSsid", String::class.java).invoke(builder, "CloneHotspot")
     // Set password (WPA2)
             softApBuilderClass.getMethod("setPassphrase", String::class.java, Int::class.javaPrimitiveType)
                 .invoke(builder, "Clone1234", 2) // 2 = SECURITY_TYPE_WPA2_PSK
     // Set band (optional)
     //        softApBuilderClass.getMethod("setBand", Int::class.javaPrimitiveType).invoke(builder, 0) // 0 = 2.4GHz
     // Set hidden SSID
             softApBuilderClass.getMethod("setHiddenSsid", Boolean::class.javaPrimitiveType).invoke(builder, false)

     // Build SoftApConfiguration
             val softApConfig = softApBuilderClass.getMethod("build").invoke(builder)
             // Apply the configuration
             val setConfigMethod = wifiManager.javaClass.getMethod(
                 "setSoftApConfiguration",
                 Class.forName("android.net.wifi.SoftApConfiguration")
             )
             setConfigMethod.invoke(wifiManager, softApConfig)

// Enable hotspot (hidden API)
        val enableMethod = wifiManager.javaClass.getMethod("setWifiApEnabled", WifiConfiguration::class.java, Boolean::class.java)
        enableMethod.invoke(wifiManager, null, true) // null = use previously set SoftApConfiguration
  */

        wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
            override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                val ssid = reservation.wifiConfiguration?.SSID
                val password = reservation.wifiConfiguration?.preSharedKey
                Log.d("Hotspot", "Hotspot started: $ssid / $password")
            }

            override fun onStopped() {}
            override fun onFailed(reason: Int) { onFailed(reason) }
        }, Handler(Looper.getMainLooper()))
    }


  /*  fun connectToHotspot( ssid: String, password: String) {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val config = WifiConfiguration().apply {
            SSID = "\"$ssid\""
            preSharedKey = "\"$password\""
        }

        val netId = wifiManager.addNetwork(config)

        wifiManager.disconnect()
        wifiManager.enableNetwork(netId, true)
        wifiManager.reconnect()
    }
*/



    @RequiresApi(Build.VERSION_CODES.Q)
    fun connectToHotspot(
        ssid: String,
        password: String,

    ) {

        Log.d("kvnnbjmf","kfnfjkdv")


        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.disconnect()

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                Log.d("HOTSPOT", "Connected successfully")

                connectivityManager.bindProcessToNetwork(network)

//                onConnected(network)
            }

            override fun onUnavailable() {
                Log.e("HOTSPOT", "Connection failed")
//                onFailed()
            }

            override fun onLost(network: Network) {
                Log.e("HOTSPOT", "Connection lost")
            }
        }

        connectivityManager.requestNetwork(request, callback)
    }
}