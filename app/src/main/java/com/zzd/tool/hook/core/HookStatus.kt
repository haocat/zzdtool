package com.zzd.tool.hook.core

import android.content.Context
import android.net.Uri
import com.zzd.tool.config.StatusProvider

object HookStatus {

    private const val AUTHORITY = "com.zzd.tool.status"

    fun init(provider: String?) {
        StatusProvider.setStatus(true, provider)
    }

    fun isModuleEnabled(context: Context): Boolean {
        return queryStatus(context)?.firstOrNull() == "true"
    }

    fun getHookProviderName(context: Context): String {
        return queryStatus(context)?.getOrNull(1) ?: "None"
    }

    private fun queryStatus(context: Context): Array<String>? {
        return try {
            val uri = Uri.parse("content://$AUTHORITY/check")
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    arrayOf(
                        cursor.getString(cursor.getColumnIndexOrThrow("activated")),
                        cursor.getString(cursor.getColumnIndexOrThrow("provider"))
                    )
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
