package com.example.mycamera.presentation

import android.os.Bundle
import androidx.navigation.NavArgs
import java.io.Serializable

class PreviewFragmentArgs : NavArgs, Serializable {
    val photoPath: String
    
    constructor(photoPath: String) {
        this.photoPath = photoPath
    }
    
    constructor(bundle: Bundle) {
        photoPath = bundle.getString("photoPath", "")
    }
    
    fun toBundle(): Bundle {
        val result = Bundle()
        result.putString("photoPath", photoPath)
        return result
    }
    
    companion object {
        @JvmStatic
        fun fromBundle(bundle: Bundle): PreviewFragmentArgs {
            return PreviewFragmentArgs(bundle)
        }
    }
}