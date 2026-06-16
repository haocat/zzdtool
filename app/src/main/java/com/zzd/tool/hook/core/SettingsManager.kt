package com.zzd.tool.hook.core

import android.content.Context
import android.net.Uri
import android.util.Log
import com.zzd.tool.config.SettingsProvider
import org.json.JSONObject

object SettingsManager {

    private const val TAG = "ZddTool"
    private var cache: JSONObject? = null

    fun initFromContentResolver(context: Context): Boolean {
        return try {
            val uri = Uri.parse("content://${SettingsProvider.AUTHORITY}/settings")
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cache = JSONObject(cursor.getString(0))
                    Log.i(TAG, "Settings loaded from provider: $cache")
                    true
                } else false
            } ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Settings query failed: ${e.message}")
            false
        }
    }

    fun getBoolean(key: String): Boolean {
        return cache?.optBoolean(key, true) ?: true
    }

    fun loadAll(context: Context): Map<String, Boolean> {
        val prefs = context.getSharedPreferences("zzdtool_settings", Context.MODE_PRIVATE)
        return mapOf(
            "hook_tablet" to prefs.getBoolean("hook_tablet", true),
            "hook_recall" to prefs.getBoolean("hook_recall", true),
            "hook_forward" to prefs.getBoolean("hook_forward", true),
            "hook_badge" to prefs.getBoolean("hook_badge", true)
        )
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        context.getSharedPreferences("zzdtool_settings", Context.MODE_PRIVATE)
            .edit().putBoolean(key, value).commit()
        Log.i(TAG, "Settings: $key=$value")
    }

    fun resetAll(context: Context) {
        context.getSharedPreferences("zzdtool_settings", Context.MODE_PRIVATE)
            .edit().clear().commit()
        Log.i(TAG, "Settings: reset")
    }

    fun clearDexKitCache(context: Context) {
        try {
            val f = java.io.File(context.filesDir, "zzdtool_dexkit.json")
            if (f.exists()) { f.delete(); Log.i(TAG, "DexKit cache cleared") }
        } catch (e: Exception) {
            Log.e(TAG, "Clear cache failed: ${e.message}")
        }
    }
}
