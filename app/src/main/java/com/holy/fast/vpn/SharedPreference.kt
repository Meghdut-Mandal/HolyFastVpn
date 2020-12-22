package com.holy.fast.vpn

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.database.FirebaseDatabase
import com.holy.fast.vpn.model.AssetServer
import com.holy.fast.vpn.model.Server
import com.holy.fast.vpn.model.StaticServer

class SharedPreference(context: Context) {
    private val mPreference: SharedPreferences
    private val mPrefEditor: SharedPreferences.Editor
    private val context: Context

    private var server: Server? = null

    /**
     * Save server details
     *
     * @param server details of ovpn server
     */
    fun saveServer(server: Server) {
        mPrefEditor.putString(SERVER_COUNTRY, server.country)
        mPrefEditor.putString(SERVER_FLAG, server.flagUrl)
        mPrefEditor.putString(SERVER_OVPN, server.ovpn)
        mPrefEditor.putString(SERVER_OVPN_USER, server.ovpnUserName)
        mPrefEditor.putString(SERVER_OVPN_PASSWORD, server.ovpnUserPassword)
        mPrefEditor.commit()
    }//                getFirebaseServer();

    /**
     * Get server data from shared preference
     *
     * @return server model object
     */
    fun getServer(): Server {
        return assetServer

    }

    private val assetServer: Server
        get() {
            return AssetServer(
                    "India",
                    Utils.getImgURL(R.drawable.uk_flag),
                    "kiit_global_servervpn.ovpn",
                    "global",
                    "1234567890")
        }

    companion object {
        private const val APP_PREFS_NAME = "CakeVPNPreference"
        private const val SERVER_COUNTRY = "server_country"
        private const val SERVER_FLAG = "server_flag"
        private const val SERVER_OVPN = "server_ovpn"
        private const val SERVER_OVPN_USER = "server_ovpn_user"
        private const val SERVER_OVPN_PASSWORD = "server_ovpn_password"
    }

    init {
        mPreference = context.getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
        mPrefEditor = mPreference.edit()
        this.context = context
//        FirebaseDatabase.getInstance().reference.child("openvpn").child("server1").get().addOnCompleteListener {
//            it.result?.getValue(StaticServer::class.java)?.also {
//                server = it
//                println("com.holy.fast.vpn>>SharedPreference> Server loaded ")
//            }
//        }
    }
}