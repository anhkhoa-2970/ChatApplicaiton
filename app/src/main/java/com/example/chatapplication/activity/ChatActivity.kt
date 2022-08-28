@file:Suppress("DEPRECATION", "UnusedEquals")

package com.example.chatapplication.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.codingwithme.firebasechat.Constants.Constants
import com.example.chatapplication.R
import com.example.chatapplication.RetrofitInstance
import com.example.chatapplication.data.entities.Message
import com.example.chatapplication.data.entities.NotificationData
import com.example.chatapplication.data.entities.PushNotification
import com.example.chatapplication.databinding.ActivityChatBinding
import com.example.chatapplication.other.MessengerAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.scottyab.aescrypt.AESCrypt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.text.SimpleDateFormat
import java.util.*


class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var messengerAdapter: MessengerAdapter
    private lateinit var messengeList: ArrayList<Message>
    private lateinit var mdbDef: DatabaseReference
    private lateinit var mediaRecorder: MediaRecorder

    // public lateinit var receiverImage : String
    private var storage: FirebaseStorage? = null
    private var dialog: ProgressDialog? = null

    private var receiverRoom: String? = null
    private var senderRoom: String? = null
    private var receiverUid: String? = null
    private var senderUid: String? = null
    private var audioPath: String? = null
    private var topic = ""

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        FirebaseService.sharedPref = getSharedPreferences("sharedPref",Context.MODE_PRIVATE)
//        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
//            FirebaseService.token = it.token
//        }
//
//        val userId = FirebaseAuth.getInstance().currentUser!!.uid
//        FirebaseMessaging.getInstance().subscribeToTopic("/topics/$userId")

        requestPermission()
        messengeList = ArrayList()
        storage = FirebaseStorage.getInstance()
        dialog = ProgressDialog(this@ChatActivity)
        dialog!!.setMessage("Uploading ...")
        dialog!!.setCancelable(false)

        receiverUid = intent.getStringExtra("uid")
        senderUid = FirebaseAuth.getInstance().uid!!
        senderRoom = senderUid + receiverUid
        receiverRoom = receiverUid + senderUid

        val name = intent.getStringExtra("name")
        val avatarReceiver = intent.getStringExtra("avatar")
        if (avatarReceiver == "") {
            binding.imgProfile.setImageResource(R.drawable.image_avatar)
        } else {
            Glide.with(this).load(avatarReceiver).into(binding.imgProfile)
        }
        binding.imgBack.setOnClickListener {
            finish()
        }
        binding.tvUserName.text = name
        mdbDef = FirebaseDatabase.getInstance().getReference()


        FirebaseDatabase.getInstance().getReference("Presence").child(receiverUid!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val status = snapshot.getValue(String::class.java)
                        if (status == "offline") {
                            binding.status.visibility = View.GONE
                        } else {
                            binding.status.setText(status)
                            binding.status.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        messengerAdapter =
            MessengerAdapter(this, messengeList, senderRoom!!, receiverRoom!!, avatarReceiver!!)
        binding.rvChat.layoutManager = LinearLayoutManager(this)
        binding.rvChat.adapter = messengerAdapter


        mdbDef.child("chats").child(senderRoom!!).child("message")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messengeList.clear()

                    for (snapShot in snapshot.children) {
                        val message = snapShot.getValue(Message::class.java)
                        message!!.messageId = snapShot.key
                        messengeList.add(message)
                    }
                    messengerAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })

        binding.btnSendMessage.setOnClickListener {
            val messageTxt = binding.etMessage.text.toString()
            if (messageTxt.isEmpty()) {
                Toast.makeText(applicationContext, "Message is empty", Toast.LENGTH_SHORT).show()
            } else {
                val now = Date()
                val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
                val messageObject = Message(encrypt(messageTxt), senderUid!!,null, formatter.format(now))
                binding.etMessage.setText("")
                val randomKey = mdbDef.push().key
                val lastMsgObj = HashMap<String, Any>()
                lastMsgObj["lastMsg"] = messageObject.message!!
                lastMsgObj["lastMsgTime"] = formatter.format(now)
                mdbDef.child("chats").child(senderRoom!!).updateChildren(lastMsgObj)
                mdbDef.child("chats").child(receiverRoom!!).updateChildren(lastMsgObj)
                mdbDef.child("chats").child(senderRoom!!).child("message").child(randomKey!!)
                    .setValue(messageObject)
                    .addOnSuccessListener {
                        mdbDef.child("chats").child(receiverRoom!!).child("message")
                            .child(randomKey)
                            .setValue(messageObject).addOnSuccessListener { }
                    }
                FirebaseDatabase.getInstance().getReference("user").child(senderUid!!).get()
                    .addOnSuccessListener {
                        val nameOfCurrentUser = it.child("name").value.toString()
                        topic = "/topics/$receiverUid"
                        PushNotification(
                            NotificationData(nameOfCurrentUser, messageTxt),
                            topic
                        ).also {
                            sendNotification(it)
                        }
                    }
            }
            it.hideKeyboard()
        }

        binding.recordButton.setOnTouchListener { _, motionEvent ->
            if (motionEvent?.action == MotionEvent.ACTION_DOWN) {
                startRecording()
            } else if (motionEvent?.action == MotionEvent.ACTION_UP) {
                stopRecording()
            }
            false
        }

        binding.imgLink.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "audio/*"
            startActivityForResult(intent, 2)
        }

        binding.imgChoosenImage.setOnClickListener {
            requestPermission()
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }

        val handler = Handler()
        binding.etMessage.addTextChangedListener {
            object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    mdbDef.child("Presence").child(senderUid!!).setValue("typing...")
                    handler.removeCallbacksAndMessages(null)
                    handler.postDelayed(userStoppedTyping, 1000)
                }

                override fun afterTextChanged(p0: Editable?) {
                }

                var userStoppedTyping = Runnable {
                    mdbDef.child("Presence").child(senderUid!!).setValue("Online")
                }
            }
        }
    }

    // for upload image
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (data?.data != null) {
                val item = data.data
                val calendar = Calendar.getInstance()
                val reference = storage!!.reference.child("chats")
                    .child(calendar.timeInMillis.toString() + "")
                dialog!!.show()
                reference.putFile(item!!)
                    .addOnCompleteListener { task ->
                        dialog!!.dismiss()
                        if (task.isSuccessful) {
                            reference.downloadUrl.addOnSuccessListener { uri ->
                                val filePath = uri.toString()
                                val messageTxt: String = binding.etMessage.text.toString()
                                val now = Date()
                                val formatter =
                                    SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
                                val message =
                                    Message(messageTxt, senderUid!!,null ,formatter.format(now))
                                message.message = "Photo"
                                message.imageUrl = filePath
                                binding.etMessage.setText("")
                                val randomKey = mdbDef.push().key
                                val lastMsgObj = HashMap<String, Any>()
                                lastMsgObj["lastMsg"] = message.message!!
                                lastMsgObj["lastMsgTime"] = formatter.format(now)
                                mdbDef.child("chats").updateChildren(lastMsgObj)
                                mdbDef.child("chats").child(receiverRoom!!)
                                    .updateChildren(lastMsgObj)
                                mdbDef.child("chats").child(senderRoom!!).child("message")
                                    .child(randomKey!!).setValue(message).addOnSuccessListener {
                                        mdbDef.child("chats").child(receiverRoom!!).child("message")
                                            .child(randomKey).setValue(message)
                                            .addOnSuccessListener { }
                                    }
                            }
                        }
                    }
            }
        }
        if (requestCode == 2) {
            if (data?.data != null) {
                val item = data.data
                val calendar = Calendar.getInstance()
                val reference = storage!!.reference.child("audios")
                    .child(calendar.timeInMillis.toString() + "")
                dialog!!.show()
                reference.putFile(item!!)
                    .addOnCompleteListener { task ->
                        dialog!!.dismiss()
                        if (task.isSuccessful) {
                            reference.downloadUrl.addOnSuccessListener { uri ->
                                val filePath = uri.toString()
                                val messageTxt: String = binding.etMessage.text.toString()
                                val now = Date()
                                val formatter =
                                    SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
                                val message =
                                    Message(messageTxt, senderUid!!,null ,formatter.format(now))
                                message.message = "Audio"
                                message.imageUrl = filePath
                                binding.etMessage.setText("")
                                val randomKey = mdbDef.push().key
                                val lastMsgObj = HashMap<String, Any>()
                                lastMsgObj["lastMsg"] = message.message!!
                                lastMsgObj["lastMsgTime"] = formatter.format(now)
                                mdbDef.child("chats").updateChildren(lastMsgObj)
                                mdbDef.child("chats").child(receiverRoom!!)
                                    .updateChildren(lastMsgObj)
                                mdbDef.child("chats").child(senderRoom!!).child("message")
                                    .child(randomKey!!).setValue(message).addOnSuccessListener {
                                        mdbDef.child("chats").child(receiverRoom!!).child("message")
                                            .child(randomKey).setValue(message)
                                            .addOnSuccessListener { }
                                    }
                            }
                        }
                    }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val currentId = FirebaseAuth.getInstance().uid
        mdbDef.child("Presence").child(currentId!!)
            .setValue("offline")
    }

    override fun onResume() {
        super.onResume()
        val currentId = FirebaseAuth.getInstance().uid
        mdbDef.child("Presence").child(currentId!!)
            .setValue("Online")
    }

    fun View.hideKeyboard() {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun hasCameraPermission() =
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private fun hasReadExternalStorage() =
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    private fun hasRecordAudio() =
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    private fun hasWriteExternalStorage() =
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission() {
        val permissionToRequest = mutableListOf<String>()
        if (!hasCameraPermission()) {
            permissionToRequest.add(Manifest.permission.CAMERA)
        }
        if (!hasReadExternalStorage()) {
            permissionToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (!hasRecordAudio()) {
            permissionToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (!hasWriteExternalStorage()){
            permissionToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
            if (permissionToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissionToRequest.toTypedArray(), 0)
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && grantResults.isNotEmpty()) {
            for (i in grantResults.indices) {
                grantResults[i] == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    private fun sendNotification(notification: PushNotification) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.postNotification(notification)
                if (response.isSuccessful) {
                    Log.d("TAG", "Response: ${Gson().toJson(response)}")
                } else {
                    Log.e("TAG", response.errorBody()!!.string())
                }
            } catch (e: Exception) {
                Log.e("TAG", e.toString())
            }
        }

    private fun startRecording() {
        mediaRecorder = MediaRecorder()
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB)

        val file =
            File(Environment.getExternalStorageDirectory().absolutePath,"ChatApplication/images" )

        if (!file.exists()) file.mkdirs()
        audioPath = Environment.getExternalStorageDirectory().absolutePath +"/"+"recording.3gp"

        mediaRecorder.setOutputFile(audioPath)
        try {
            mediaRecorder.prepare()
            mediaRecorder.start()
        }catch (e : IOException){
            e.printStackTrace()
        }
    }

    private fun stopRecording(){
        try {
            mediaRecorder.stop()
            mediaRecorder.release()
        } catch (stopException : RuntimeException ) {
            stopException.printStackTrace()
        }
        sendRecodingMessage(audioPath!!)
    }

    private fun sendRecodingMessage(audioPath: String) {
        val file = File(audioPath)
        if (file != null){
            Toast.makeText(this,"File exitst",Toast.LENGTH_SHORT).show()
        }
        if (receiverUid == null) Toast.makeText(
            this,
            "Send simple message first",
            Toast.LENGTH_SHORT
        )
            .show() else {
            val calendar = Calendar.getInstance()
            val reference = storage!!.reference.child("audios")
                .child(calendar.timeInMillis.toString() + "")
            val audioFile: Uri = Uri.fromFile(file)
            reference.putFile(audioFile)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful)
                        reference.downloadUrl.addOnSuccessListener { uri ->
                            val filePath = uri.toString()
                            val messageTxt: String = binding.etMessage.text.toString()
                            val now = Date()
                            val formatter =
                                SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
                            val message =
                                Message(messageTxt, senderUid!!,null ,formatter.format(now))
                            message.message = "AudioRecord"
                            message.imageUrl = filePath
                            binding.etMessage.setText("")
                            val randomKey = mdbDef.push().key
                            val lastMsgObj = HashMap<String, Any>()
                            lastMsgObj["lastMsg"] = message.message!!
                            lastMsgObj["lastMsgTime"] = formatter.format(now)
                            mdbDef.child("chats").updateChildren(lastMsgObj)
                            mdbDef.child("chats").child(receiverRoom!!)
                                .updateChildren(lastMsgObj)
                            mdbDef.child("chats").child(senderRoom!!).child("message")
                                .child(randomKey!!).setValue(message).addOnSuccessListener {
                                    mdbDef.child("chats").child(receiverRoom!!).child("message")
                                        .child(randomKey).setValue(message)
                                        .addOnSuccessListener { }
                                }
                        }
                }.addOnFailureListener{
                    Toast.makeText(this,"Load audio failed",Toast.LENGTH_SHORT).show()
                }
        }
    }

    @Throws(GeneralSecurityException::class)
    fun encrypt(text: String): String {
        return AESCrypt.encrypt(Constants.KEY_ENCYPTION, text)
    }
}