package com.holy.fast.vpn.model

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FirebaseServer : Server {
    var conf : String = ""

    constructor()
    constructor(country: String?, flagUrl: String?, ovpn: String?, ovpnUserName: String?, ovpnUserPassword: String?) : super(country, flagUrl, ovpn, ovpnUserName, ovpnUserPassword) {}
    override fun getConnectionString(context: Context): String {
//        val conf : String?



        val dataRef = FirebaseDatabase.getInstance().reference.child("file")
        dataRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
//                Log.d(TAG, snapshot.value as String)
                if (snapshot.exists()){
                    conf = snapshot.value as String
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG, error.message)
            }

        })

//        return conf
        return  conf
    }
}