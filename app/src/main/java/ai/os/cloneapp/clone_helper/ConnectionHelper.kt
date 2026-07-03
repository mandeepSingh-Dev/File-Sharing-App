package ai.os.cloneapp.clone_helper

import ai.os.cloneapp.DeviceRole
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ConnectionHelper(private val context: Context,
    val onReceiveIPAddressOfReceiver : (String?) -> Unit) {

    companion object {
        val TAG = "Connection_Helper_LOG"
    }

    val wifiManager: WifiManager? by lazy {
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager?
    }
    val wifiP2pManager: WifiP2pManager? by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    }
    var channel: WifiP2pManager.Channel? = null

    fun initialize(mainLooper: Looper, role: DeviceRole) {
            channel = wifiP2pManager?.initialize(context, mainLooper, null)
            registerPeerChangeReceiver()
    }

    fun createGroup(onFetchDetails: (String, String) -> Unit){
//        if(role == DeviceRole.RECEIVER) {
            if (channel != null) {
                resetOldP2pConnection {
                    setupReceiverGroup(onFetchDetails = onFetchDetails)
                }
            }
//        }else{
//            delay(500)
//            resetOldP2pConnection {
//                connectToReceiver("DIRECT-Hy-realme NARZO 80x 5G", "eJHckAU1")
//            }
//
//            while(true){
//                delay(500)
//                wifiP2pManager?.requestConnectionInfo(channel,{
//
//                    Log.d("fkbnknvbfk", "Address: ${it.groupOwnerAddress} , ${it.groupFormed} , Group Owner: ${it.isGroupOwner}")
//                })
//            }
//        }
    }

    fun connect(ssid : String, pass : String){
        resetOldP2pConnection {
//            connectToReceiver("DIRECT-Hy-realme NARZO 80x 5G", "eJHckAU1")
            connectToReceiver(ssid, pass)
        }
    }


     private fun setupReceiverGroup(onFetchDetails: (String,String) -> Unit) {
        wifiP2pManager?.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Group created successfully! Now fetch the details.
                Log.d(TAG, "fetchGroupDetails" )
                fetchGroupDetails(onFetchDetails)
            }
            override fun onFailure(reason: Int) {
                Log.e("P2P", "Failed to create group: $reason")
            }
        })
    }

    private fun fetchGroupDetails(onFetchDetails: (String,String) -> Unit) {

        wifiP2pManager?.requestGroupInfo(channel) { group ->
            if (group != null && group.isGroupOwner) {
                val ssid = group.networkName
                val password = group.passphrase

                Log.d(TAG, "ssid: $ssid password: $password  E" )

                onFetchDetails(ssid,password)
//                connectToReceiver(ssid = ssid, pass = password)

                // Format this for a standard Wi-Fi QR Code
                val qrContent = "WIFI:S:$ssid;T:WPA;P:$password;;"
            }
        }
    }

    fun registerPeerChangeReceiver(){
         val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        val receiver = PeerChangedBReceiver(channel, wifiP2pManager, onReceiveIPAddressOfReceiver = onReceiveIPAddressOfReceiver)
        context.registerReceiver(receiver,intentFilter)
    }

    class PeerChangedBReceiver(
        val channel: WifiP2pManager.Channel?,
        val wifiP2pManager : WifiP2pManager?,
        val onReceiveIPAddressOfReceiver : (String?) -> Unit)  : BroadcastReceiver()
    {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.let {
                when(it){
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        Log.d(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION")
                        wifiP2pManager?.requestPeers(channel,object : WifiP2pManager.PeerListListener{
                            override fun onPeersAvailable(peers: WifiP2pDeviceList?) {
//                                Log.d(TAG, peers.toString())
                            }
                        })
                    }
                    WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {

                        val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                        when (state) {
                            WifiP2pManager.WIFI_P2P_STATE_ENABLED -> {
                                // Wifi P2P is enabled
                            }
                            else -> {
                                // Wi-Fi P2P is not enabled
                            }
                        }
                        Log.d(TAG, "WIFI_P2P_STATE_CHANGED_ACTION $state")

                    }
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {

                        CoroutineScope(Dispatchers.IO).launch {
                            delay(2000)

                            val info = intent.getParcelableExtra<WifiP2pInfo>(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
                            val  group = intent.getParcelableExtra<WifiP2pGroup>(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);

                            Log.d("fknbfknbfk", info?.groupOwnerAddress.toString())
                            Log.d("fknbfknbfk", group?.networkName.toString())

                            val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
Log.d("fkljbkfnvf", networkInfo?.isConnected.toString())
//                        if(networkInfo?.isConnected == true) {
                            wifiP2pManager?.requestConnectionInfo(
                                channel,
                                object : WifiP2pManager.ConnectionInfoListener {
                                    override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
                                        Log.d(
                                            TAG,
                                            "Group formed: ${info?.groupFormed} - Group Owner: ${info?.isGroupOwner} - Group Owner address${info?.groupOwnerAddress}"
                                        )
                                        onReceiveIPAddressOfReceiver(info?.groupOwnerAddress?.hostAddress)

                                    }


                                })
                            wifiP2pManager?.requestGroupInfo(channel, WifiP2pManager.GroupInfoListener{info ->
                                Log.d(
                                    TAG,
                                    "Group Owner: ${info?.isGroupOwner} - Group Owner address"
                                )

                            })
//                        }
                        }
                        Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION")
                    }
                    WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                        Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION")
                    }
                }
                }
        }
    }


