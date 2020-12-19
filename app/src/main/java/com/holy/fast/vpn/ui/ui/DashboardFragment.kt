package com.holy.fast.vpn.ui.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.holy.fast.vpn.R
import com.holy.fast.vpn.ui.AuthActivity
import kotlinx.android.synthetic.main.fragment_dashboard.*

class DashboardFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)

        return root
    }

    override fun onResume() {
        super.onResume()
        log_out.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent= Intent(requireActivity(),AuthActivity::class.java)
            startActivity(intent)
            requireActivity().finishAffinity()
        }
    }
}