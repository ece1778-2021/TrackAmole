package com.example.track_a_mole

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.ByteArrayOutputStream
import java.util.*

class Photo : AppCompatActivity() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore
    private val storage: FirebaseStorage = Firebase.storage

    private lateinit var uid: String

    private lateinit var asyText: EditText
    private lateinit var bordText: EditText
    private lateinit var colText: EditText
    private lateinit var evolText: EditText
    private lateinit var diamText: EditText

    private lateinit var img: ImageView
    private lateinit var responseGen: SwitchCompat
    private lateinit var captionText: TextView
    private lateinit var labeltext: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)


        val back = findViewById<Button>(R.id.back)
        val confirm = findViewById<Button>(R.id.confirm)

        responseGen = findViewById<SwitchCompat>(R.id.autogenresponses)

        img = findViewById(R.id.bigImageView)

        //cpText = findViewById(R.id.caption)
        asyText = findViewById(R.id.asym_response)
        bordText = findViewById(R.id.border_response)
        colText = findViewById(R.id.colour_response)
        diamText = findViewById(R.id.diameter_response)
        evolText = findViewById(R.id.evolve_response)



        //captionText = findViewById(R.id.captionText)

        uid = auth.currentUser?.uid.toString()

        val extras: Bundle? = intent.extras
        if (extras != null) {
            img.setImageBitmap(extras.get("NEW_PHOTO") as Bitmap)
        } else {
            Log.w("PHOTO", "Error: Photo Intent started without new picture")
        }

        back.setOnClickListener { onBackPressed() }
        confirm.setOnClickListener { onConfirm() }
        responseGen.setOnCheckedChangeListener { bt: CompoundButton, checked: Boolean ->
            onSwitch(
                checked
            )
        }

        labeltext = getString(R.string.caption_loading_text)

        val opts = ImageLabelerOptions.Builder().setConfidenceThreshold(0.7f).build()
        val labeler = ImageLabeling.getClient(opts)
        val image = InputImage.fromBitmap((img.drawable as BitmapDrawable).bitmap, 0)
        var tmpText = ""

        labeler.process(image)
            .addOnSuccessListener { labels ->
                for (label in labels) {
                    val t = label.text
                    tmpText = tmpText.plus(" #$t")
                }
                labeltext = tmpText
                if (responseGen.isChecked) {
                    captionText.text = labeltext
                }
            }
            .addOnFailureListener { e ->
                Log.w("CAPT", e.toString())
            }
    }

    private fun onConfirm() {
        Toast.makeText(this, "Posting Image.", Toast.LENGTH_SHORT).show()
        val storageRef = storage.reference
        val now: Long = Calendar.getInstance().time.toInstant().epochSecond
        val nowStr: String = now.toString()
        val pathString = "$uid/$nowStr.jpg"
        val imgRef: StorageReference = storageRef.child(pathString)
        val bitmap = (img.drawable as BitmapDrawable).bitmap

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val dt = baos.toByteArray()
        val uploadTask = imgRef.putBytes(dt)
        val dataUID = "$uid$nowStr"

        var asym = asyText.text.toString()
        var border = bordText.text.toString()
        var colour = colText.text.toString()
        var diameter = diamText.text.toString()
        var evolve = evolText.text.toString()
//        if (captionGen.isChecked) {
//            capt = capt.plus(captionText.text.toString())
//        }


        uploadTask.addOnSuccessListener { taskSnapshot ->
            val new_img = hashMapOf(
                "storageRef" to pathString,
                "timestamp" to now,
                "uid" to uid,
                "asymetry" to asym,
                "border" to border,
                "colour" to colour,
                "diameter" to diameter,
                "evolve" to evolve
            )

            db.collection("mole_photos").document(dataUID)
                .set(new_img)
                .addOnSuccessListener { Log.d("PHOTOS", "DB ADD SUCCESS") }
                .addOnFailureListener { Log.d("PHOTOS", "DB ADD FAILURE") }
        }
            .addOnFailureListener {
                Log.w("PHOTO", "Upload picture failed")
            }

        Thread.sleep(1000) // give the image time to actually post to the database
        Toast.makeText(this, "Image Posted.", Toast.LENGTH_SHORT).show()
        val mainIntent = Intent(this, MainActivity::class.java)
        startActivity(mainIntent)
    }

    private fun onSwitch(checked: Boolean) {
        if (checked) {
            captionText.text = labeltext
        } else {
            captionText.text = getString(R.string.caption_text)
        }
    }
}
