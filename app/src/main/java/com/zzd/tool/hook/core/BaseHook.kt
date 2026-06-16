package com.zzd.tool.hook.core

import android.util.Log

abstract class BaseHook(
    private val feature: HookFeature? = null
) {

    companion object {
        private const val TAG = "ZddTool"
    }

    private val runtimeErrors = mutableListOf<Throwable>()
    private var initialized = false

    open val hookKey: String get() = feature?.key ?: ""
    open val label: String get() = feature?.label ?: this::class.java.simpleName

    fun isEnabled(): Boolean {
        val key = hookKey
        return if (key.isNotEmpty()) SettingsManager.getBoolean(key) else true
    }

    fun isInitialized(): Boolean = initialized

    fun traceError(e: Throwable) {
        if (runtimeErrors.size < 50) runtimeErrors.add(e)
        Log.e(TAG, "[$label] error: ${e.message}", e)
    }

    fun init(cl: ClassLoader): Boolean {
        if (initialized) return true
        return try {
            val result = onInit(cl)
            initialized = result
            if (result) Log.i(TAG, "✔ $label")
            else Log.w(TAG, "✘ $label: onInit returned false")
            result
        } catch (e: Throwable) {
            traceError(e)
            Log.e(TAG, "✘ $label: init failed", e)
            false
        }
    }

    protected abstract fun onInit(cl: ClassLoader): Boolean
}
