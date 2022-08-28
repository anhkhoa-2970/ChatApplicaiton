package com.example.chatapplication.data.entities

import android.graphics.drawable.Drawable
import android.provider.ContactsContract
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_item")
data class User(

    val name: String = "",
    val email: String = "",
    val uid: String = "",
    var avatar: String = ""
//    @ColumnInfo(name="iamge")
//    val image : Drawable?
)