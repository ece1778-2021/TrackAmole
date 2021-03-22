package com.example.track_a_mole

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PhysicianAdapter(private val userSet: MutableList<String>, private val uidSet:MutableList<String>) :
    RecyclerView.Adapter<PhysicianAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, context: Context) : RecyclerView.ViewHolder(view) {
        var unameDisplay: TextView = view.findViewById(R.id.uNamePatient)
        val c = context
        var uid: String = ""

        init {
            // Define click listener for the ViewHolder's View.
            view.setOnClickListener{
                Log.i("RVCLICK", adapterPosition.toString())
                //val zoomIntent = Intent(c, FocusImage::class.java)
                //zoomIntent.putExtra(
                //    "PICTURE",
                //    (imageView.drawable as BitmapDrawable).bitmap
                //)
                //zoomIntent.putExtra(
                //    "UID",
                //   uid
                //)
                //c.startActivity(zoomIntent)
            }
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.rview_item_phys, viewGroup, false)

        return ViewHolder(view, viewGroup.context)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.uid = uidSet[position]
        viewHolder.unameDisplay.text = userSet[position]
        //viewHolder.locDisplay.setText(imgLocSet[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = userSet.size

}