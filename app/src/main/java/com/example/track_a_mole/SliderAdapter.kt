package com.example.track_a_mole

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager.widget.PagerAdapter

class SliderAdapter: PagerAdapter {
    var context:Context
    private var images:Array<Int>
    private var imgList:MutableList<Bitmap>
    lateinit var inflater:LayoutInflater



    constructor(context:Context, images:Array<Int>, imgList:MutableList<Bitmap>):super(){
        this.context = context
        this.images = images
        this.imgList = imgList
    }
    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun getCount(): Int {
        return imgList.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        var image:ImageView
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var view:View = inflater.inflate(R.layout.slider_image_item, container, false)
        image = view.findViewById(R.id.slider_image)
        //image.setImageResource(images[position])
        image.setImageBitmap(imgList[position])
        container!!.addView(view, 0)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container!!.removeView(`object` as View)

    }
}