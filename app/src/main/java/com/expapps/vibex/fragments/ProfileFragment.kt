package com.expapps.vibex.fragments

import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.expapps.vibex.FirebaseSource
import com.expapps.vibex.R
import com.expapps.vibex.Utils.openActivity
import com.expapps.vibex.activities.UploadSongActivity
import com.expapps.vibex.auth.LoginActivity
import com.expapps.vibex.databinding.FragmentProfileBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding

    private lateinit var firebaseSource: FirebaseSource

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(inflater)
        firebaseSource = FirebaseSource()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginBtn.setOnClickListener {
            requireActivity().openActivity(LoginActivity::class.java)
        }
        binding.logoutBtn.setOnClickListener {
            firebaseSource.logout()
            Handler().postDelayed({ checkLogin() }, 100)
        }
        binding.uploadSongBtn.setOnClickListener {
            requireActivity().openActivity(UploadSongActivity::class.java)
        }
    }

    override fun onResume() {
        super.onResume()
        checkLogin()
    }

    private fun checkLogin() {
        if (isLoggedIn()) {
            setEmail()
            binding.profileDetailsLayout.visibility = View.VISIBLE
            binding.errorDetailsLayout.visibility = View.GONE
            binding.loginBtn.visibility = View.GONE
            binding.logoutBtn.visibility = View.VISIBLE
        } else {
            binding.profileDetailsLayout.visibility = View.GONE
            binding.errorDetailsLayout.visibility = View.VISIBLE
            binding.loginBtn.visibility = View.VISIBLE
            binding.logoutBtn.visibility = View.GONE
        }
    }

    private fun setEmail() {
        binding.loggedInTv.text = "Logged In as - ${firebaseSource.getCurrentUserEmail()}"
    }

    private fun isLoggedIn(): Boolean {
        return firebaseSource.isLoggedIn()
    }
}