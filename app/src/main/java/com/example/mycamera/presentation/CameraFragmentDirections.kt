package com.example.mycamera.presentation

import android.os.Bundle
import androidx.navigation.NavDirections
import com.example.mycamera.R

class CameraFragmentDirections private constructor() {
    companion object {
        fun actionCameraFragmentToPreviewFragment(photoPath: String): NavDirections {
            return object : NavDirections {
                override val actionId: Int
                    get() = R.id.action_mainFragment_to_previewFragment

                override val arguments: Bundle
                    get() {
                        val bundle = Bundle()
                        bundle.putString("photoPath", photoPath)
                        return bundle
                    }
            }
        }
    }
}