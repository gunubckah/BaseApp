package com.example.mycamera.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.mycamera.R
import com.example.mycamera.databinding.FragmentPreviewBinding
import com.example.mycamera.presentation.PreviewFragmentArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PreviewFragment : Fragment() {

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var photoFile: File

    // 使用导航参数传递照片路径
    private val args: PreviewFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 从导航参数获取照片路径
        val photoPath = args.photoPath
        if (photoPath.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "照片路径无效", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        photoFile = File(photoPath)
        if (!photoFile.exists()) {
            Toast.makeText(requireContext(), "照片文件不存在", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        setupToolbar()
        loadImage()
        setupButtons()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun loadImage() {
        // 使用协程在后台线程加载图片
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(photoFile.absolutePath, options)

                val screenWidth = resources.displayMetrics.widthPixels
                val sampleSize = (options.outWidth / screenWidth).coerceAtLeast(1)

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inJustDecodeBounds = false
                    inPreferredConfig = Bitmap.Config.RGB_565
                }

                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath, decodeOptions)

                withContext(Dispatchers.Main) {
                    binding.ivPreview.setImageBitmap(bitmap)

                    // 添加图片点击效果
                    binding.ivPreview.setOnClickListener {
                        toggleFullscreen()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "图片加载失败: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnShare.setOnClickListener {
            sharePhoto()
        }

        binding.btnDelete.setOnClickListener {
            deletePhoto()
        }

        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun sharePhoto() {
        val photoUri = Uri.fromFile(photoFile)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, photoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, getString(R.string.share))
        startActivity(chooserIntent)
    }

    private fun deletePhoto() {
        if (photoFile.exists() && photoFile.delete()) {
            Toast.makeText(requireContext(), "照片已删除", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        } else {
            Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleFullscreen() {
        val isToolbarVisible = binding.appBarLayout.visibility == View.VISIBLE
        val isBottomLayoutVisible = binding.controlsLayout.visibility == View.VISIBLE

        binding.appBarLayout.visibility = if (isToolbarVisible) View.GONE else View.VISIBLE
        binding.controlsLayout.visibility = if (isBottomLayoutVisible) View.GONE else View.VISIBLE

        // 切换全屏状态
        if (isToolbarVisible) {
            activity?.window?.decorView?.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        } else {
            activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}