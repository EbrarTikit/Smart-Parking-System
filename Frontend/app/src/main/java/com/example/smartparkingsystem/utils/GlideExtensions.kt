package com.example.smartparkingsystem.utils

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.smartparkingsystem.R

fun ImageView.loadImage(
    imageUrl: String?,
    placeholder: Int = R.drawable.placeholder,
    errorImage: Int = R.drawable.error_image
) {
    Glide.with(this.context)
        .load(imageUrl)
        .apply(
            RequestOptions()
                .placeholder(placeholder)
                .error(errorImage)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
        )
        .into(this)
}