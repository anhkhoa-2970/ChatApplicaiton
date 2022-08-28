@file:Suppress("DEPRECATION")

package com.example.chatapplication.activity

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.chatapplication.data.entities.User
import com.example.chatapplication.databinding.ActivitySetupProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class SetupProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetupProfileBinding
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var filePath : Uri
    private val PICK_IMAGE_REQUEST: Int = 2020

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseUser = FirebaseAuth.getInstance().currentUser!!

        binding.imgAvatar.setOnClickListener {
            chooseImage()
        }

        binding.btnSave.setOnClickListener {
            val name = binding.edtName.text.toString()
            uploadImage(name)
            startActivity(Intent(this, MainActivity::class.java))
        }
        binding.btnCancel.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun chooseImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent,"Select Image"),PICK_IMAGE_REQUEST)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK){
            filePath = data?.data!!
            binding.imgAvatar.setImageURI(filePath)
        }
    }
    private fun uploadImage(name:String){
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Uploading ...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss",Locale.getDefault())
        val now = Date()
        val fileName= formatter.format(now)
        val storageFeference = FirebaseStorage.getInstance().getReference("images/$fileName")
        try {
            storageFeference.putFile(filePath).addOnCompleteListener{
                binding.imgAvatar.setImageURI(null)
                storageFeference.downloadUrl.addOnSuccessListener {
                    updateImageAndName(name,it.toString())
                }
                Toast.makeText(this,"Successfully uploaded",Toast.LENGTH_SHORT).show()
                if(progressDialog.isShowing)
                    progressDialog.dismiss()
            }.addOnFailureListener{
                Toast.makeText(this,"Failed",Toast.LENGTH_SHORT).show()
                if(progressDialog.isShowing)
                    progressDialog.dismiss()
            }
        }catch ( e: Exception){
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }
    private fun updateImageAndName(name: String,avatar : String){
        val uid= FirebaseAuth.getInstance().uid?:""
        val email = FirebaseAuth.getInstance().currentUser?.email!!
        val ref = FirebaseDatabase.getInstance().getReference("user/$uid")
        val user = User(name,email,uid,avatar)
        ref.setValue(user)
    }
//    private fun updateData(name: String,email: String,uid: String?) {
//        mDbRef = FirebaseDatabase.getInstance().getReference()
//        mDbRef.child("user").child(uid!!).setValue(User(name, email ,uid))
//    }
}