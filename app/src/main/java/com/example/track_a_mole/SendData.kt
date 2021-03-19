package com.example.track_a_mole

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SendData : AppCompatActivity() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore

    private lateinit var uid: String
    private lateinit var physUID: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_data)

        val back = findViewById<Button>(R.id.back)
        back.setOnClickListener { onBackPressed() }

        val add_btn = findViewById<Button>(R.id.add_doctor)
        add_btn.setOnClickListener { onAdd() }

        physUID = findViewById(R.id.physicianIDInput)
        uid = auth.currentUser?.uid.toString()

    }

    fun onAdd() {

        val loading = findViewById<ProgressBar>(R.id.loading)
        val providedUID = physUID.text.toString()

        if (providedUID == "") {
            Toast.makeText(this, "Add a Physician ID!", Toast.LENGTH_SHORT).show()
            return
        }

        // Remove focus from field if focused
        physUID.isEnabled = false
        physUID.isEnabled = true

        loading.visibility = View.VISIBLE
        // First check if field is valid
        var physicianAuthorized = mutableListOf<String>()
        val providedIDUser = db.collection("mole_users").document(providedUID)
        providedIDUser.get()
            .addOnSuccessListener { document ->
                if (document == null) {
                    Log.w("ADDP", "No UID Matches")
                    Toast.makeText(this, "Invalid ID!", Toast.LENGTH_LONG).show()
                    loading.visibility = View.GONE
                } else {
                    val providedIDInfo = document.data
                    var isPhysician: Boolean = providedIDInfo?.get("physician") != null

                    if (isPhysician) {
                        isPhysician = providedIDInfo?.get("physician") as Boolean
                    }

                    if (isPhysician) {
                        if (providedIDInfo?.get("authorized") != null) {
                            physicianAuthorized =
                                providedIDInfo["authorized"] as MutableList<String>
                        }
                        physicianAuthorized.add(uid)
                        // Add user to doctor
                        providedIDUser.update("authorized", physicianAuthorized as List<String>)
                            .addOnSuccessListener {
                                Log.d("ADDP", "Success - DR successfully updated")
                                // Now check current user and add doctor as authorized
                                var userAuthorized = mutableListOf<String>()
                                val currentUser = db.collection("mole_users").document(uid)
                                currentUser.get().addOnSuccessListener { document ->
                                    if (document == null) {
                                        Log.w("ADDP", "No UID Matches")
                                    }
                                    val user_info = document.data
                                    if (user_info?.get("authorized") != null) {
                                        userAuthorized =
                                            user_info["authorized"] as MutableList<String>
                                    }
                                    userAuthorized.add(providedUID)
                                    currentUser.update("authorized", userAuthorized as List<String>)
                                        .addOnSuccessListener {
                                            Log.d("ADDP", "Success - User successfully updated")

                                        }
                                        .addOnFailureListener {
                                            Log.w("ADDP", "Unable to retrieve User data")
                                        }

                                    Log.d("ADDP", "Success - User successfully loaded")

                                }
                                    .addOnFailureListener {
                                        Log.w("ADDP", "Unable to retrieve data")
                                    }


                            }
                            .addOnFailureListener {
                                Log.w("ADDP", "Unable to retrieve DR data")
                            }

                    }
                    else {
                        Toast.makeText(this, "ID does not belong to Physician!", Toast.LENGTH_LONG).show()
                    }
                }
                Log.d("ADDP", "Success - DR successfully loaded")

            }
            .addOnFailureListener {
                Log.w("ADDP", "Unable to retrieve DR data")
            }

        Toast.makeText(this, "Physician Added!", Toast.LENGTH_SHORT).show()

        loading.visibility = View.GONE
    }

}