package com.zzd.tool.config

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import com.tencent.mmkv.MMKV

class SettingsProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.zzd.tool.settings"
        private const val TAG = "ZddTool"
    }

    private var mmkv: MMKV? = null

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(AUTHORITY, "settings", 1)
    }

    override fun onCreate(): Boolean {
        context?.let {
            MMKV.initialize(it)
            mmkv = MMKV.mmkvWithID("zzdtool_settings")
        }
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        if (uriMatcher.match(uri) != 1) return null
        return MatrixCursor(arrayOf("json")).apply {
            addRow(arrayOf(readSettings().toString()))
        }
    }

    private fun readSettings(): org.json.JSONObject {
        return try {
            org.json.JSONObject().apply {
                put("hook_tablet", mmkv?.decodeBool("hook_tablet", true) ?: true)
                put("hook_recall", mmkv?.decodeBool("hook_recall", true) ?: true)
                put("hook_forward", mmkv?.decodeBool("hook_forward", true) ?: true)
                put("hook_badge", mmkv?.decodeBool("hook_badge", true) ?: true)
                put("activated", mmkv?.decodeBool("activated", false) ?: false)
                put("provider", mmkv?.decodeString("provider") ?: "None")
            }
        } catch (_: Exception) {
            org.json.JSONObject().apply {
                put("hook_tablet", true); put("hook_recall", true)
                put("hook_forward", true); put("hook_badge", true)
                put("activated", false); put("provider", "None")
            }
        }
    }

    override fun call(method: String, arg: String?, extras: android.os.Bundle?): android.os.Bundle {
        return android.os.Bundle().apply {
            if (method == "activate") {
                mmkv?.encode("activated", true)
                mmkv?.encode("provider", arg ?: "Unknown")
                Log.i(TAG, "Activation: $arg")
            }
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?,
                        selectionArgs: Array<String>?): Int = 0
}
