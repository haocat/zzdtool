package com.zzd.tool.hook.core

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tencent.mmkv.MMKV
import com.zzd.tool.config.SettingsProvider

object SettingsManager {

    private const val TAG = "ZddTool"
    private val settings = HashMap<String, Boolean>()
    private var activated = false
    private var providerName = "None"

    fun initFromContentResolver(context: Context): Boolean {
        return try {
            val uri = Uri.parse("content://${SettingsProvider.AUTHORITY}/settings")
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val json = org.json.JSONObject(cursor.getString(0))
                    settings.clear()
                    settings["hook_tablet"] = json.optBoolean("hook_tablet", true)
                    settings["hook_recall"] = json.optBoolean("hook_recall", true)
                    settings["hook_forward"] = json.optBoolean("hook_forward", true)
                    settings["hook_badge"] = json.optBoolean("hook_badge", true)
                    settings["hook_dark_mode"] = json.optBoolean("hook_dark_mode", false)
                    activated = json.optBoolean("activated", false)
                    providerName = json.optString("provider", "None")
                    Log.i(TAG, "Settings: $settings")
                    true
                } else false
            } ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Settings query failed: ${e.message}")
            false
        }
    }

    fun getBoolean(key: String): Boolean = settings[key] ?: true

    fun isActivated(): Boolean = activated

    fun getProvider(): String = providerName

    fun loadAll(context: Context): Map<String, Boolean> {
        ensureMMKV(context)
        val kv = MMKV.mmkvWithID("zzdtool_settings") ?: return defaultMap()
        return mapOf(
            "hook_tablet" to kv.decodeBool("hook_tablet", true),
            "hook_recall" to kv.decodeBool("hook_recall", true),
            "hook_forward" to kv.decodeBool("hook_forward", true),
            "hook_badge" to kv.decodeBool("hook_badge", true),
            "hook_dark_mode" to kv.decodeBool("hook_dark_mode", false)
        )
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        ensureMMKV(context)
        MMKV.mmkvWithID("zzdtool_settings")?.encode(key, value)
        Log.i(TAG, "Settings: $key=$value")
    }

    fun resetAll(context: Context) {
        ensureMMKV(context)
        MMKV.mmkvWithID("zzdtool_settings")?.clearAll()
        Log.i(TAG, "Settings: reset")
    }

    fun clearDexKitCache(context: Context) {
        try {
            MMKV.mmkvWithID("zzdtool_dexkit")?.clearAll()
            Log.i(TAG, "DexKit cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Clear cache failed: ${e.message}")
        }
    }

    private fun ensureMMKV(context: Context) {
        try {
            MMKV.initialize(context)
        } catch (_: Exception) {}
    }

    private fun defaultMap() = mapOf(
        "hook_tablet" to true,
        "hook_recall" to true,
        "hook_forward" to true,
        "hook_badge" to true,
        "hook_dark_mode" to false
    )
}