//    discoverPeers()  = Start searching people in a room
//    requestPeers()   = Ask "who have we found so far?"
    // 1. Discover Peers
    fun discoverPeers() {

        wifiP2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Discovery started")
            }
            override fun onFailure(reason: Int) {


                Log.d(TAG, "Discovery Failure $reason")
            }
        })

    }

/*
    fun connect(device: WifiP2pDevice) {

        val config = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }

        wifiP2pManager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Discovery connected")

                // Connection logic handled in BroadcastReceiver
            }
            override fun onFailure(reason: Int) {
                Log.d(TAG, "Discovery failed")
            }
        })
    }*/

    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    private fun connectToReceiver(ssid: String, pass: String) {

        wifiP2pManager?.cancelConnect(channel,null)

        // 1. Create the configuration using the scanned details
        val config = WifiP2pConfig.Builder()
            .setNetworkName(ssid)
            .setPassphrase(pass)
            .build()

        Log.d(TAG, "ssid: $ssid pass: $pass")

        // 2. Instruct the manager to join that specific group
        wifiP2pManager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Connected")

                waitForActualConnection()

                // Success! The OS is now handshaking with the Receiver.
                // Connection info will be received in your BroadcastReceiver
                // or via requestConnectionInfo.
            }

            override fun onFailure(reason: Int) {
                Log.d(TAG, "Connection failure")
                Log.e("P2P", "Connection failed: $reason")
            }
        })
    }


    private fun waitForActualConnection() {
        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                wifiP2pManager?.requestConnectionInfo(channel) { info ->
                    if (info != null && info.groupFormed) {
                        Log.d(TAG, "REAL CONNECTION ESTABLISHED")
                        Log.d(TAG, "Owner IP = ${info.groupOwnerAddress.hostAddress}")
                    } else {
                        Log.d(TAG, "Still connecting...")
                        Handler(Looper.getMainLooper()).postDelayed(this, 1000)
                    }
                }
            }
        }, 1000)
    }

    fun resetOldP2pConnection(onDone: (() -> Unit)? = null) {
        wifiP2pManager?.removeGroup(channel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "Old P2P group removed")
                    onDone?.invoke()
                }

                override fun onFailure(reason: Int) {
                    Log.d(TAG, "No old group / remove failed: $reason")
                    onDone?.invoke()
                }
            })

        wifiP2pManager?.cancelConnect(channel,null)
    }

}