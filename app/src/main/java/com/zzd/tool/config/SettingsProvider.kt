package com.zzd.tool.config

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log

class SettingsProvider : ContentProvider() {

    companion object {
        private const val TAG = "ZddTool"
        private const val PREFS_NAME = "zzdtool_settings"

        private val defaults = mapOf(
            "hook_tablet" to true,
            "hook_recall" to true,
            "hook_forward" to true,
            "hook_badge" to true
        )

        private var cache: MutableMap<String, Boolean>? = null

        fun getBoolean(key: String): Boolean {
            return cache?.get(key) ?: defaults[key] ?: true
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

    private fun readSettings(): org.json.JSONObject {
        return try {
            val prefs = context?.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                ?: return defaults toJson defaults
            cache = defaults.toMutableMap().apply {
                keys.forEach { put(it, prefs.getBoolean(it, defaults[it] ?: true)) }
            }
            org.json.JSONObject().apply {
                cache?.forEach { (k, v) -> put(k, v) }
            }
        } catch (_: Exception) {
            defaults toJson defaults
        }
    }

    private infix fun Map<String, Boolean>.toJson(other: Map<String, Boolean>): org.json.JSONObject {
        return org.json.JSONObject().apply { other.forEach { (k, v) -> put(k, v) } }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?,
                        selectionArgs: Array<String>?): Int = 0
}
