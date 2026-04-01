package com.example.mycamera.fragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.mycamera.R
import com.example.mycamera.databinding.DialogPreviewBinding
import java.io.File

class PreviewDialogFragment : DialogFragment() {

    private var _binding: DialogPreviewBinding? = null
    private val binding get() = _binding!!
    private lateinit var photoFile: File

    companion object {
        private const val ARG_PHOTO_PATH = "photo_path"

        fun newInstance(photoPath: String): PreviewDialogFragment {
            val fragment = PreviewDialogFragment()
            val args = Bundle().apply {
                putString(ARG_PHOTO_PATH, photoPath)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val photoPath = arguments?.getString(ARG_PHOTO_PATH) ?: return
        photoFile = File(photoPath)

        loadImage()
        setupClickListeners()
    }

    private fun loadImage() {
        // 使用Glide加载图片
        Glide.with(this)
            .load(photoFile)
            .into(binding.imageView)
    }

    private fun setupClickListeners() {
        // 点击任意位置关闭
        binding.imageView.setOnClickListener {
            dismiss()
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()

        // 设置对话框全屏
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawable(ColorDrawable(Color.BLACK))
            setWindowAnimations(R.style.DialogAnimation)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}