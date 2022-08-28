package com.example.chatapplication.activity

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.chatapplication.R
import com.example.chatapplication.data.entities.User
import com.example.chatapplication.databinding.ActivityMainBinding
import com.example.chatapplication.firebase.FirebaseService
import com.example.chatapplication.other.UserAdapter
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var adapter: UserAdapter
    private lateinit var mDbRef: DatabaseReference
    private lateinit var userList : ArrayList<User>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


//        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
//            if (!task.isSuccessful) {
//                Log.w("TAG", "Fetching FCM registration token failed", task.exception)
//                return@OnCompleteListener
//            }
//            FirebaseService.token = task.result
//            // Get new FCM registration token
//        })
        FirebaseService.sharedPref = getSharedPreferences("sharedPref",Context.MODE_PRIVATE)
        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
            FirebaseService.token = it.token
        }


        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().getReference()
        var userId = mAuth.currentUser!!.uid
        FirebaseMessaging.getInstance().subscribeToTopic("/topics/$userId")

        userList = ArrayList()
        adapter = UserAdapter(this,userList)
        binding.rvMessager.layoutManager = LinearLayoutManager(this)
        binding.rvMessager.adapter = adapter

        mDbRef.child("user").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for(postSnapshot in snapshot.children){
                    val currentUser = postSnapshot.getValue(User::class.java)
                    if(mAuth.currentUser?.uid == currentUser?.uid){
                        if (currentUser!!.avatar == ""){
                            binding.imgProfile.setImageResource(R.drawable.image_avatar)
                        }else{
                            Glide.with(this@MainActivity).load(currentUser.avatar).into(binding.imgProfile)
                        }
                    }else{
                        var userId = currentUser!!.uid
                        FirebaseMessaging.getInstance().subscribeToTopic("/topics/$userId")
                        userList.add(currentUser!!)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, error.message,Toast.LENGTH_SHORT).show()
            }
        })
        binding.imgProfile.setOnClickListener {
            val intent = Intent(this, SetupProfileActivity::class.java)
            startActivity(intent)
        }

        binding.imgLogout.setOnClickListener {
            mAuth.signOut()
            val intent = Intent(this, LogIn::class.java)
            finish()
            startActivity(intent)
        }
    }
}