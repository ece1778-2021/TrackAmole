package com.example.track_a_mole

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import java.text.SimpleDateFormat
import java.util.*

class Tracker : AppCompatActivity() {
    private lateinit var moleID:String
    private lateinit var tempDisplay:TextView

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore
    private val storage: FirebaseStorage = Firebase.storage

    private lateinit var uid: String

    private val REQUEST_IMAGE_CAPTURE: Int = 1
    private val REQUEST_GALLERY_IMAGE: Int = 2
    private val NUM_COLUMNS: Int = 3
    private val ONE_MEGABYTE: Long = 1024 * 1024

    private val imgList = mutableListOf<Bitmap>()
    private val strList = mutableListOf<String>()
    private val dateList = mutableListOf<String>()
    private val locationList = mutableListOf<String>()

    private val simpleDateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.ENGLISH)


    private lateinit var adapter: CustomAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracker)

        uid = auth.currentUser?.uid.toString()

        val playBtn: ImageButton = findViewById(R.id.imageButton_play)
        val homeBtn: ImageButton = findViewById(R.id.homeButton)

        val rView: RecyclerView = findViewById(R.id.tracker_view)
        rView.layoutManager = LinearLayoutManager(this)

        adapter = CustomAdapter(imgList, strList, dateList, locationList)
        rView.adapter = adapter

        var time:Long


        val extras: Bundle? = intent.extras
        if (extras != null) {
            if (!intent.hasExtra("MOLE_NAME")) {
                Log.w("ZOOM", "NO UID")
            }
            moleID = extras.get("MOLE_NAME") as String
        }

        val storageRef = storage.reference
        db.collection("mole_photos").whereEqualTo("uid", uid).whereEqualTo("uniqueName", moleID).get()
            .addOnSuccessListener { documents ->
                var ds = documents.sortedWith(compareBy { it.data["timestamp"] as Long })
                ds = ds.asReversed()
                for (document in ds) {
                    val sr = document.data["storageRef"]
                    val pathReference = storageRef.child("$sr")

                    val f = pathReference.getBytes(ONE_MEGABYTE).addOnSuccessListener { bytes ->
                        if (bytes == null) {
                            Log.d("PICS", "No picture in DB")
                        }
                        imgList.add(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                        strList.add(document.id)
                        time = document.data["timestamp"] as Long
                        dateList.add(getDateString(time).toString())
                        locationList.add(document.data["location"] as String)
                        adapter.notifyDataSetChanged()
                        Log.d("PICS", "Successfully loaded image $sr from DB")
                    }.addOnFailureListener {
                        Log.w("PICS", "Unable to get image $sr from DB")
                    }
                    // Solve the race condition by waiting for each one
                    // This is not ideal but should be ok for this type of scale
                    while (!f.isComplete) {
                        //loading.visibility = View.VISIBLE
                        Thread.sleep(50)
                        //loading.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener {
                Log.d("PICS", "Unable to find user images.")
                Toast.makeText(
                    this,
                    "Unable to Load Images! Please log out and back in again.",
                    Toast.LENGTH_SHORT
                ).show()
            }

        //playBtn.setOnClickListener()
        playBtn.setOnClickListener { viewMole() }
        homeBtn.setOnClickListener{ returnHome() }
    }

    private fun returnHome(){
        val homeView = Intent(this, MainActivity::class.java)
        startActivity(homeView)
    }

    private fun viewMole() {
        val moleView = Intent(this, MoleTransit::class.java)
        moleView.putExtra("example", moleID)
        startActivity(moleView)
    }

    private fun getDateString(time: Long) : String = simpleDateFormat.format(time * 1000L)
}



