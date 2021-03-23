package com.example.track_a_mole

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.track_a_mole.BuildConfig.DEBUG
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class HistoryMoles : AppCompatActivity() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore
    private val storage:FirebaseStorage = Firebase.storage

    private lateinit var uid:String

    private val ONE_MEGABYTE: Long = 1024 * 1024

    private val names = mutableListOf<String>()
    private val uidList = mutableListOf<String>()

    private lateinit var adapter:MoleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_moles)

        uid = auth.currentUser?.uid.toString()

        val rView:RecyclerView = findViewById(R.id.moleView)
        rView.layoutManager = LinearLayoutManager(this)

        adapter = MoleAdapter(names)
        rView.adapter = adapter

        updateMoleNames()

        //adapter.notifyDataSetChanged()
    }

    private fun updateMoleNames() {
        val storageRef = storage.reference
        db.collection("mole_photos").whereEqualTo("uid", uid).get().addOnSuccessListener {
                documents ->
            for(document in documents){
                val nameString = document.get("uniqueName") as String
                if (nameString !in names) {
                    names.add(nameString as String)
                    Log.d("HISTORY", nameString)
                }
            }
            adapter.notifyDataSetChanged()

            // Solve the race condition by waiting for each one
            // This is not ideal but should be ok for this type of scale
            //loading.visibility = View.GONE
        }

    }
}