package com.zzd.tool.config

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import org.json.JSONObject
import java.io.File

object SettingsManager {

    private const val TAG = "ZddTool"
    private const val AUTHORITY = "com.zzd.tool.settings"
    private val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")

    private const val KEY_TABLET = "hook_tablet"
    private const val KEY_RECALL = "hook_recall"
    private const val KEY_FORWARD = "hook_forward"
    private const val KEY_BADGE = "hook_badge"
    private const val KEY_CACHE = "dexkit_cache"

    private var cache: JSONObject? = null
    private var initialized = false

    fun initHooks(context: Context): Boolean {
        if (initialized) return true
        try {
            val resolver = context.contentResolver
            val result = resolver.call(CONTENT_URI, "getAll", null, null)
            val json = result?.getString("json")
            if (json != null) {
                cache = JSONObject(json)
                initialized = true
                Log.i(TAG, "Settings: loaded via ContentProvider ✅")
                return true
            }
        } catch (e: Throwable) {
            Log.w(TAG, "ContentProvider failed: ${e.message}")
        }
        Log.w(TAG, "Settings: unavailable → all features enabled")
        return false
    }

    fun isTabletEnabled(): Boolean = cache?.optBoolean(KEY_TABLET, true) ?: true
    fun isRecallEnabled(): Boolean = cache?.optBoolean(KEY_RECALL, true) ?: true
    fun isForwardEnabled(): Boolean = cache?.optBoolean(KEY_FORWARD, true) ?: true
    fun isBadgeEnabled(): Boolean = cache?.optBoolean(KEY_BADGE, true) ?: true
    fun isCacheEnabled(): Boolean = cache?.optBoolean(KEY_CACHE, true) ?: true

    // ── Activity 侧 ──

    fun loadSettings(context: Context): JSONObject {
        return try {
            val resolver = context.contentResolver
            val result = resolver.call(CONTENT_URI, "getAll", null, null)
            val json = result?.getString("json")
            if (json != null) JSONObject(json) else defaultJson()
        } catch (e: Exception) {
            Log.w(TAG, "loadSettings failed: ${e.message}")
            defaultJson()
        }
    }

    fun saveSetting(context: Context, key: String, value: Boolean) {
        try {
            val resolver = context.contentResolver
            val args = Bundle().apply {
                putString("key", key)
                putBoolean("value", value)
            }
            resolver.call(CONTENT_URI, "put", null, args)
        } catch (e: Exception) {
            Log.e(TAG, "saveSetting failed: ${e.message}")
        }
    }

    fun clearDexKitCache(context: Context) {
        try {
            val cacheFile = File(context.filesDir, "zzdtool_dexkit.json")
            if (cacheFile.exists()) {
                cacheFile.delete()
                Log.i(TAG, "DexKit cache cleared")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache: ${e.message}")
        }
    }

    private fun defaultJson() = JSONObject().apply {
        put(KEY_TABLET, true)
        put(KEY_RECALL, true)
        put(KEY_FORWARD, true)
        put(KEY_BADGE, true)
        put(KEY_CACHE, true)
    }
}
