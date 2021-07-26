package com.example.twitterdemo

import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_login.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class Login : AppCompatActivity() {
    private var mAuth:FirebaseAuth?=null
    private var database = FirebaseDatabase.getInstance()
    private var  myRef = database.getReference()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        mAuth = FirebaseAuth.getInstance()
        ivPerson.setOnClickListener {

            checkPermission()
        }
        buLogin.setOnClickListener {
            var email = etEmail.text.toString()
            var pass = etPass.text.toString()
            Login(email,pass)
        }

        //Dummy Data


    }
    fun Login(email:String,pass:String){
        mAuth!!.createUserWithEmailAndPassword(email,pass)
            .addOnCompleteListener(this){task->
                if(task.isSuccessful){
                    Toast.makeText(applicationContext,"Successful Login",Toast.LENGTH_LONG).show()
                    SaveImageInFirebase()
                }
                else {
                    Toast.makeText(applicationContext,"Failed Login",Toast.LENGTH_LONG).show()

                }
            }
    }

    fun SaveImageInFirebase(){
var cu = mAuth!!.currentUser
        val Storage = FirebaseStorage.getInstance()
        var storageRef = Storage.getReferenceFromUrl("gs://fir-multi-71e05.appspot.com")
        val df = SimpleDateFormat("ddMMyyHHmmss")
        val dateobj = Date()
        val imagePath = cu!!.email+"."+df.format(dateobj)+".jpg"
       val imageRef = storageRef.child("images/"+imagePath)
        ivPerson.isDrawingCacheEnabled = true
        ivPerson.buildDrawingCache()
        val drawable = ivPerson.drawable as BitmapDrawable
        val bitmap = drawable.bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos)
        val data = baos.toByteArray()
        val uploadTask = imageRef.putBytes(data)
        uploadTask.addOnFailureListener {
            Toast.makeText(applicationContext,"Failed to upload",Toast.LENGTH_LONG).show()
        }.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener(OnSuccessListener<Any?> {
                myRef.child("Users").child(cu.uid).child("email").setValue(cu.email)
                myRef.child("Users").child(cu.uid).child("ProfileImage").setValue(it.toString())
                LoadTweets()
            })


        }

        }



    override fun onStart() {
        super.onStart()
        LoadTweets()
    }

    fun LoadTweets(){
        var currentuser: FirebaseUser? = mAuth!!.currentUser
        if(currentuser!=null)
        {

            var intent  = Intent(this,MainActivity::class.java)
            intent.putExtra("email",currentuser.email)
            intent.putExtra("uid",currentuser.uid)
            startActivity(intent)
        }

    }


    fun clicked(view:View){

    }
    val ReadImage:Int = 253;

    fun checkPermission(){
        if(Build.VERSION.SDK_INT>=23){
            if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),ReadImage)
            }
            else LoadImage()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode){
            ReadImage->{
                if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    LoadImage()
                }
                else {
                    Toast.makeText(this,"Permission not granted",Toast.LENGTH_LONG).show()
                }
            }
            else ->{
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)

            }
        }
    }
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
            ivPerson.setImageBitmap(bitmap)
        }
    }
}