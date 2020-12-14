package com.holy.fast.vpn.model

import android.content.Context

class AssetServer : Server {
    constructor()
    constructor(country: String?, flagUrl: String?, ovpn: String?, ovpnUserName: String?, ovpnUserPassword: String?) : super(country, flagUrl, ovpn, ovpnUserName, ovpnUserPassword) {}

    override fun getConnectionString(context: Context): String {
        val conf = context.assets.open(ovpn)
        return conf.bufferedReader().lineSequence().joinToString(separator = "\n")
    }
}