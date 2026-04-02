package com.example.mycamera.utils

import android.util.Log
import com.example.mycamera.BuildConfig

/**
 * 简洁的日志管理工具类
 * 功能：
 * 1. 提供不同级别的日志输出
 * 2. 开发环境输出所有日志，生产环境只输出错误日志
 * 3. 支持自定义 TAG
 */
object LogUtil {
    
    // 默认 TAG
    private const val DEFAULT_TAG = "MyCamera"
    
    // 日志开关：开发环境开启，生产环境关闭
    var isLoggable = BuildConfig.DEBUG
    
    // 是否启用详细日志
    var enableVerbose = true
    var enableDebug = true
    var enableInfo = true
    var enableWarn = true
    var enableError = true
    
    /**
     * Verbose 级别日志
     */
    fun v(tag: String = DEFAULT_TAG, message: String) {
        if (isLoggable && enableVerbose) {
            Log.v(tag, message)
        }
    }
    
    /**
     * Debug 级别日志
     */
    fun d(tag: String = DEFAULT_TAG, message: String) {
        if (isLoggable && enableDebug) {
            Log.d(tag, message)
        }
    }
    
    /**
     * Info 级别日志
     */
    fun i(tag: String = DEFAULT_TAG, message: String) {
        if (isLoggable && enableInfo) {
            Log.i(tag, message)
        }
    }
    
    /**
     * Warning 级别日志
     */
    fun w(tag: String = DEFAULT_TAG, message: String) {
        if (isLoggable && enableWarn) {
            Log.w(tag, message)
        }
    }
    
    /**
     * Error 级别日志
     */
    fun e(tag: String = DEFAULT_TAG, message: String) {
        if (isLoggable && enableError) {
            Log.e(tag, message)
        }
    }
    
    /**
     * 带异常信息的 Error 日志
     */
    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable) {
        if (isLoggable && enableError) {
            Log.e(tag, message, throwable)
        }
    }
}