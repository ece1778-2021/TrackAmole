package com.example.track_a_mole

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class StartScreen : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var emailData: EditText
    private lateinit var password: EditText
    private lateinit var loading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_screen)

        auth = Firebase.auth

        if (auth.currentUser != null) {
            val loggedInIntent = Intent(this, MainActivity::class.java)
            startActivity(loggedInIntent)
        }

        val loginButton: Button = findViewById(R.id.login)
        val registerButton: Button = findViewById(R.id.register)

        emailData = findViewById<EditText>(R.id.email)
        password = findViewById<EditText>(R.id.password)
        loading = findViewById<ProgressBar>(R.id.loading)

        loginButton.setOnClickListener { login() }
        registerButton.setOnClickListener { register() }
    }

    private fun login() {
        loading.visibility = View.VISIBLE

        val providedEmail = emailData.text.toString()
        val providedPassword = password.text.toString()

        if ((providedEmail == "") || (providedPassword == "")) {
            Log.d("LOGIN", "User did not provide input")
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

        auth.signInWithEmailAndPassword(providedEmail, providedPassword)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LOGIN", "SUCCESS")
                    loading.visibility = View.GONE
                    val loggedInIntent = Intent(this, MainActivity::class.java)
                    startActivity(loggedInIntent)
                } else {
                    Log.d("LOGIN", "FAILURE")
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

    private fun register() {
        val registrationIntent = Intent(this, Registration::class.java)
        startActivity(registrationIntent)
    }
}