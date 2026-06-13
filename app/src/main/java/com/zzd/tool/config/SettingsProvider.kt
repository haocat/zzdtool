package com.zzd.tool.config

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import org.json.JSONObject
import java.io.File

class SettingsProvider : ContentProvider() {

    companion object {
        private const val TAG = "ZddTool"
        private const val SETTINGS_FILE = "zzdtool_settings.json"
        private val DEFAULTS = JSONObject().apply {
            put("hook_tablet", true)
            put("hook_recall", true)
            put("hook_forward", true)
            put("hook_badge", true)
            put("dexkit_cache", true)
        }
    }

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: android.os.Bundle?): android.os.Bundle {
        val result = android.os.Bundle()
        when (method) {
            "getAll" -> {
                result.putString("json", readFile().toString())
            }
            "get" -> {
                val key = arg ?: return result
                result.putBoolean("value", readFile().optBoolean(key, DEFAULTS.optBoolean(key, true)))
            }
            "put" -> {
                val key = extras?.getString("key") ?: return result
                val value = extras?.getBoolean("value") ?: return result
                val json = readFile()
                json.put(key, value)
                writeFile(json)
                result.putBoolean("ok", true)
            }
        }
        return result
    }

    private fun readFile(): JSONObject {
        val file = File(context?.filesDir, SETTINGS_FILE)
        return try {
            if (file.exists()) JSONObject(file.readText()) else DEFAULTS
        } catch (_: Exception) { DEFAULTS }
    }

    private fun writeFile(json: JSONObject) {
        val file = File(context?.filesDir, SETTINGS_FILE)
        try {
            file.writeText(json.toString(2))
            file.setReadable(true, false)
        } catch (e: Exception) {
            Log.e(TAG, "Settings write failed: ${e.message}")
        }
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?,
                        selectionArgs: Array<String>?): Int = 0
}
