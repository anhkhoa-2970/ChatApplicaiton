package com.example.chatapplication.other

import android.app.AlertDialog
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.codingwithme.firebasechat.Constants.Constants
import com.example.chatapplication.R
import com.example.chatapplication.data.entities.Message
import com.example.chatapplication.databinding.DeleteLayoutBinding
import com.example.chatapplication.databinding.ItemReceiveBinding
import com.example.chatapplication.databinding.ItemSendBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.scottyab.aescrypt.AESCrypt
import java.io.IOException
import java.security.GeneralSecurityException

class MessengerAdapter(
    val context: Context,
    messengeList: ArrayList<Message>,
    senderRoom: String,
    receiverRom: String,
    imageUrl: String
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var messages: ArrayList<Message>
    var mediaPlayer : MediaPlayer? = null
    val ITEM_RECEIVE = 1
    val ITEM_SEND = 2
    var senderRoom: String
    var receiverRoom: String
    var imageUrl: String
    var isPlaying = 1


    init {
        if (messengeList != null) {
            this.messages = messengeList
        }
        this.senderRoom = senderRoom
        this.receiverRoom = receiverRom
        this.imageUrl = imageUrl

    }

    class SendViewHolder(binding: ItemSendBinding) : RecyclerView.ViewHolder(binding.root) {
        val sendMessage = binding.tvMessageSend
        val image = binding.imgFromSender
        val mLinear = binding.mLinear
        val llPlayAudio = binding.llPlayMedia
        val playMedia = binding.imgPlay
        val timer = binding.tvTime
    }

    class ReceiveViewHolder(binding: ItemReceiveBinding) : RecyclerView.ViewHolder(binding.root) {
        val receiveMessage = binding.tvMessageReceive
        val receiverAvatar = binding.imgAvatarReceiver
        val image = binding.imgFromReceiver
        val mLinear = binding.mLinear
        val llPlayAudio = binding.llPlayMedia
        val playMedia = binding.imgPlay
        val timer = binding.tvTime
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 1) {
            val binding =
                ItemReceiveBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ReceiveViewHolder(binding)
        } else {
            val binding =
                ItemSendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SendViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessenge = messages[position]
        if (holder.javaClass == SendViewHolder::class.java) {
            val viewHolder = holder as SendViewHolder
            if (currentMessenge.message.equals("Photo")) {
                viewHolder.sendMessage.visibility = View.GONE
                viewHolder.mLinear.visibility = View.GONE
                viewHolder.image.visibility = View.VISIBLE
                Glide.with(context).load(currentMessenge.imageUrl)
                    .placeholder(R.drawable.image_holder).into(viewHolder.image)
            }else if (currentMessenge.message.equals("Audio") or currentMessenge.message.equals("AudioRecord")) {
                viewHolder.sendMessage.visibility = View.GONE
                viewHolder.mLinear.visibility = View.GONE
                viewHolder.llPlayAudio.visibility = View.VISIBLE
                viewHolder.playMedia.setOnClickListener {
                    if(isPlaying== 1){
                        playMedia(currentMessenge.imageUrl!!)
                        isPlaying = 2
                    }else{
                        stopMedia()
                        isPlaying = 1
                    }
                    isPlaying = 1
                }
            }else{
                viewHolder.sendMessage.text = decrypt(currentMessenge.message!!)
            }

            viewHolder.itemView.setOnLongClickListener {
                val view = LayoutInflater.from(context).inflate(R.layout.delete_layout, null)
                val binding: DeleteLayoutBinding = DeleteLayoutBinding.bind(view)
                val dialog =
                    AlertDialog.Builder(context).setTitle("Delete Message").setView(binding.root)
                        .create()
                binding.tvDeleteEveryone.setOnClickListener {

                    currentMessenge.messageId?.let { it1 ->
                        FirebaseDatabase.getInstance().getReference("chats").child(senderRoom)
                            .child("message").child(it1).setValue(null)
                    }
                    currentMessenge.messageId?.let { it1 ->
                        FirebaseDatabase.getInstance().getReference("chats").child(receiverRoom)
                            .child("message").child(it1).setValue(null)
                    }
                    dialog.dismiss()
                }
                binding.tvDeleteForMe.setOnClickListener {
                    currentMessenge.messageId?.let { it1 ->
                        FirebaseDatabase.getInstance().reference.child("chats").child(senderRoom)
                            .child("message").child(it1).setValue(null)
                    }
                    dialog.dismiss()
                }
                binding.tvCancel.setOnClickListener {
                    dialog.dismiss()
                }
                dialog.show()
                false
            }


        } else {
            val viewHolder = holder as ReceiveViewHolder
            if (currentMessenge.message.equals("Photo")) {
                viewHolder.receiveMessage.visibility = View.GONE
                viewHolder.mLinear.visibility = View.GONE
                viewHolder.image.visibility = View.VISIBLE
                Glide.with(context).load(currentMessenge.imageUrl)
                    .placeholder(R.drawable.image_holder).into(viewHolder.image)
            }else if (currentMessenge.message.equals("Audio") || currentMessenge.message.equals("AudioRecord")) {
                viewHolder.receiveMessage.visibility = View.GONE
                viewHolder.mLinear.visibility = View.GONE
                viewHolder.llPlayAudio.visibility = View.VISIBLE
                viewHolder.playMedia.setOnClickListener {
                    if(isPlaying== 1){
                        playMedia(currentMessenge.imageUrl!!)
                        isPlaying = 2
                    }else{
                        stopMedia()
                        isPlaying = 1
                    }
                    isPlaying = 1
                }
            }else{
                Glide.with(context).load(imageUrl).placeholder(R.drawable.image_avatar)
                    .into(viewHolder.receiverAvatar)
                viewHolder.receiveMessage.text = decrypt(currentMessenge.message!!)
            }

            viewHolder.itemView.setOnLongClickListener {
                val view = LayoutInflater.from(context).inflate(R.layout.delete_layout, null)
                val binding: DeleteLayoutBinding = DeleteLayoutBinding.bind(view)
                val dialog =
                    AlertDialog.Builder(context).setTitle("Delete Message").setView(binding.root)
                        .create()
                binding.tvDeleteEveryone.setOnClickListener {
                    currentMessenge.message = "This message is removed"
                    //val message = Message(currentMessenge.message,senderID = null,null)
                    currentMessenge.messageId?.let { it1 ->
                        FirebaseDatabase.getInstance().getReference("chats").child(senderRoom)
                            .child("message").child(it1).setValue(null)
                    }
                    currentMessenge.messageId?.let { it1 ->
                        FirebaseDatabase.getInstance().getReference("chats").child(receiverRoom)
                            .child("message").child(it1).setValue(null)
                    }
                    dialog.dismiss()
                }
                binding.tvDeleteForMe.setOnClickListener {
                    currentMessenge.messageId?.let { it1 ->
                        FirebaseDatabase.getInstance().reference.child("chats").child(senderRoom)
                            .child("message").child(it1).setValue(null)
                    }
                    dialog.dismiss()
                }
                binding.tvCancel.setOnClickListener {
                    dialog.dismiss()
                }
                dialog.show()
                false
            }
        }

    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun getItemViewType(position: Int): Int {
        val currentMessage = messages[position]
        if (FirebaseAuth.getInstance().currentUser!!.uid.equals(currentMessage.senderID)) {
            return ITEM_SEND
        } else {
            return ITEM_RECEIVE
        }
    }

    private fun playMedia(audioUrl : String){
        mediaPlayer = MediaPlayer()
        mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
        try {
            mediaPlayer!!.setDataSource(audioUrl)
            mediaPlayer!!.prepare()
            mediaPlayer!!.start()
        }catch ( e : IOException){
            e.printStackTrace()
        }
    }
    private fun stopMedia(){
        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
        }
    }

    @Throws(GeneralSecurityException::class)
    fun encrypt(text: String): String {
        return AESCrypt.encrypt(Constants.KEY_ENCYPTION, text)
    }

    @Throws(GeneralSecurityException::class)
    fun decrypt(text: String): String {
        return AESCrypt.decrypt(Constants.KEY_ENCYPTION, text)
    }
}