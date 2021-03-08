package com.example.track_a_mole

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class History : AppCompatActivity() {

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

    private lateinit var adapter: CustomAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        uid = auth.currentUser?.uid.toString()

        val rView: RecyclerView = findViewById(R.id.recycle_hist)
        rView.layoutManager = LinearLayoutManager(this)

        adapter = CustomAdapter(imgList, strList)
        rView.adapter = adapter

        // Place first in case not loading any images

        //loading.visibility = View.GONE
        val storageRef = storage.reference
        db.collection("mole_photos").whereEqualTo("uid", uid).get()
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
    }
}