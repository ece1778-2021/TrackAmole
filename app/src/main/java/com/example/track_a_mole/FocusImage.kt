package com.example.track_a_mole

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage

class FocusImage : AppCompatActivity() {
    private lateinit var imageUID: String
    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore
    private val storage: FirebaseStorage = Firebase.storage

    private lateinit var imageData: DocumentReference
    private lateinit var pathRef: StorageReference

    private lateinit var commentsShown: EditText
    private lateinit var comments: String
    private lateinit var addCommentField: EditText

    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_focusimage)

        val img = findViewById<ImageView>(R.id.bigImageView)
        val asymText = findViewById<TextView>(R.id.asymResponse)
        val bordText = findViewById<TextView>(R.id.borderResponse)
        val diamText = findViewById<TextView>(R.id.diameterResponse)
        val colText = findViewById<TextView>(R.id.colourResponse)
        val evolText = findViewById<TextView>(R.id.evolveResponse)
        val locText = findViewById<TextView>(R.id.locationResponse)

        //val add_comment_btn = findViewById<Button>(R.id.add_comment)
        val del = findViewById<ImageButton>(R.id.delete)

        //addCommentField = findViewById(R.id.add_comment_text)
        //commentsShown = findViewById(R.id.comments)

        val extras: Bundle? = intent.extras
        if (extras != null) {
            if (!intent.hasExtra("UID")) {
                Log.w("ZOOM", "NO UID")
                errorExit()
            }

            imageUID = extras.get("UID") as String
            img.setImageBitmap(extras.get("PICTURE") as Bitmap)

            username = ""
            db.collection("mole_users").document(auth.currentUser?.uid as String).get()
                .addOnSuccessListener { document ->
                    if (document == null) {
                        Log.w("ZOOM", "No UID Matches")
                    }
                    username = document.data?.get("username") as String
                    Log.d("ZOOM", "Success - User successfully loaded")

                }
                .addOnFailureListener {
                    Log.w("ZOOM", "Failed to get user data")
                }


            imageData = db.collection("mole_photos").document(imageUID)
            val f = imageData.get()
                .addOnSuccessListener { document ->
                    if (document == null) {
                        Log.w("ZOOM", "Unable to get Photo by UID")
                        errorExit()
                    }
                    val img_info = document.data
                    val imgUID = img_info?.get("uid") as String
                    var asymetry = img_info["asymetry"] as String
                    var border = img_info["border"] as String
                    var diameter = img_info["diameter"] as String
                    var colour = img_info["colour"] as String
                    var evolve = img_info["evolve"] as String
                    var location = img_info["location"] as String


//                    if (img_info[""] != null) {
//                        imgCaption = img_info["border"] as String
//                    }
//                    comments = ""
//                    if (img_info["comments"] != null) {
//                        comments = img_info["comments"] as String
//                        commentsShown.setText(
//                            comments,
//                            TextView.BufferType.EDITABLE
//                        )
//                    }

                    val storageRef = storage.reference
                    val sr = img_info["storageRef"] as String
                    pathRef = storageRef.child(sr)


                    if (asymetry != "") {
                        asymText.text = asymetry
                        bordText.text = border
                        colText.text = colour
                        diamText.text = diameter
                        evolText.text = evolve
                        locText.text = location
                    }

                    if (imgUID == auth.currentUser?.uid) {
                        del.visibility = View.VISIBLE
                        del.isEnabled = true
                    }


                    Log.d("PROFILE", "Success - User successfully loaded")

                }
                .addOnFailureListener {
                    Log.w("ZOOM", "Unable to get Photo by UID")
                    errorExit()
                }

            // Stall to guarantee fields will be available
            while (!f.isComplete) {
                Thread.sleep(50)
            }

        } else {
            Log.w("ZOOM", "NO PICTURE PROVIDED")
            errorExit()
        }

        //val btn = findViewById<Button>(R.id.back)
        //btn.setOnClickListener { onBackPressed() }

        //add_comment_btn.setOnClickListener { onAddComment() }
        del.setOnClickListener { onDeleteCalled() }
    }

    private fun onAddComment() {
        // Not a great way to do comments, but this was very fast...
        val additionalComment = addCommentField.text.toString()
        if (comments == "") {
            comments = "$username: $additionalComment"
            commentsShown.setText(
                comments,
                TextView.BufferType.EDITABLE
            )
        } else {
            comments = "$comments\n$username: $additionalComment"
            commentsShown.setText(
                comments,
                TextView.BufferType.EDITABLE
            )
        }


        addCommentField.setText("", TextView.BufferType.EDITABLE)
        addCommentField.isEnabled = false
        addCommentField.isEnabled = true

        imageData.update("comments", comments)
            .addOnSuccessListener {
                Toast.makeText(this, "Comment Posted!", Toast.LENGTH_SHORT).show()
                Log.d("ZOOM", "Comment update success") }
            .addOnFailureListener { e -> Log.w("ZOOM", "Error updating document", e) }
    }

    private fun onDeleteCalled() {
        Log.w("ZOOM", "Delete")
        imageData.delete().addOnSuccessListener {
            Log.w("ZOOM", "DB DELETED")
            Toast.makeText(this, "Image Deleted!", Toast.LENGTH_SHORT).show()
            val mainIntent = Intent(this, MainActivity::class.java)
            startActivity(mainIntent)
        }.addOnFailureListener {
            Log.w("ZOOM", "ERROR - DB DELETE")
            Toast.makeText(this, "Error Deleting Image!", Toast.LENGTH_SHORT).show()
        }
        pathRef.delete().addOnSuccessListener {
            Log.w("ZOOM", "STORAGE DELETED")
        }.addOnFailureListener {
            Log.w("ZOOM", "ERROR - STORAGE DELETE")
        }
    }

    private fun errorExit() {
        Log.w("ZOOM", "ERROR")
        Toast.makeText(this, "Unable to Load Image!", Toast.LENGTH_SHORT).show()
        onBackPressed()
    }
}