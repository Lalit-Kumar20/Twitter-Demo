package com.example.twitterdemo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.add_ticket.*
import kotlinx.android.synthetic.main.add_ticket.view.*
import kotlinx.android.synthetic.main.tweets_ticket.view.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    var ListTweets = ArrayList<Ticket>()
    var adapter:TweetsAdapter?=null
    var email:String?=null
    var UserUID:String?=null
    private var database = FirebaseDatabase.getInstance()
    private var  myRef = database.getReference()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var b: Bundle? = intent.extras
        if (b != null) {
            email = b.getString("email")
            UserUID = b.getString("uid")
        }
        adapter = TweetsAdapter(this,ListTweets)
        lvTweets.adapter = adapter
        LoadPost()

    }

    inner class TweetsAdapter: BaseAdapter {
        var listNotes = ArrayList<Ticket>()
        var context: Context?=null
        constructor(context: Context, listNotes:ArrayList<Ticket>):super(){
            this.listNotes = listNotes
            this.context = context
        }
        override fun getCount(): Int {
            return listNotes.size
        }

        override fun getItem(position: Int): Any {
            return listNotes[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            var myTweet = listNotes[position]
           if(myTweet.tweetPersonUID.equals("add")){
               var myView = layoutInflater.inflate(R.layout.add_ticket,null)
               myView.ivAttach.setOnClickListener {
                   LoadImage()
               }
               myView.ivPost.setOnClickListener {
                   //upload
                   var df = SimpleDateFormat("yyyy.MM.dd")
                   var date = Date()
                   myRef.child("posts").push().setValue(PostInfo(UserUID!!,myView.etPost.text.toString(),DownloadURL!!,df.format(date)))
                   myView.etPost.setText("")
               }
               return myView

           }
            else if(myTweet.tweetPersonUID.equals("loading")){
               var myView = layoutInflater.inflate(R.layout.loading_ticket, null)
               return myView

           }
            else {
               var myView = layoutInflater.inflate(R.layout.tweets_ticket, null)
               myView.txt_tweet.setText(myTweet.tweetText)
               myView.txt_tweet_date.setText(myTweet.date)
               Picasso.get().load(myTweet.tweetImageURL).into(myView.tweet_picture);

               myRef.child("Users").child(myTweet.tweetPersonUID.toString()).addValueEventListener(object : ValueEventListener{
                   override fun onDataChange(datasnapshot: DataSnapshot) {
                       try {
                               var c  =  datasnapshot!!.value as HashMap<String,String>
                           Log.d("data",c["email"].toString())
                           myView.txtUserName.text = c["email"]
                           Picasso.get().load(c["ProfileImage"]).into(myView.picture_path);

                       }
                       catch (ex:Exception){}
                   }

                   override fun onCancelled(error: DatabaseError) {

                   }

               })
               return myView

           }
        }

    }

    //Load Image
    val PickImageCode = 12;
    fun LoadImage(){
        var intent = Intent()
        intent.setType("image/*")
        intent.setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(Intent.createChooser(intent,"Select Picture"),PickImageCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==PickImageCode  && data!=null && resultCode== RESULT_OK){
            val selectedImage=data.data
            var bitmap:Bitmap = MediaStore.Images.Media.getBitmap(contentResolver,selectedImage)
            UploadImage(bitmap)
            ivAttach.setImageBitmap(bitmap)

        }
    }
     var DownloadURL:String?=null
    fun UploadImage(bitmap:Bitmap){
        ListTweets.add(0, Ticket("0","h","w","loading","tt"))
        adapter!!.notifyDataSetChanged()
        val Storage = FirebaseStorage.getInstance()
        var storageRef = Storage.getReferenceFromUrl("gs://fir-multi-71e05.appspot.com")
        val df = SimpleDateFormat("ddMMyyHHmmss")
        val dateobj = Date()
        val imagePath = email+"."+df.format(dateobj)+".jpg"
        val imageRef = storageRef.child("imagePost/"+imagePath)

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos)
        val data = baos.toByteArray()
        val uploadTask = imageRef.putBytes(data)
        uploadTask.addOnFailureListener {
            Toast.makeText(applicationContext,"Failed to upload", Toast.LENGTH_LONG).show()
        }.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener(OnSuccessListener<Any?> {
                 DownloadURL = it.toString()
                ListTweets.removeAt(0)
                adapter!!.notifyDataSetChanged()


            })


        }

    }
    fun LoadPost(){
        myRef.child("posts").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(datasnapshot: DataSnapshot) {
                try {
                    ListTweets.clear()
                    ListTweets.add(Ticket("0","hi","url","add","tt"))

                    var td = datasnapshot!!.value
                    for(d in datasnapshot.children){
                        var c  =  d.value as HashMap<String,String>
                        ListTweets.add(Ticket(d.key.toString(),c["text"] as String,c["postImage"] as String,c["userUID"] as String,c["date"] as String))
                    }
                    adapter!!.notifyDataSetChanged()
                }
                catch (ex:Exception){}
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

}