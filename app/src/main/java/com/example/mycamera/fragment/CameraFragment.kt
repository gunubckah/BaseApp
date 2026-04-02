package com.example.mycamera.fragment

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
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mycamera.R
import com.example.mycamera.callback.TackPhotoCallback
import com.example.mycamera.data.CameraRepository
import com.example.mycamera.databinding.FragmentCameraBinding
import com.example.mycamera.presentation.CameraFragmentDirections
import com.example.mycamera.utils.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class CameraFragment : Fragment() {

    private val TAG = "CameraFragment"
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraRepository: CameraRepository

    private val mHandle = Handler(Looper.getMainLooper()){msg ->
        when(msg.what){
            UPDATE_PREVIEW_PHOTO -> {
                updateLastPhotoPreview(msg.obj as File)
                true
            }
            else -> {
                false
            }
        }
    }
    private val requiredPermissions = arrayOf(
        android.Manifest.permission.CAMERA
    )

    // 权限请求启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startCamera()
        } else {
            Toast.makeText(
                requireContext(),
                "需要相机权限才能使用应用",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraRepository = CameraRepository(requireContext())

        // 检查权限
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        setupClickListeners()
    }

    private fun allPermissionsGranted(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(
                requireContext(),
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(requiredPermissions)
    }

    private fun setupClickListeners() {
        binding.btnCapture.setOnClickListener {
            capturePhoto()
        }

        binding.btnSwitchCamera.setOnClickListener {
            switchCamera()
        }

        binding.btnFlash.setOnClickListener {
            toggleFlash()
        }

        binding.btnMode.setOnClickListener {
            // TODO: 模式切换功能
        }
    }

    private fun startCamera() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                cameraRepository.startCamera(
                    previewView = binding.previewView,
                    lifecycleOwner = viewLifecycleOwner
                )
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "相机启动失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun capturePhoto() {
        CoroutineScope(Dispatchers.IO).launch {
            val photoFile = cameraRepository.takePhoto(object : TackPhotoCallback{
                override fun onSuccess(path: String) {
                    val updateMsg = Message.obtain(mHandle, UPDATE_PREVIEW_PHOTO)
                    updateMsg.obj = File(path)
                    mHandle.sendMessage(updateMsg)
                }

                override fun onFailed() {
                    LogUtil.e(TAG, "takePhoto save photo failed")
                }
            })
            if (photoFile != null) {
                // 显示拍照成功动画
                withContext(Dispatchers.Main) {
                    showCaptureAnimation()
                    // 更新预览图

                    Toast.makeText(
                        requireContext(),
                        "照片已保存",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
        binding.btnFlash.setImageResource(iconRes)
    }

    private fun showCaptureAnimation() {
        val captureRing = binding.captureRing
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

    private fun updateLastPhotoPreview(photoFile: File) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val thumbnail = createThumbnail(photoFile, 200, 200)

                withContext(Dispatchers.Main) {
                    thumbnail?.let {
                        // 添加淡入动画
                        val fadeIn = ObjectAnimator.ofFloat(
                            binding.ivLastPhoto,
                            View.ALPHA,
                            0f,
                            1f
                        )
                        fadeIn.duration = 300
                        fadeIn.start()

                        binding.ivLastPhoto.setImageBitmap(it)

                        // 设置点击事件
                        binding.ivLastPhoto.setOnClickListener {
                            LogUtil.d(TAG, "ivLastPhoto has clicked")
                            // 导航到预览页面
                            val action = CameraFragmentDirections
                                .actionCameraFragmentToPreviewFragment(
                                    photoPath = photoFile.absolutePath
                                )
                            findNavController().navigate(action)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraFragment", "更新预览图失败", e)
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
                inPreferredConfig = Bitmap.Config.RGB_565
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
            when (exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        cameraRepository.cleanup()
        _binding = null
    }

    companion object{
        private const val UPDATE_PREVIEW_PHOTO = 1001
    }
}