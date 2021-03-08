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
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.math.sqrt

class Photo : AppCompatActivity() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore
    private val storage: FirebaseStorage = Firebase.storage

    private lateinit var uid: String

    private lateinit var asyText: Spinner
    private lateinit var bordText: Spinner
    private lateinit var colText:  Spinner
    private lateinit var evolText: Spinner
    private lateinit var diamText: Spinner
    private lateinit var locationText: EditText

    private lateinit var img: ImageView
    private lateinit var responseGen: SwitchCompat

    private var area: Double? = null
    private var colour: Int? = null
    private var symmetry:Double? = null

    private val loader = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    System.loadLibrary("opencv_java3")
                    processImage()
                }
                else -> super.onManagerConnected(status)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)


        val back = findViewById<Button>(R.id.back)
        val confirm = findViewById<Button>(R.id.confirm)

        responseGen = findViewById<SwitchCompat>(R.id.autogenresponses)

        img = findViewById(R.id.bigImageView)

        asyText = findViewById(R.id.asym_response)
        bordText = findViewById(R.id.border_response)
        colText = findViewById(R.id.colour_response)
        diamText = findViewById(R.id.diameter_response)
        evolText = findViewById(R.id.evolve_response)
        locationText = findViewById(R.id.location_response)

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

        if (OpenCVLoader.initDebug()) {
            Log.d("OPENCV", "OpenCV successfully loaded")
            loader.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        } else {
            Log.d("OPENCV", "OpenCV load failed")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, loader)
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.dropdown_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            asyText.adapter = adapter
            bordText.adapter = adapter
            colText.adapter = adapter
            diamText.adapter = adapter
            evolText.adapter = adapter
        }
    }

    private fun processImage() {
        // Create data structures
        val src = (img.drawable as BitmapDrawable).bitmap
        val final: Bitmap = Bitmap.createBitmap(src)
        val cvImgBase = Mat()
        val cvImg = Mat()
        val h = Mat()

        // Perform inital processing on image
        Utils.bitmapToMat(src as Bitmap, cvImgBase)
        Imgproc.cvtColor(cvImgBase, cvImg, Imgproc.COLOR_BGR2GRAY)
        Imgproc.medianBlur(cvImg, cvImg, 3)
        Imgproc.Canny(cvImg, cvImg, 10.0, 100.0)

        // Identify contour(s) in image
        val ctrs = mutableListOf<MatOfPoint>()
        Imgproc.findContours(cvImg, ctrs, h, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        // Do math
        val s = ctrs.size
        val areas = MutableList<Double>(s) { index -> 0.0 }
        val colours = MutableList<Int>(s) { index -> 0 }
        val symmetries = MutableList<Double>(s) { index -> 0.0 }

        for (idx in ctrs.indices) {
            // Get Mole Area
            areas[idx] = Imgproc.contourArea(ctrs[idx])

            // Calculate the symmetry with PCA & center for colour
            val m = Imgproc.moments(ctrs[idx])

            val cX = (m.m10 / m.m00).toInt()
            val cY = (m.m01 / m.m00).toInt()
            colours[idx] = src.getPixel(cX, cY)
            symmetries[idx] = checkSymmetry(m.mu20, m.mu11, m.mu02)

            // Draw contour around the mole
            Imgproc.drawContours(cvImgBase, ctrs, idx, Scalar(0.0, 255.0, 0.0))
        }
        val aStr = areas.toString()
        val cStr = colours.toString()
        val sStr = symmetries.toString()

        area = areas.sum() / s
        symmetry = symmetries.sum() / s

        Log.i("OPENCV", "Areas: $aStr")
        Log.i("OPENCV", "Colours: $cStr")
        Log.i("OPENCV", "Symmetry: $sStr")
        Utils.matToBitmap(cvImgBase, final)
        img.setImageBitmap(final)
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

        val asym = asyText.selectedItem.toString()
        val border = bordText.selectedItem.toString()
        val colour = colText.selectedItem.toString()
        val diameter = diamText.selectedItem.toString()
        val evolve = evolText.selectedItem.toString()
        val location = locationText.text.toString()

        uploadTask.addOnSuccessListener { taskSnapshot ->
            val new_img = hashMapOf(
                "storageRef" to pathString,
                "timestamp" to now,
                "uid" to uid,
                "asymetry" to asym,
                "border" to border,
                "colour" to colour,
                "diameter" to diameter,
                "evolve" to evolve,
                "location" to location
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
        if (area == null || symmetry == null) {
            Toast.makeText(this, "Image is being processed.\nPlease reset switch and try again.", Toast.LENGTH_SHORT).show()
            return
        }
        if (checked) {
            asyText.setSelection(symmetryText(symmetry!!))
            asyText.isEnabled = false
            diamText.setSelection(diamText(area!!))
            diamText.isEnabled = false
        }
        else {
            asyText.isEnabled = true
            diamText.isEnabled = true
        }
    }
}

fun checkSymmetry(cm20: Double, cm11: Double, cm02: Double): Double {
    /*
    * Check symmetry of a 2D object given the relevant moments using PCA
    * Derived from first principles.
    * RETURNS: value = 1 => perfect symmetry
    *          value = 0 => perfect asymmetry
     */
    // Calculate eigenvalues of covariance matrix using quadratic formula, where the matrix is:
    // [cm20 cm11
    //  cm11 cm02]
    // Due to matrix properties it is guaranteed that there will be two real +ve eigenvals
    val b: Double = -1.0 * (cm20 + cm02)
    val c: Double = cm20 * cm02 - (cm11 * cm11)
    val sqrt_disc: Double = sqrt((b * b) - (4  * c))

    val ev1 = (-b + sqrt_disc) / 2.0
    val ev2 = (-b - sqrt_disc) / 2.0

    return if (ev2 > ev1) ev1 / ev2 else ev2 / ev1
}

fun symmetryText (symmetry: Double): Int {
    // TODO: Choose more rigorous threshold
    return optionIndex(symmetry > 0.7)
}

fun diamText(area:Double): Int {
    // TODO: Choose better threshold
    return optionIndex(area > 120.0)
}

fun optionIndex(value: Boolean?): Int {
    // Get index of spinner choice corresponding to appropriate value
    // 0 = unsure, 1 = Yes, 2 = No
    if (value == null) {
        return 0
    }
    if (value) {
        return 1
    }
    return 2
}