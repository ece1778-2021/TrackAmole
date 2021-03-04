package com.example.track_a_mole

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class GlobalFeed : AppCompatActivity() {
    private lateinit var loading: ProgressBar
    private val storage: FirebaseStorage = Firebase.storage
    
    private val NUM_COLUMNS: Int = 1
    private val ONE_MEGABYTE: Long = 1024 * 1024
    private val MAX_NUM_PHOTOS: Long = 16

    private val imgList = mutableListOf<Bitmap>()
    private val strList = mutableListOf<String>()

    private lateinit var adapter: CustomAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global_feed)

        Log.d("PROFILE", "Profile Start")

        loading = findViewById<ProgressBar>(R.id.loading)
        val back_to_main: Button = findViewById(R.id.mainb)

        val db = Firebase.firestore

        // Set up RecyclerVew
        val rView: RecyclerView = findViewById(R.id.recycleg)
        rView.layoutManager = GridLayoutManager(this, NUM_COLUMNS)

        adapter = CustomAdapter(imgList, strList)
        rView.adapter = adapter

        Toast.makeText(this, "Loading Images. Please Wait.", Toast.LENGTH_SHORT).show()

        val storageRef = storage.reference
        db.collection("mole_photos").limit(MAX_NUM_PHOTOS).get()
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
                        adapter.notifyDataSetChanged()
                        Log.d("PICS", "Successfully loaded image $sr from DB")
                    }.addOnFailureListener {
                        Log.w("PICS", "Unable to get image $sr from DB")
                    }
                    // Solve the race condition by waiting for each one
                    // This is not ideal but should be ok for this type of scale
                    while (!f.isComplete) {
                        loading.visibility = View.VISIBLE
                        Thread.sleep(50)
                        loading.visibility = View.GONE
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

//        loading.visibility = View.GONE

        back_to_main.setOnClickListener { onMainListener() }
    }

    private fun onMainListener() {
        // ok since user should only ever be coming from MainActivity
        onBackPressed()
    }
}