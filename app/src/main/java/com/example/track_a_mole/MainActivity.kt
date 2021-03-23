package com.example.track_a_mole

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity : AppCompatActivity() {

    private lateinit var username: TextView
    private lateinit var physicianID: TextView
    private lateinit var img: CircleImageView

    private lateinit var loading: ProgressBar

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore
    private val storage: FirebaseStorage = Firebase.storage

    private lateinit var uid: String

    private val REQUEST_IMAGE_CAPTURE: Int = 1
    private val REQUEST_GALLERY_IMAGE: Int = 2
    private val ONE_MEGABYTE: Long = 1024 * 1024

    private val imgList = mutableListOf<Bitmap>()
    private val strList = mutableListOf<String>()

    private lateinit var adapter: CustomAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("PROFILE", "Profile Start")

        physicianID = findViewById(R.id.physicianID)

        username = findViewById(R.id.profile_name)
        img = findViewById(R.id.profile_image)
        loading = findViewById<ProgressBar>(R.id.loading)
        val logout: Button = findViewById(R.id.logout)
        val photo: Button = findViewById(R.id.button_picture)
        val history: Button = findViewById(R.id.button_history)
        val gallery_photo: Button = findViewById(R.id.button_gallery)
        val doctor_btn: Button = findViewById(R.id.button_sendData)

        if (auth.currentUser?.uid == null) {
            Log.w("PROFILE", "Got to Profile but no UID")
            val exitIntent = Intent(this, StartScreen::class.java)
            startActivity(exitIntent)
        }

        uid = auth.currentUser?.uid.toString()

        val extras: Bundle? = intent.extras
        if (extras != null && intent.hasExtra("PROFILE_PIC")) {
            img.setImageBitmap(extras.get("PROFILE_PIC") as Bitmap)
        } else {
            val storageRef = storage.reference
            val ustr: String = uid
            val prof_pic_name = getString(R.string.pp_storage_name)
            val pathReference = storageRef.child("$ustr/$prof_pic_name")


            pathReference.getBytes(ONE_MEGABYTE).addOnSuccessListener { bytes ->
                if (bytes == null) {
                    Log.d("PROFILE", "No picture in DB")
                }
                img.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                Log.d("PROFILE", "Successfully loaded user image from DB")
            }.addOnFailureListener {
                Log.w("PROFILE", "Unable to get picture from DB")
                val t: Toast =
                    Toast.makeText(this, "No profile picture found.", Toast.LENGTH_SHORT)
                t.show()
            }
        }

        val user_data = db.collection("mole_users").document(uid)
        user_data.get()
            .addOnSuccessListener { document ->
                if (document == null) {
                    Log.w("PROFILE", "No UID Matches")
                }
                val user_info = document.data
                username.text = user_info?.get("username") as String
                username.visibility = View.VISIBLE

                var check: Boolean = user_info["physician"] != null
                if (check) {
                    check = user_info["physician"] as Boolean
                }

                if (check) {
                    val pStr = getString(R.string.physician_id_txt)
                    val pidStr = "$pStr $uid"
                    physicianID.text = pidStr
                    physicianID.visibility = View.VISIBLE

                    doctor_btn.text = getString(R.string.physician_btn_txt)
                    doctor_btn.setOnClickListener{ onViewData() }
                }
                else {
                    doctor_btn.setOnClickListener{ onSendData() }
                }


                Log.d("PROFILE", "Success - User successfully loaded")

            }
            .addOnFailureListener {
                val exitIntent = Intent(this, StartScreen::class.java)
                startActivity(exitIntent)
            }

        loading.visibility = View.GONE

        photo.setOnClickListener {
            val moleIntent = Intent(this, MoleSelect::class.java)
            startActivity(moleIntent)
        }
        gallery_photo.setOnClickListener { selectImage() }
        history.setOnClickListener { loadHistory() }
        logout.setOnClickListener { onLogout() }
    }

    private fun loadHistory() {
        val getHistory = Intent(this, HistoryMoles::class.java)
        startActivity(getHistory)

    }

    private fun onNewPhoto() {
        //selectImage()
        Toast.makeText(this, "Hold camera 5cm directly above mole.", Toast.LENGTH_LONG).show()
        dispatchTakePictureIntent()
    }

    private fun selectImage() {
        val pickPhoto = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        //val pickPhoto = Intent(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickPhoto, REQUEST_GALLERY_IMAGE)
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            // display error state to the user
            Log.d("PIC", "Picture activity error")
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            val photoIntent = Intent(this, Photo::class.java)
            photoIntent.putExtra(
                "NEW_PHOTO",
                imageBitmap
            )
            startActivity(photoIntent)
        }
        if (requestCode == REQUEST_GALLERY_IMAGE && resultCode == RESULT_OK) {
            val imageUri = data?.data
            val source = imageUri?.let { ImageDecoder.createSource(this.contentResolver, it) }
            val imageBitmap = source?.let { ImageDecoder.decodeBitmap(it) }
            val photoIntent = Intent(this, Photo::class.java)
            photoIntent.putExtra(
                "NEW_PHOTO",
                imageBitmap
            )
            startActivity(photoIntent)
        }
    }

    private fun onLogout() {
        auth.signOut()
        val exitIntent = Intent(this, StartScreen::class.java)
        startActivity(exitIntent)
    }

    private fun onGlobal() {
        val globalIntent = Intent(this, GlobalFeed::class.java)
        startActivity(globalIntent)
    }

    private fun onSendData() {
        val sendIntent = Intent(this, SendData::class.java)
        startActivity(sendIntent)
    }

    private fun onViewData() {
        val patientData = Intent(this, PhysicianView::class.java)
        startActivity(patientData)
    }
}