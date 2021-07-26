package com.example.twitterdemo

class PostInfo {
var UserUID:String?=null
    var text:String?=null
    var postImage:String?=null
    var date:String? = null
    constructor(UserUID:String,text:String,postImage:String,date:String){
        this.UserUID = UserUID
        this.text = text
        this.postImage = postImage
        this.date = date
    }
}
