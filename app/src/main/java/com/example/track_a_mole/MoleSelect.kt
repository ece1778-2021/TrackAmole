package com.example.track_a_mole

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MoleSelect : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore

    private lateinit var uid:String
    private lateinit var moleID:String

    private var moleFlag:Int = 0
    private val REQUEST_IMAGE_CAPTURE: Int = 1

    private lateinit var nameSelect: Spinner

    private lateinit var textExistMole: TextView
    private lateinit var textNewMole: TextView

    private lateinit var newMoleName: EditText

    private lateinit var submitButton: Button

    private val names = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mole_select)

        uid = auth.currentUser?.uid.toString()

        nameSelect = findViewById(R.id.spinner_mole)
        textNewMole = findViewById(R.id.disp_mole_text)
        textExistMole = findViewById(R.id.disp_mole_text2)
        newMoleName = findViewById(R.id.text_mole_name)

        submitButton = findViewById(R.id.button_continue_picture)

        setSpinner()
        submitButton.setOnClickListener {
            saveName()
            onNewPhoto() }
        //nameSelect.onItemSelectedListener = this



    }

    private fun saveName() {
        if(moleFlag == 0){
            Log.d("mole.select'activity", "new mole name")
            moleID = newMoleName.text.toString()
        }
        if(moleFlag == 1){
            Log.d("mole.select'activity", "old mole name")
            moleID = nameSelect.selectedItem.toString()
        }
    }

    private fun onNewPhoto() {
        //selectImage()
        Toast.makeText(this, "Hold camera 5cm directly above mole.", Toast.LENGTH_LONG).show()
        dispatchTakePictureIntent()
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            // display error state to the user
            Log.d("PIC", "Picture activity error")
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            val photoIntent = Intent(this, Photo::class.java)
            photoIntent.putExtra(
                "NEW_PHOTO",
                imageBitmap
            )
            photoIntent.putExtra(
                "MOLE_NAME",
                moleID
            )
            startActivity(photoIntent)
        }
    }

    private fun setSpinner() {
        db.collection("mole_photos").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { documents ->
              for (document in documents) {
                  val nameString = document.get("uniqueName") as String
                  if (nameString !in names) {
                      names.add(document.get("uniqueName") as String)
                  }
              }
            }

//        names.add("select 1")
        names.add("None")


//        // Create an ArrayAdapter using a simple spinner layout and languages array
        val aa = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
//        // Set layout to use when the list of choices appear
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        // Set Adapter to Spinner
        nameSelect.adapter = aa

//        ArrayAdapter.createFromResource(this, names, android.R.layout.simple_spinner_item).also{
//            adapter -> adapter.setDropDownViewResource(android.R.layout.simple_spinner_item)
//            nameSelect.adapter = adapter
        }


    fun onRadioButtonClicked(view: View) {
        if(view is RadioButton){
            val checked = view.isChecked
            when(view.getId()){
                R.id.radio_new_mole->
                    if(checked){
                        textExistMole.visibility = View.GONE
                        nameSelect.visibility = View.GONE
                        textNewMole.visibility = View.VISIBLE
                        newMoleName.visibility = View.VISIBLE
                        moleFlag = 0

                    }
                R.id.radio_existing_mole->
                    if(checked){
                        textExistMole.visibility = View.VISIBLE
                        nameSelect.visibility = View.VISIBLE
                        textNewMole.visibility = View.GONE
                        newMoleName.visibility = View.GONE
                        moleFlag = 1
                    }
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        TODO("Not yet implemented")
    }
}