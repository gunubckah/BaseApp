package com.example.mycamera

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import com.example.mycamera.data.CameraRepository
import com.example.mycamera.databinding.ActivityMainBinding
import com.example.mycamera.fragment.PreviewDialogFragment
import com.example.mycamera.utils.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )
    }
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置导航控制器
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 配置AppBar
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        // 隐藏ActionBar（因为相机需要全屏）
        supportActionBar?.hide()
    }

    override fun onBackPressed() {
        // 如果是相机页面，直接退出应用
        // 如果是预览页面，返回相机页面
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        if (navController.currentDestination?.id == R.id.mainFragment) {
            super.onBackPressed()
        } else {
            navController.popBackStack()
        }
    }
    private lateinit var previewView: PreviewView
    private lateinit var cameraRepository: CameraRepository


    private fun setupClickListeners() {
        findViewById<ImageButton>(R.id.btn_capture).setOnClickListener {
            capturePhoto()
        }

        findViewById<ImageButton>(R.id.btn_switch_camera).setOnClickListener {
            switchCamera()
        }

        findViewById<ImageButton>(R.id.btn_flash).setOnClickListener {
            toggleFlash()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        lifecycleScope.launch {
            try {
                cameraRepository.startCamera(
                    previewView = previewView,
                    lifecycleOwner = this@MainActivity
                )
            } catch (e: Exception) {
                LogUtil.e(TAG, "相机启动失败: ${e.message}")
                Toast.makeText(
                    this@MainActivity,
                    "相机启动失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun capturePhoto() {
        lifecycleScope.launch {
            val photoFile = cameraRepository.takePhoto(null)
            if (photoFile != null) {
                // 显示拍照成功动画
                showCaptureAnimation()
                // 更新预览图
                updateLastPhotoPreview(photoFile)
                Toast.makeText(
                    this@MainActivity,
                    "照片已保存",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun switchCamera() {
        cameraRepository.switchCamera()
    }

    private fun toggleFlash() {
        cameraRepository.toggleFlashMode()
        updateFlashButtonIcon()
    }

    private fun updateFlashButtonIcon() {
        val iconRes = when (cameraRepository.getFlashMode()) {
            CameraRepository.FlashMode.OFF -> R.drawable.ic_flash_off
            CameraRepository.FlashMode.ON -> R.drawable.ic_flash_on
            CameraRepository.FlashMode.AUTO -> R.drawable.ic_flash_auto
        }
        findViewById<ImageButton>(R.id.btn_flash).setImageResource(iconRes)
    }

    private fun showCaptureAnimation() {
        val captureRing = findViewById<View>(R.id.capture_ring)
        captureRing.visibility = View.VISIBLE

        val scaleAnim = ObjectAnimator.ofPropertyValuesHolder(
            captureRing,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.2f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.2f, 1f)
        ).apply {
            duration = 300
        }

        val alphaAnim = ObjectAnimator.ofPropertyValuesHolder(
            captureRing,
            PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f)
        ).apply {
            duration = 300
        }

        AnimatorSet().apply {
            playTogether(scaleAnim, alphaAnim)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    captureRing.visibility = View.INVISIBLE
                }
            })
            start()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "需要相机权限才能使用应用",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun updateLastPhotoPreview(photoFile: File) {
        val imageView = findViewById<ImageView>(R.id.iv_last_photo)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 使用BitmapFactory创建缩略图
                val thumbnail = createThumbnail(photoFile, 200, 200)

                withContext(Dispatchers.Main) {
                    thumbnail?.let {
                        // 添加淡入动画
                        val fadeIn = ObjectAnimator.ofFloat(imageView, View.ALPHA, 0f, 1f)
                        fadeIn.duration = 300
                        fadeIn.start()

                        imageView.setImageBitmap(it)

                        // 设置点击事件，点击预览图片
                        imageView.setOnClickListener {
                            showPhotoPreview(photoFile)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "更新预览图失败", e)
            }
        }
    }

    private fun createThumbnail(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            // 第一次解码，只获取图片尺寸
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            // 计算合适的采样率
            val (width, height) = options.outWidth to options.outHeight
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2

                while ((halfHeight / inSampleSize) >= reqHeight &&
                    (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            // 第二次解码，使用采样率
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = inSampleSize
                inJustDecodeBounds = false
                inPreferredConfig = Bitmap.Config.RGB_565 // 节省内存
            }

            var bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)

            // 如果图片有旋转信息，需要进行旋转
            val rotation = getImageRotation(file.absolutePath)
            if (rotation != 0 && bitmap != null) {
                bitmap = rotateBitmap(bitmap, rotation)
            }

            // 裁剪为正方形
            bitmap?.let {
                val size = minOf(it.width, it.height)
                val x = (it.width - size) / 2
                val y = (it.height - size) / 2

                Bitmap.createBitmap(it, x, y, size, size)?.let { cropped ->
                    Bitmap.createScaledBitmap(cropped, reqWidth, reqHeight, true)
                }
            }
        } catch (e: Exception) {
            Log.e("Thumbnail", "创建缩略图失败", e)
            null
        }
    }

    private fun getImageRotation(filePath: String): Int {
        return try {
            val exif = ExifInterface(filePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: IOException) {
            0
        }
    }

    private fun rotateBitmap(source: Bitmap, angle: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height, matrix, true
        )
    }

    // 显示照片预览
    private fun showPhotoPreview(photoFile: File) {
        val previewDialog = PreviewDialogFragment.newInstance(photoFile.absolutePath)
        previewDialog.show(supportFragmentManager, "PreviewDialog")
    }
}