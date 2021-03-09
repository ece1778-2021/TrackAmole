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

class CustomAdapter(private val dataSet: MutableList<Bitmap>, private val imgPathsSet: MutableList<String>, private val imgDateSet: MutableList<String>, private val imgLocSet: MutableList<String>) :
    RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, context: Context) : RecyclerView.ViewHolder(view) {
        var imageView: ImageView = view.findViewById(R.id.im)
        var dateDisplay: TextView = view.findViewById(R.id.date)
        var locDisplay: TextView = view.findViewById(R.id.location)
        val c = context
        var uid: String = ""

        init {
            // Define click listener for the ViewHolder's View.
            view.setOnClickListener{
                Log.i("RVCLICK", adapterPosition.toString())
                val zoomIntent = Intent(c, FocusImage::class.java)
                zoomIntent.putExtra(
                    "PICTURE",
                    (imageView.drawable as BitmapDrawable).bitmap
                )
                zoomIntent.putExtra(
                    "UID",
                    uid
                )
                c.startActivity(zoomIntent)
            }
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.rview_item, viewGroup, false)

        return ViewHolder(view, viewGroup.context)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.imageView.setImageBitmap(dataSet[position])
        viewHolder.uid = imgPathsSet[position]
        viewHolder.dateDisplay.setText(imgDateSet[position])
        viewHolder.locDisplay.setText(imgLocSet[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}