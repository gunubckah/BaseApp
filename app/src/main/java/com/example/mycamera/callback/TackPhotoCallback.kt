package com.example.mycamera.callback

interface TackPhotoCallback {
    /**
     * 拍照保存成功
     */
    fun onSuccess(path: String)

    /**
     * 拍照保存失败
     */
    fun onFailed()
}