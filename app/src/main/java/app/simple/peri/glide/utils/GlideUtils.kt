package app.simple.peri.glide.utils

import android.graphics.Bitmap
import android.widget.ImageView
import app.simple.peri.glide.modules.GlideApp
import app.simple.peri.models.Wallpaper
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.load.resource.bitmap.Downsampler
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

object GlideUtils {

    fun ImageView.loadWallpaper(wallpaper: Wallpaper) {
        GlideApp.with(context)
            .asBitmap()
            .load(app.simple.peri.glide.wallpaper.Wallpaper(wallpaper, context))
            .transition(BitmapTransitionOptions.withCrossFade())
            //.transition(GenericTransitionOptions.with(R.anim.zoom_out))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .set(Downsampler.ALLOW_HARDWARE_CONFIG, true)
            .into(this)
    }

    fun ImageView.loadWallpaper(wallpaper: Wallpaper, onLoad: (Bitmap) -> Unit) {
        GlideApp.with(context)
            .asBitmap()
            .load(app.simple.peri.glide.wallpaper.Wallpaper(wallpaper, context))
            .transition(BitmapTransitionOptions.withCrossFade())
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .set(Downsampler.ALLOW_HARDWARE_CONFIG, false)
            .addListener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                    /* no-op */
                    return false
                }

                override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    onLoad(resource!!)
                    return false
                }
            })
            .into(this)
    }
}