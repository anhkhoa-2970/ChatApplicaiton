package com.example.chatapplication.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.example.chatapplication.databinding.ActivityLogInBinding
import com.google.firebase.auth.FirebaseAuth

class LogIn : AppCompatActivity() {
    private lateinit var binding: ActivityLogInBinding
    private lateinit var mAuth: FirebaseAuth
    var emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        actionBar?.hide()

        mAuth = FirebaseAuth.getInstance()

        binding.tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }
        binding.btnLogin.setOnClickListener {
            val email = binding.edtMail.text.toString()
            val password = binding.edtPassword.text.toString()
            if (checkInput(email, password)) {
                Toast.makeText(this, "Information cannot be empty", Toast.LENGTH_SHORT).show()
            } else if (email.trim { it <= ' ' }.matches(emailPattern.toRegex())) {
                logIn(email, password)
            }else{
                Toast.makeText(this,"Incorrect information. Please check again!!",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logIn(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this, MainActivity::class.java)
                    finish()
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "User does not exist", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun checkInput(email: String, password: String): Boolean {
        return (TextUtils.isEmpty(email) || TextUtils.isEmpty(password))
    }
}