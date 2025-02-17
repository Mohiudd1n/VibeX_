package com.expapps.vibex.auth

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.expapps.vibex.FirebaseSource
import com.expapps.vibex.Utils.openActivity
import com.expapps.vibex.Utils.showToast
import com.expapps.vibex.MainActivity
import com.expapps.vibex.databinding.ActivityLoginBinding
import com.expapps.vibex.isStringsNotNullOrEmpty
import com.expapps.vibex.toText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseSource: FirebaseSource
    private lateinit var progressDialog: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
    }

    private fun initViews() {
        init()
        binding.loginBtn.setOnClickListener {
            val email = binding.emailEt.toText()
            val password = binding.passwordEt.toText()
            if (isStringsNotNullOrEmpty(email, password)) {
                if (password.length > 6) {
                    val user = User(email = email, password = password)
                    login(user)
                } else {
                    showToast("Password should be more than 6 characters")
                }
            } else {
                showToast("All fields are mandatory")
            }
        }
        binding.adminBtn.setOnClickListener {
            openActivity(RegisterActivity::class.java, finishPrev = true)
        }
    }

    private fun init() {
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseSource = FirebaseSource()
        progressDialog = ProgressDialog(this)
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setTitle("VibeX")
        progressDialog.setMessage("Registering user...")
    }


    private fun login(user: User) {
        showProgress()
        firebaseAuth.signInWithEmailAndPassword(user.email ?: "", user.password ?: "")
            .addOnSuccessListener {
                dismissProgress()
                showToast("Login successful")

                firebaseSource.fetchUserInfo(it.user?.uid ?: "").observe(this) { loginFlowCompleted ->
                    if (loginFlowCompleted != null) {
                        if (!loginFlowCompleted) {
                            finish()
                        }
                    } else {
                        showToast("Success: User already registered")
                    }
                }
            }
            .addOnFailureListener {
                dismissProgress()
                showToast("Login failed, please check credentials")
            }
    }



    private fun showProgress() {
        progressDialog.show()
    }

    private fun dismissProgress() {
        progressDialog.dismiss()
    }


}