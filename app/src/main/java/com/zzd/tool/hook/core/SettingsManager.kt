package com.zzd.tool.hook.core

import android.util.Log
import com.tencent.mmkv.MMKV

object SettingsManager {

    private const val TAG = "ZddTool"

    private val mmkv by lazy {
        MMKV.mmkvWithID("zzdtool_config", MMKV.MULTI_PROCESS_MODE)
    }

    fun getBoolean(key: String, default: Boolean): Boolean =
        mmkv.decodeBool(key, default)

    fun putBoolean(key: String, value: Boolean) {
        mmkv.encode(key, value)
        Log.i(TAG, "Settings: $key=$value")
    }

    fun loadAll(): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        for (feature in HookFeature.entries) {
            result[feature.key] = getBoolean(feature.key, feature.defaultEnabled)
        }
        return result
    }

    fun saveAll(settings: Map<String, Boolean>) {
        settings.forEach { (key, value) ->
            mmkv.encode(key, value)
        }
        Log.i(TAG, "Settings: saved ${settings.size} items")
    }

    fun resetAll() {
        HookFeature.entries.forEach { mmkv.remove(it.key) }
        Log.i(TAG, "Settings: reset to defaults")
    }

    fun clearDexKitCache(cacheDir: java.io.File?) {
        try {
            val cacheFile = java.io.File(cacheDir, "zzdtool_dexkit.json")
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
}
