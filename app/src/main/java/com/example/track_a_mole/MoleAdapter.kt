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

class MoleAdapter(private val nameSet: MutableList<String>) :
    RecyclerView.Adapter<MoleAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, context: Context) : RecyclerView.ViewHolder(view) {
        var unameDisplay: TextView = view.findViewById(R.id.uNamePatient)
        var textDisplay: TextView = view.findViewById(R.id.displayText)
        val c = context
        var moleName: String = ""


        init {
            // Define click listener for the ViewHolder's View.
            view.setOnClickListener{
                Log.i("RVCLICK", adapterPosition.toString())
                val zoomIntent = Intent(c, Tracker::class.java)
                zoomIntent.putExtra(
                   "MOLE_NAME",
                    moleName
                )
                //zoomIntent.putExtra(
                //    "UID",
                //   uid
                //)
                c.startActivity(zoomIntent)
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
        viewHolder.textDisplay.text = "Mole name:"
        viewHolder.moleName = nameSet[position]
        viewHolder.unameDisplay.text = nameSet[position]
        //viewHolder.locDisplay.setText(imgLocSet[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = nameSet.size

}