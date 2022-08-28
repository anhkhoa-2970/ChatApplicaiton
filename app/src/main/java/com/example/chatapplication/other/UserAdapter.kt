package com.example.chatapplication.other

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapplication.activity.ChatActivity
import com.example.chatapplication.R
import com.example.chatapplication.data.entities.User
import com.example.chatapplication.databinding.ItemUserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class UserAdapter(private val context: Context, private val userList : ArrayList<User>):RecyclerView.Adapter<UserAdapter.UserViewHolder>() {
 //   private val userList : List<User> = emptyList()
    inner class UserViewHolder(val binding: ItemUserBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currUser = userList[position]
        holder.binding.apply {
            tvName.text = currUser.name

            if(currUser?.avatar == ""){
                imvAvatar.setImageResource(R.drawable.image_avatar)
            }else{
                Glide.with(holder.itemView.context).load(currUser?.avatar).into(imvAvatar)
            }
        }
        Glide.with(context).load(currUser.avatar).placeholder(R.drawable.image_avatar).into(holder.binding.imvAvatar)
        holder.itemView.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("name",currUser.name)
            intent.putExtra("uid",currUser.uid)
            intent.putExtra("avatar",currUser.avatar)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return userList.size
    }
}