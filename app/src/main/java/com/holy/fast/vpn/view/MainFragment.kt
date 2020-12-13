package com.holy.fast.vpn.view

import android.app.Activity
import android.content.*
import android.net.VpnService
import android.os.Bundle
import android.os.RemoteException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.holy.fast.vpn.CheckInternetConnection
import com.holy.fast.vpn.R
import com.holy.fast.vpn.SharedPreference
import com.holy.fast.vpn.interfaces.ChangeServer
import com.holy.fast.vpn.model.Server
import de.blinkt.openvpn.OpenVpnApi
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.OpenVPNThread
import de.blinkt.openvpn.core.VpnStatus
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class MainFragment : Fragment(), View.OnClickListener, ChangeServer {
    private var server: Server? = null
    private var connection: CheckInternetConnection? = null
    private val vpnThread = OpenVPNThread()
    private val vpnService = OpenVPNService()
    var vpnStart = false
    private var preference: SharedPreference? = null
    lateinit var durationTv: TextView
    lateinit var vpnBtn: Button
    lateinit var logTv: TextView
    lateinit var byteOutTv: TextView
    lateinit var byteInTv: TextView
    lateinit var lastPacketReceiveTv: TextView
    lateinit var selectedServerIcon: ImageView



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.fragment_main, container, false)
        durationTv = view.findViewById(R.id.durationTv)
        vpnBtn = view.findViewById(R.id.vpnBtn)
        logTv = view.findViewById(R.id.logTv)
        lastPacketReceiveTv = view.findViewById(R.id.lastPacketReceiveTv)
        byteInTv = view.findViewById(R.id.byteInTv)
        byteOutTv = view.findViewById(R.id.byteOutTv)
        selectedServerIcon = view.findViewById(R.id.selectedServerIcon)

        initializeAll()
        return view
    }

    /**
     * Initialize all variable and object
     */
    private fun initializeAll() {
        preference = SharedPreference(context)
        server = preference!!.server

        // Update current selected server icon
        updateCurrentServerIcon(server?.flagUrl)
        connection = CheckInternetConnection()
        LocalBroadcastManager.getInstance(activity!!).registerReceiver(broadcastReceiver, IntentFilter("connectionState"))
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vpnBtn.setOnClickListener(this)
//        vpnBtn

        // Checking is vpn already running or not
        isServiceRunning
        VpnStatus.initLogCache(activity!!.cacheDir)
    }

    /**
     * @param v: click listener view
     */
    override fun onClick(v: View) {
        when (v.id) {
            R.id.vpnBtn ->                 // Vpn is running, user would like to disconnect current connection.
                if (vpnStart) {
                    confirmDisconnect()
                } else {
                    prepareVpn()
                }
        }
    }

    /**
     * Show show disconnect confirm dialog
     */
    fun confirmDisconnect() {
        val builder = AlertDialog.Builder(activity!!)
        builder.setMessage(activity!!.getString(R.string.connection_close_confirm))
        builder.setPositiveButton(activity!!.getString(R.string.yes)) { dialog, id -> stopVpn() }
        builder.setNegativeButton(activity!!.getString(R.string.no)) { dialog, id ->
            // User cancelled the dialog
        }

        // Create the AlertDialog
        val dialog = builder.create()
        dialog.show()
    }

    /**
     * Prepare for vpn connect with required permission
     */
    private fun prepareVpn() {
        if (!vpnStart) {
            if (internetStatus) {

                // Checking permission for network monitor
                val intent = VpnService.prepare(context)
                if (intent != null) {
                    startActivityForResult(intent, 1)
                } else startVpn() //have already permission

                // Update confection status
                status("connecting")
            } else {

                // No internet connection available
                showToast("you have no internet connection !!")
            }
        } else if (stopVpn()) {

            // VPN is stopped, show a Toast message.
            showToast("Disconnect Successfully")
        }
    }

    /**
     * Stop vpn
     * @return boolean: VPN status
     */
    fun stopVpn(): Boolean {
        try {
            OpenVPNThread.stop()
            status("connect")
            vpnStart = false
            return true
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * Taking permission for network access
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {

            //Permission granted, start the VPN
            startVpn()
        } else {
            showToast("Permission Deny !! ")
        }
    }

    /**
     * Internet connection status.
     */
    val internetStatus: Boolean
        get() = connection!!.netCheck(context)

    /**
     * Get service status
     */
    val isServiceRunning: Unit
        get() {
            setStatus(OpenVPNService.getStatus())
        }

    /**
     * Start the VPN
     */
    private fun startVpn() {
        try {
            // .ovpn file
            val conf = activity!!.assets.open(server!!.ovpn)
            val isr = InputStreamReader(conf)
            val br = BufferedReader(isr)
            var config = ""
            var line: String?
            while (true) {
                line = br.readLine()
                if (line == null) break
                config += """
                    $line
                    
                    """.trimIndent()
            }
            br.readLine()
            OpenVpnApi.startVpn(context, config, server!!.country, server!!.ovpnUserName, server!!.ovpnUserPassword)

            // Update log
            logTv.setText("Connecting...")
            vpnStart = true
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
        catch (e: RemoteException) {
            e.printStackTrace()
        }
    }


    /**
     * Status change with corresponding vpn connection status
     * @param connectionState
     */
    fun setStatus(connectionState: String?) {
        if (connectionState != null) when (connectionState) {
            "DISCONNECTED" -> {
                status("connect")
                vpnStart = false
                OpenVPNService.setDefaultStatus()
                logTv.setText("")
            }
            "CONNECTED" -> {
                vpnStart = true // it will use after restart this activity
                status("connected")
                logTv.setText("")
            }
            "WAIT" -> logTv.setText("waiting for server connection!!")
            "AUTH" -> logTv.setText("server authenticating!!")
            "RECONNECTING" -> {
                status("connecting")
                logTv.setText("Reconnecting...")
            }
            "NONETWORK" -> logTv.setText("No network connection")
        }
    }

    /**
     * Change button background color and text
     * @param status: VPN current status
     */
    fun status(status: String) {
        if (status == "connect") {
            vpnBtn.setText(context!!.getString(R.string.connect))
        } else if (status == "connecting") {
            vpnBtn.setText(context!!.getString(R.string.connecting))
        } else if (status == "connected") {
            vpnBtn.setText(context!!.getString(R.string.disconnect))
        } else if (status == "tryDifferentServer") {
            vpnBtn.setBackgroundResource(R.drawable.button_connected)
            vpnBtn.setText("Try Different\nServer")
        } else if (status == "loading") {
            vpnBtn.setBackgroundResource(R.drawable.button)
            vpnBtn.setText("Loading Server..")
        } else if (status == "invalidDevice") {
            vpnBtn.setBackgroundResource(R.drawable.button_connected)
            vpnBtn.setText("Invalid Device")
        } else if (status == "authenticationCheck") {
            vpnBtn.setBackgroundResource(R.drawable.button_connecting)
            vpnBtn.setText("Authentication \n Checking...")
        }
    }

    /**
     * Receive broadcast message
     */
    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                setStatus(intent.getStringExtra("state"))
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                var duration = intent.getStringExtra("duration")
                var lastPacketReceive = intent.getStringExtra("lastPacketReceive")
                var byteIn = intent.getStringExtra("byteIn")
                var byteOut = intent.getStringExtra("byteOut")
                if (duration == null) duration = "00:00:00"
                if (lastPacketReceive == null) lastPacketReceive = "0"
                if (byteIn == null) byteIn = " "
                if (byteOut == null) byteOut = " "
                updateConnectionStatus(duration, lastPacketReceive, byteIn, byteOut)
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    /**
     * Update status UI
     * @param duration: running time
     * @param lastPacketReceive: last packet receive time
     * @param byteIn: incoming data
     * @param byteOut: outgoing data
     */
    fun updateConnectionStatus(duration: String, lastPacketReceive: String, byteIn: String, byteOut: String) {

        durationTv.setText("Duration: $duration")
        lastPacketReceiveTv.setText("Packet Received: $lastPacketReceive second ago")
        byteInTv.setText("Bytes In: $byteIn")
        byteOutTv.setText("Bytes Out: $byteOut")
    }


    /**
     * Show toast message
     * @param message: toast message
     */
    fun showToast(message: String?) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }


    /**
     * VPN server country icon change
     * @param serverIcon: icon URL
     */
    fun updateCurrentServerIcon(serverIcon: String?) {
        Glide.with(context!!)
                .load(serverIcon)
                .into(selectedServerIcon)
    }

    /**
     * Change server when user select new server
     * @param server ovpn server details
     */
    override fun newServer(server: Server) {
        this.server = server
        updateCurrentServerIcon(server.flagUrl)

        // Stop previous connection
        if (vpnStart) {
            stopVpn()
        }
        prepareVpn()
    }

    override fun onResume() {
        if (server == null) {
            server = preference!!.server
        }
        super.onResume()
    }

    /**
     * Save current selected server on local shared preference
     */
    override fun onStop() {
        if (server != null) {
            preference!!.saveServer(server)
        }
        super.onStop()
    }
}