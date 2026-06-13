package com.zzd.tool.config

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

object SettingsManager {

    private const val TAG = "ZddTool"
    private const val AUTHORITY = "com.zzd.tool.settings"
    private const val PREFS_NAME = "zzdtool_settings"
    private const val TARGET_PKG = "com.alibaba.taurus.zhejiang"

    private const val KEY_TABLET = "hook_tablet"
    private const val KEY_RECALL = "hook_recall"
    private const val KEY_FORWARD = "hook_forward"
    private const val KEY_BADGE = "hook_badge"
    private var cache: JSONObject? = null
    private var initialized = false

    // ── Hook 侧（目标 App 进程） ──

    fun initHooks(context: Context): Boolean {
        if (initialized) return true
        try {
            val resolver = context.contentResolver
            val uri = android.net.Uri.parse("content://$AUTHORITY/settings")
            val result = resolver.query(uri, null, null, null, null)
            if (result != null && result.moveToFirst()) {
                val json = JSONObject(result.getString(0))
                result.close()
                cache = json
                initialized = true
                Log.i(TAG, "Settings: loaded ✅ (平板=${json.optBoolean(KEY_TABLET)}, " +
                    "撤回=${json.optBoolean(KEY_RECALL)}, " +
                    "转发=${json.optBoolean(KEY_FORWARD)}, " +
                    "badge=${json.optBoolean(KEY_BADGE)})")
                return true
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Settings: ${e.message}")
        }
        Log.w(TAG, "Settings: unavailable → all features enabled")
        return false
    }

    fun isTabletEnabled(): Boolean = cache?.optBoolean(KEY_TABLET, true) ?: true
    fun isRecallEnabled(): Boolean = cache?.optBoolean(KEY_RECALL, true) ?: true
    fun isForwardEnabled(): Boolean = cache?.optBoolean(KEY_FORWARD, true) ?: true
    fun isBadgeEnabled(): Boolean = cache?.optBoolean(KEY_BADGE, true) ?: true

    // ── Activity 侧（模块进程） ──

    fun loadSettings(context: Context): JSONObject {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            JSONObject().apply {
                put(KEY_TABLET, prefs.getBoolean(KEY_TABLET, true))
                put(KEY_RECALL, prefs.getBoolean(KEY_RECALL, true))
                put(KEY_FORWARD, prefs.getBoolean(KEY_FORWARD, true))
                put(KEY_BADGE, prefs.getBoolean(KEY_BADGE, true))
            }
        } catch (e: Exception) {
            Log.w(TAG, "loadSettings failed: ${e.message}")
            defaultJson()
        }
    }

    fun saveSetting(context: Context, key: String, value: Boolean) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(key, value).commit()
            Log.i(TAG, "saveSetting $key=$value")
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
            } else {
                Log.w(TAG, "DexKit cache file not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache: ${e.message}")
        }
    }

    fun resetSettings(context: Context): Boolean {
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().clear().commit()
            Log.i(TAG, "Settings reset")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset settings: ${e.message}")
            false
        }
    }

    private fun defaultJson() = JSONObject().apply {
        put(KEY_TABLET, true)
        put(KEY_RECALL, true)
        put(KEY_FORWARD, true)
        put(KEY_BADGE, true)
    }
}
