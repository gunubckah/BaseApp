package com.example.mycamera.data

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.mycamera.callback.TackPhotoCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.security.auth.callback.Callback

class CameraRepository(private val context: Context) {

    private val TAG = "CameraRepository"

    enum class FlashMode { OFF, ON, AUTO }

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var currentFlashMode = FlashMode.OFF
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    suspend fun startCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner
    ) = withContext(Dispatchers.Main) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setTargetRotation(previewView.display.rotation)
            .setFlashMode(getCameraXFlashMode())
            .build()

        try {
            cameraProvider?.unbindAll()

            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            Log.e(TAG, "相机绑定失败", e)
        }
    }

    suspend fun takePhoto(callback: TackPhotoCallback?): File? = withContext(Dispatchers.IO) {
        val imageCapture = imageCapture ?: return@withContext null

        val photoFile = createPhotoFile()

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "照片保存成功: ${photoFile.absolutePath}")
                    callback?.onSuccess(photoFile.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "拍照失败", exception)
                    callback?.onFailed()
                }
            }
        )

        photoFile
    }

    fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        // 重启相机
        cameraProvider?.unbindAll()
    }

    fun toggleFlashMode() {
        currentFlashMode = when (currentFlashMode) {
            FlashMode.OFF -> FlashMode.ON
            FlashMode.ON -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.OFF
        }
        updateFlashMode()
    }

    fun getFlashMode(): FlashMode = currentFlashMode

    private fun updateFlashMode() {
        val imageCapture = imageCapture ?: return
        imageCapture.flashMode = getCameraXFlashMode()
    }

    private fun getCameraXFlashMode(): Int = when (currentFlashMode) {
        FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        FlashMode.ON -> ImageCapture.FLASH_MODE_ON
        FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
    }

    private fun createPhotoFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        val storageDir = context.getExternalFilesDir("Pictures")

        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    fun cleanup() {
        cameraExecutor.shutdown()
    }
}