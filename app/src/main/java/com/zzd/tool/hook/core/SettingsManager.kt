package com.zzd.tool.hook.core

import android.content.Context
import android.util.Log
import com.tencent.mmkv.MMKV

object SettingsManager {

    private const val TAG = "ZddTool"

    private val mmkv by lazy { MMKV.mmkvWithID("zzdtool_config", MMKV.MULTI_PROCESS_MODE) }

    fun getBoolean(key: String): Boolean = mmkv.decodeBool(key, true)

    fun putBoolean(key: String, value: Boolean) {
        mmkv.encode(key, value)
        Log.i(TAG, "Settings: $key=$value")
    }

    fun loadAll(): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        for (feature in HookFeature.entries) {
            result[feature.key] = getBoolean(feature.key)
        }
        return result
    }

    fun resetAll() {
        HookFeature.entries.forEach { mmkv.remove(it.key) }
        Log.i(TAG, "Settings: reset to defaults")
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
