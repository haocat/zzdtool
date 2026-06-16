package com.zzd.tool.hook.core

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONObject

object SettingsManager {

    private const val TAG = "ZddTool"
    private const val AUTHORITY = "com.zzd.tool.settings"

    fun getBoolean(key: String): Boolean {
        return try {
            val uri = Uri.parse("content://$AUTHORITY/settings")
            val cursor = android.app.ActivityThread.currentApplication()
                ?.contentResolver?.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val json = JSONObject(it.getString(0))
                    json.optBoolean(key, true)
                } else true
            } ?: true
        } catch (_: Exception) {
            true
        }
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        try {
            context.getSharedPreferences("zzdtool_settings", Context.MODE_PRIVATE)
                .edit().putBoolean(key, value).commit()
            Log.i(TAG, "Settings: $key=$value")
        } catch (e: Exception) {
            Log.e(TAG, "Settings save failed: ${e.message}")
        }
    }

    fun loadAll(): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        for (feature in HookFeature.entries) {
            result[feature.key] = getBoolean(feature.key)
        }
        return result
    }

    fun resetAll(context: Context) {
        try {
            context.getSharedPreferences("zzdtool_settings", Context.MODE_PRIVATE)
                .edit().clear().commit()
            Log.i(TAG, "Settings: reset to defaults")
        } catch (e: Exception) {
            Log.e(TAG, "Settings reset failed: ${e.message}")
        }
    }

    fun clearDexKitCache(context: Context) {
        try {
            val cacheFile = java.io.File(context.filesDir, "zzdtool_dexkit.json")
            if (cacheFile.exists()) {
                cacheFile.delete()
                Log.i(TAG, "DexKit cache cleared")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache: ${e.message}")
        }
    }
}
