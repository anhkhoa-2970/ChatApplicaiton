package com.example.chatapplication.data.entities

class Message{
    var messageId : String? = null
    var senderID: String? = null
    var message :String? = null
    var imageUrl: String? = null
    var url : String? = null
  //  var timeStap : Long = 0
    var timeStap: String? = null
    constructor(){}
    constructor(message : String?,senderID : String?,url: String?,timeStap: String?){
        this.message = message
        this.senderID = senderID
        this.timeStap = timeStap
    }
}