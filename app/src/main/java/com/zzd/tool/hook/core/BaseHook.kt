package com.zzd.tool.hook.core

import android.util.Log

abstract class BaseHook(
    private val feature: HookFeature
) {

    companion object {
        private const val TAG = "ZddTool"
    }

    private val runtimeErrors = mutableListOf<Throwable>()
    private var initialized = false

    val hookKey: String get() = feature.key
    val label: String get() = feature.label

    fun isEnabled(): Boolean =
        SettingsManager.getBoolean(hookKey)

    fun isInitialized(): Boolean = initialized

    fun hasErrors(): Boolean = runtimeErrors.isNotEmpty()

    fun getRuntimeErrors(): List<Throwable> = runtimeErrors.toList()

    fun traceError(e: Throwable) {
        if (runtimeErrors.size < 50) {
            runtimeErrors.add(e)
        }
        Log.e(TAG, "[$label] runtime error: ${e.message}", e)
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
