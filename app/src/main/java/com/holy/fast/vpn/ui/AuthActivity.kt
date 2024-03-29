package com.holy.fast.vpn.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.holy.fast.vpn.R
import com.holy.fast.vpn.SharedPreference
import kotlinx.android.synthetic.main.activity_auth.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthActivity : AppCompatActivity() {

    private val RC_SIGN_IN: Int = 123
    private val TAG = "SignInActivity Tag"
    private val gso by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
    }
    private val googleSignInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(this, gso)
    }
    private var inAuth = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        googleSignInClient.
        if (FirebaseAuth.getInstance().currentUser != null) {

            startMain()
        }
        setContentView(R.layout.activity_auth)
        signInButton.setOnClickListener {
            signIn()
        }
    }

    override fun onResume() {
        super.onResume()
        if (FirebaseAuth.getInstance().currentUser != null) {
            signInButton.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
        }
    }


    private fun signIn() {
        if (inAuth) return
        inAuth = true

        signInButton.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)!!
            Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
            firebaseAuthWithGoogle(account.idToken!!)
        }
        catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
        }
        inAuth = false

    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        GlobalScope.launch(Dispatchers.IO) {
            val auth = Firebase.auth.signInWithCredential(credential).await()
            val firebaseUser = auth.user
            withContext(Dispatchers.Main) {
                updateUI(firebaseUser)
            }
        }

    }

    private fun updateUI(firebaseUser: FirebaseUser?) {
        if (firebaseUser != null) {
            FirebaseDatabase.getInstance().reference.child("check").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = snapshot.getValue(Boolean::class.java) ?: false
                    if (value){
                        verifyRegistration(firebaseUser)
                    }else {
                        startMain()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    cancelAuth()
                }
            })

        } else {
            cancelAuth()
        }
    }

    private fun cancelAuth() {
        googleSignInClient.signOut()
        FirebaseAuth.getInstance().signOut()
        inAuth = false
        signInButton.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
    }

    private fun verifyRegistration(firebaseUser: FirebaseUser) {
        val email = firebaseUser.email?.replace("@", "_")?.replace(".", "_") ?: "Not Found"
        FirebaseDatabase.getInstance().reference.child("user").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChild(email)) {
                    inAuth = false
                    startMain()
                } else {
                    cancelAuth()
                    Snackbar.make(progressBar, "This Vpn is only for special members.Sorry ;(", Snackbar.LENGTH_LONG).show()
                }
                inAuth = false
            }

            override fun onCancelled(error: DatabaseError) {
                cancelAuth()
            }
        })
    }

    private fun startMain() {
        val intent = Intent(this@AuthActivity, HomeActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }


}