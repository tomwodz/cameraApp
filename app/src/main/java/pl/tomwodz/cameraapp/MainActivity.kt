package pl.tomwodz.cameraapp

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.ScaleGestureDetector
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import pl.tomwodz.cameraapp.databinding.ActivityMainBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraSelector: CameraSelector
    private lateinit var binding: ActivityMainBinding
    private lateinit var imgCaptureExecutor: ExecutorService
    private var camera: Camera? = null
    var uri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraPermissionResult.launch(android.Manifest.permission.CAMERA)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        imgCaptureExecutor = Executors.newSingleThreadExecutor()

        val changeCamera = findViewById<Button>(R.id.changeCamera)
        val takePhoto = findViewById<Button>(R.id.takePhoto)
        val gallery = findViewById<Button>(R.id.gallery)

        changeCamera.setOnClickListener(){
            if(cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            } else if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }

        takePhoto.setOnClickListener(){
            takePhoto()
            animateFlash()
        }

        gallery.setOnClickListener(){
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, 1)

//            val fileName = "JPEG_1.jpg"
//            val outputDirectory = getOutputDirectory()
//            val photoFile = File(outputDirectory, fileName)
//            var temp = Uri.parse(photoFile.toString()).toString()


        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
            uri = data?.data.toString()
        println(uri)
        var intent: Intent = Intent(this, ImageActivity::class.java)
        intent.putExtra("uri", uri)
        startActivity(intent)
    }

    private val cameraPermissionResult = registerForActivityResult(RequestPermission()) { permissionGranted ->
            if (permissionGranted) {
                startCamera()
            } else {
                requestPermissions(
                    arrayOf(android.Manifest.permission.CAMERA), 1
                )
            }
    }

    private fun startCamera() {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.preview.surfaceProvider)
        }
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder().build()
            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview,
                    imageCapture)

                val zoomGestureListener = object :
                    ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        val cameraInfo = camera?.cameraInfo
                        val cameraControl = camera?.cameraControl
                        val zoomRatio = cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                        val newZoomRatio = cameraControl?.setZoomRatio(2f)
                        return true
                    }
                }
                val zoomGestureDetector = ScaleGestureDetector(this, zoomGestureListener)
                binding.preview.setOnTouchListener { _, event ->
                    zoomGestureDetector.onTouchEvent(event)
                    return@setOnTouchListener true
                }
            } catch (e: Exception) {
                Log.d(TAG, "Use case binding failed")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        imageCapture?.let { imageCapture ->
            val fileName = "JPEG_${System.currentTimeMillis()}.jpg"
            //val fileName = "JPEG_1.jpg"
            val outputDirectory = getOutputDirectory()
            val photoFile = File(outputDirectory, fileName)
            val outputFileOptions =
                ImageCapture.OutputFileOptions.Builder(photoFile).build()
            imageCapture.takePicture(
                outputFileOptions,
                imgCaptureExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults:
                                              ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        val mediaScanIntent =
                            Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                        mediaScanIntent.data = savedUri
                        sendBroadcast(mediaScanIntent)
                    }
                    override fun onError(exception: ImageCaptureException) {
                        Log.d(TAG, "Error taking photo: $exception")
                    }
                }
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun animateFlash() {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }
    
}
