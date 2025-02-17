package com.expapps.vibex.auth

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.expapps.vibex.FirebaseSource
import com.expapps.vibex.Utils
import com.expapps.vibex.Utils.openActivity
import com.expapps.vibex.Utils.showToast
import com.expapps.vibex.MainActivity
import com.expapps.vibex.isStringsNotNullOrEmpty
import com.expapps.vibex.toText
import com.expapps.vibex.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var firebaseSource: FirebaseSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
    }


    private fun init() {
        firebaseSource = FirebaseSource()
        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setTitle("VibeX")
        progressDialog.setMessage("Logging in...")
    }


    private fun initViews() {
        init()
        binding.registerBtn.setOnClickListener {
            val email = binding.emailEt.toText()
            val password = binding.passwordEt.toText()
            val confirmPassword = binding.confirmPasswordEt.toText()
            if (isStringsNotNullOrEmpty(email, password, confirmPassword)) {
                if (password.length > 6) {
                    if (password == confirmPassword) {
                        login(email, password)
                    } else {
                        Toast.makeText(this, "Password does not match", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Password should be more than 6 characters", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "All the fields are mandatory", Toast.LENGTH_SHORT).show()
            }
        }

        binding.adminBtn.setOnClickListener {
            openActivity(LoginActivity::class.java, finishPrev = true)
        }
    }

    private fun login(email: String, password: String) {
        val user = User(email = email, password = password)
        checkIfUserExist(user)
    }


    private fun register(user: User) {
        firebaseAuth.createUserWithEmailAndPassword(user.email ?: "", user.password ?: "")
            .addOnSuccessListener {
                dismissProgress()
                user.userId = it.user?.uid
                user.mcode = Utils.getMCodeFromUserId(user.userId ?: "")
                addUser(user)
            }
            .addOnFailureListener {
                dismissProgress()
                showToast("Registration error")
            }
    }

    private fun checkIfUserExist(user: User) {
        showProgress()
        firebaseAuth.fetchSignInMethodsForEmail(user.email ?: "")
            .addOnSuccessListener {
                if (!it.signInMethods.isNullOrEmpty()) {
                    dismissProgress()
                    showToast("Error: User already exist")
                } else {
                    register(user)
                }
            }
    }

    private fun addUser(user: User) {
        showProgress()
        firebaseSource.addUser(user).observe(this) {
            if (it == true) {
                showToast("User Registered")
                finish()
            } else {
                showToast("Unknown error occurred")
            }
        }
    }

    private fun showProgress() {
        progressDialog.show()
    }

    private fun dismissProgress() {
        progressDialog.dismiss()
    }
}