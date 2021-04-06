package com.example.track_a_mole

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class MoleTransit : AppCompatActivity() {


    var images:Array<Int> = arrayOf(R.drawable.one, R.drawable.two)
    private var molesList = mutableListOf<Bitmap>()
    private lateinit var moleID:String

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore
    private val storage: FirebaseStorage = Firebase.storage

    private val ONE_MEGABYTE: Long = 1024 * 1024
    private var flag:Int = 0
    private val imgList = mutableListOf<Bitmap>()
    private lateinit var uid: String
    private lateinit var viewpager:ViewPager
    private lateinit var adapter:PagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mole_transit)

        uid = auth.currentUser?.uid.toString()
        moleID = intent.getStringExtra("example").toString()

        viewpager = findViewById(R.id.viewPager)
        viewpager.setPageTransformer(true, FadeOutPageTransformer())
        //viewpager.autoScroll(2000)



//        val extras: Bundle? = intent.extras
//        if (extras != null) {
//            if (!intent.hasExtra("MOLE_NAME")) {
//                Log.w("ZOOM", "NO UID")
//            }
//            moleID = extras.get("example") as String
//            Log.d("TRANSITON", moleID)
//        }


        val storageRef = storage.reference
        db.collection("mole_photos").whereEqualTo("uid", uid).whereEqualTo("uniqueName", moleID).get()
            .addOnSuccessListener { documents ->
                var ds = documents.sortedWith(compareBy { it.data["timestamp"] as Long })
                //ds = ds.asReversed()
                Log.d("MOLE PICS", "Found  Documents")
                for (document in ds) {
                    val sr = document.data["storageRef"]
                    val pathReference = storageRef.child("$sr")

                    val f = pathReference.getBytes(ONE_MEGABYTE).addOnSuccessListener { bytes ->
                        if (bytes == null) {
                            Log.d("MOLE PICS", "No picture in DB")
                        }
                        imgList.add(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                        Log.d("MOLE PICS", "Successfully loaded image $sr from DB")
                        updateView()
                    }.addOnFailureListener {
                        Log.w("MOLE PICS", "Unable to get image $sr from DB")
                    }
                    // Solve the race condition by waiting for each one
                    // This is not ideal but should be ok for this type of scale
                    while (!f.isComplete) {
                        //loading.visibility = View.VISIBLE
                        Thread.sleep(50)
                        //loading.visibility = View.GONE
                    }
                }
//                if(imgList.isNotEmpty()){
//                    var adapter: PagerAdapter = SliderAdapter(applicationContext, images, imgList)
//                    var viewpager: ViewPager = findViewById(R.id.viewPager)
//                    Log.d("TRANSITON", moleID)
//                    viewpager.adapter = adapter
//                    viewpager.autoScroll(2000)
//                }


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

    private fun updateView() {
        if(flag == 0){
            adapter = SliderAdapter(applicationContext, images, imgList)
            viewpager.adapter = adapter
            viewpager.autoScroll(3000)
            flag = 1
        }
        else{
            adapter.notifyDataSetChanged()

        }
        Log.d("TRANSITON", "updating view")
    }


}