package com.zzd.tool.config

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import de.robv.android.xposed.XSharedPreferences
import java.io.File

object SettingsManager {

    private const val TAG = "ZddTool"
    private const val PREFS_NAME = "zzdtool_settings"

    // Keys
    private const val KEY_TABLET = "hook_tablet"
    private const val KEY_RECALL = "hook_recall"
    private const val KEY_FORWARD = "hook_forward"
    private const val KEY_BADGE = "hook_badge"
    private const val KEY_CACHE = "dexkit_cache"

    // Defaults
    private const val DEFAULT_TABLET = true
    private const val DEFAULT_RECALL = true
    private const val DEFAULT_FORWARD = true
    private const val DEFAULT_BADGE = true
    private const val DEFAULT_CACHE = true

    private var xPrefs: SharedPreferences? = null
    private var initialized = false

    fun initHooks(context: Context): Boolean {
        if (initialized) return true

        // 方法 1: XSharedPreferences — Xposed 标准方式
        try {
            val xp = XSharedPreferences("com.zzd.tool", PREFS_NAME)
            xp.reload()
            xPrefs = xp
            initialized = true
            Log.i(TAG, "Settings: XSharedPreferences ✅")
            return true
        } catch (e: Throwable) {
            Log.w(TAG, "XSharedPreferences: ${e.message}")
        }

        // 方法 2: createPackageContext — 备选
        try {
            val ctx = context.createPackageContext("com.zzd.tool", 0)
            xPrefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            initialized = true
            Log.i(TAG, "Settings: createPackageContext ✅")
            return true
        } catch (e: Throwable) {
            Log.w(TAG, "createPackageContext: ${e.message}")
        }

        Log.w(TAG, "Settings not available, all features enabled")
        return false
    }

    private fun getBoolean(key: String, default: Boolean): Boolean {
        return xPrefs?.getBoolean(key, default) ?: default
    }

    fun isTabletEnabled(): Boolean = getBoolean(KEY_TABLET, DEFAULT_TABLET)
    fun isRecallEnabled(): Boolean = getBoolean(KEY_RECALL, DEFAULT_RECALL)
    fun isForwardEnabled(): Boolean = getBoolean(KEY_FORWARD, DEFAULT_FORWARD)
    fun isBadgeEnabled(): Boolean = getBoolean(KEY_BADGE, DEFAULT_BADGE)
    fun isCacheEnabled(): Boolean = getBoolean(KEY_CACHE, DEFAULT_CACHE)

    fun getActivityPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun clearDexKitCache(context: Context) {
        try {
            val filesDir = context.filesDir
            val cacheFile = File(filesDir, "zzdtool_dexkit.json")
            if (cacheFile.exists()) {
                cacheFile.delete()
                Log.i(TAG, "DexKit cache cleared")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache: ${e.message}")
        }
    }
}