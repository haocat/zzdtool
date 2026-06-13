package com.zzd.tool.config

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import org.json.JSONObject

class SettingsProvider : ContentProvider() {

    companion object {
        private const val TAG = "ZddTool"
        private val DEFAULTS = JSONObject().apply {
            put("hook_tablet", true)
            put("hook_recall", true)
            put("hook_forward", true)
            put("hook_badge", true)
        }
    }

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI("com.zzd.tool.settings", "settings", 1)
    }

    override fun onCreate(): Boolean = true

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        if (uriMatcher.match(uri) != 1) return null
        return MatrixCursor(arrayOf("json")).apply {
            addRow(arrayOf(readSettings().toString()))
        }
    }

    private fun readSettings(): JSONObject {
        return try {
            val prefs = context?.getSharedPreferences("zzdtool_settings", android.content.Context.MODE_PRIVATE)
                ?: return JSONObject(DEFAULTS.toString())
            JSONObject().apply {
                put("hook_tablet", prefs.getBoolean("hook_tablet", true))
                put("hook_recall", prefs.getBoolean("hook_recall", true))
                put("hook_forward", prefs.getBoolean("hook_forward", true))
                put("hook_badge", prefs.getBoolean("hook_badge", true))
            }
        } catch (_: Exception) { JSONObject(DEFAULTS.toString()) }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?,
                        selectionArgs: Array<String>?): Int = 0
    override fun call(method: String, arg: String?, extras: android.os.Bundle?): android.os.Bundle = android.os.Bundle()
}
