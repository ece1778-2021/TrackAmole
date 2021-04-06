package com.example.track_a_mole

import android.view.View
import androidx.viewpager.widget.ViewPager
import kotlin.math.abs

class FadeOutPageTransformer: ViewPager.PageTransformer{
    override fun transformPage(page: View, position: Float) {
        page.apply{
            val pageWidth = width
            val pageHeight = height
            translationX =-position*pageWidth
            alpha = 1- abs(position)

        }
    }

}