package com.example.track_a_mole

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream

class Registration : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var pw: EditText
    private lateinit var pw2: EditText
    private lateinit var username: EditText
    private lateinit var bio: EditText
    private lateinit var image: CircleImageView

    private lateinit var loading: ProgressBar

    private lateinit var imageButton: Button
    private lateinit var registerButton: Button

    private val REQUEST_IMAGE_CAPTURE: Int = 1

    private var IMAGE_CAPTURED: Boolean = false

    private val auth = Firebase.auth

    private val storage = Firebase.storage


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        // Initialize references to XML objects
        email = findViewById<EditText>(R.id.email)
        pw = findViewById<EditText>(R.id.password)
        pw2 = findViewById<EditText>(R.id.password_confirmation)
        username = findViewById<EditText>(R.id.username)
        bio = findViewById<EditText>(R.id.bio)
        image = findViewById(R.id.profile_image)
        loading = findViewById<ProgressBar>(R.id.loading)
        imageButton = findViewById(R.id.add_image)
        registerButton = findViewById<Button>(R.id.register)

        // Set Actions
        pw2.afterTextChanged { checkPasswordMatch() }

        registerButton.setOnClickListener { onRegister() }
        imageButton.setOnClickListener { addImage() }

        // Generate default image
        image.setImageBitmap(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
    }

    private fun checkPasswordMatch() {
        if (pw.text.toString() != pw2.text.toString()) {
            pw2.error = "Passwords do not match!"
            registerButton.isEnabled = false
        }
        else {
            registerButton.isEnabled = true
        }
    }

    private fun onRegister() {
        // Set loading
        loading.visibility = View.VISIBLE

        val providedEmail = email.text.toString()
        val providedPassword = pw.text.toString()
        val providedUsername = username.text.toString()
        val providedBio = bio.text.toString()

        // Catch user input which will cause the app to crash and return failure
        if ((providedEmail == "") || (providedPassword == "")) {
            Log.d("REGISTER", "User did not provide input E/P")
            loading.visibility = View.GONE
            val t = Toast.makeText(
                this,
                "Error: Blank email/password fields.",
                Toast.LENGTH_LONG
            )
            t.setGravity(Gravity.CENTER, 0, 0)
            t.show()
            return
        }

        // Catch other NULL inputs (password handled by checkPasswordMatch)
        if ((providedUsername == "") || (providedBio == "")) {
            Log.d("REGISTER", "User did not provide input U/B")
            loading.visibility = View.GONE
            val t = Toast.makeText(
                this,
                "Error: Blank username and/or bio fields. Please fill out these fields",
                Toast.LENGTH_LONG
            )
            t.setGravity(Gravity.CENTER, 0, 0)
            t.show()
            return
        }


        // Attempt to create the user with provided email/password
        auth.createUserWithEmailAndPassword(providedEmail, providedPassword)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("REGISTER", "SUCCESS")
                    val userUID: String = auth.currentUser?.uid ?: "BADLOGIN" // should never happen
                    val db = Firebase.firestore

                    val new_user = hashMapOf(
                        "email" to providedEmail,
                        "username" to providedUsername,
                        "bio" to providedBio
                    )

                    db.collection("users").document(userUID)
                        .set(new_user)
                        .addOnSuccessListener { Log.d("REGISTER", "DB ADD SUCCESS") }
                        .addOnFailureListener { Log.d("REGISTER", "DB ADD FAILURE") }

                    if (IMAGE_CAPTURED) {
                        val storageRef = storage.reference
                        val prof_pic_name = getString(R.string.pp_storage_name)
                        val imgRef: StorageReference = storageRef.child("$userUID/$prof_pic_name")

                        // From https://firebase.google.com/docs/storage/android/upload-files
                        val bitmap = (image.drawable as BitmapDrawable).bitmap
                        val baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                        val data = baos.toByteArray()
                        val uploadTask = imgRef.putBytes(data)
                        uploadTask.addOnFailureListener {
                            Log.w(
                                "REGISTER",
                                "Upload profile picture failed with " + task.exception
                            )
                        }

                    }

                    loading.visibility = View.GONE

                    val loggedInIntent = Intent(this, MainActivity::class.java)
                    loggedInIntent.putExtra(
                        "PROFILE_PIC",
                        (image.drawable as BitmapDrawable).bitmap
                    )
                    startActivity(loggedInIntent)
                } else {
                    Log.d("REGISTER", "Create user failed with " + task.exception)
                    loading.visibility = View.GONE
                    val t = Toast.makeText(
                        this,
                        "Authentication Failed! Please try again.",
                        Toast.LENGTH_LONG
                    )
                    t.setGravity(Gravity.CENTER, 0, 0)
                    t.show()
                }
            }
    }

    private fun addImage() {
        dispatchTakePictureIntent()
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            // display error state to the user
            Log.d("REG", "Profile pic activity error")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            image.setImageBitmap(imageBitmap)
            IMAGE_CAPTURED = true

        }
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}