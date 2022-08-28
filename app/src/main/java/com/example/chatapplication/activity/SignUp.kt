package com.example.chatapplication.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.example.chatapplication.data.entities.User
import com.example.chatapplication.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUp : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference
    var emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        actionBar?.hide()

        mAuth= FirebaseAuth.getInstance()
        binding.btnSignUp.setOnClickListener {
            val name = binding.edtName.text.toString()
            val email = binding.edtMail.text.toString()
            val password = binding.edtPassword.text.toString()
            val confirmPassword = binding.edtConfirmPassword.text.toString()
            if(name.isEmpty()){
                Toast.makeText(this,"Name is not empty",Toast.LENGTH_SHORT).show()
            }
            if(checkInput(email,password)){
                Toast.makeText(this, "Email or password is not empty.", Toast.LENGTH_SHORT)
                    .show()
            }else if(!password.equals(confirmPassword)){
                Toast.makeText(this,"Password not match",Toast.LENGTH_SHORT).show()
            }else if(email.trim{it <= ' '}.matches(emailPattern.toRegex())){
                signUp(name,email,password)
            }
        }

        binding.tvLogin.setOnClickListener {
            val intent = Intent(this, LogIn::class.java)
            startActivity(intent)
        }
    }

    private fun signUp(name:String,email: String, password: String){
        mAuth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener (this){task ->
                if(task.isSuccessful){
                    addUserToDatabase(name,email,mAuth.currentUser?.uid!!)
                    val intent = Intent(this, MainActivity::class.java)
                    finish()
                    startActivity(intent)
                }else{
                    Toast.makeText(this,"Some error occurred", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun addUserToDatabase(name: String, email: String, uid: String?) {
        mDbRef = FirebaseDatabase.getInstance().getReference()
        mDbRef.child("user").child(uid!!).setValue(User(name,email,uid,""))
    }

    private fun checkInput(email: String, password: String) : Boolean{
        return(TextUtils.isEmpty(email)||TextUtils.isEmpty(password))
    }
}