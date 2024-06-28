package pl.tomwodz.cameraapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.RequestOptions
import java.io.File
import java.security.MessageDigest

class ImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uriImage: String? = intent.getStringExtra("uri")

        setContentView(R.layout.activity_image)

        var imageView = findViewById<ImageView>(R.id.imageView)
        val editImage = findViewById<Button>(R.id.editButton)
        imageView.setImageURI(Uri.parse(uriImage))

        editImage.setOnClickListener(){
            println(uriImage)
            loadAndDisplayImageBlackAndWhite(applicationContext, imageView, imageUri =  "$uriImage")
        }
    }

    fun loadAndDisplayImageBlackAndWhite(context: Context, imageView: ImageView, imageUri: String) {
        val requestOptions = RequestOptions()
            .transform(BlackAndWhiteTransformation())
        Glide.with(context)
            .load(imageUri)
            .apply(requestOptions)
            .into(imageView)
    }

    class BlackAndWhiteTransformation : BitmapTransformation() {
        override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
            val matrix = ColorMatrix().apply {
                setSaturation(0f)
            }
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(matrix)
            }
            val outputBitmap = Bitmap.createBitmap(toTransform.width, toTransform.height, toTransform.config)
            val canvas = android.graphics.Canvas(outputBitmap)
            canvas.drawBitmap(toTransform, 0f, 0f, paint)
            return outputBitmap
        }
        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        }
    }
}