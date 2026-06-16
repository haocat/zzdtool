package com.zzd.tool.hook.core

import android.content.Context
import android.content.pm.PackageManager
import com.tencent.mmkv.MMKV

object HookStatus {

    private const val KEY_ACTIVATED = "module_activated"
    private const val KEY_PROVIDER = "hook_provider"

    private val mmkv by lazy {
        MMKV.mmkvWithID("zzdtool_status", MMKV.MULTI_PROCESS_MODE)
    }

    fun init(provider: String?) {
        mmkv.encode(KEY_ACTIVATED, true)
        mmkv.encode(KEY_PROVIDER, provider ?: "Unknown")
    }

    fun isLegacyXposed(): Boolean {
        return try {
            ClassLoader.getSystemClassLoader().loadClass("de.robv.android.xposed.XposedBridge")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    fun isModuleEnabled(): Boolean {
        return mmkv.decodeBool(KEY_ACTIVATED, false)
    }

    fun isTaiChiInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("me.weishu.exp", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getHookProviderName(): String {
        return mmkv.decodeString(KEY_PROVIDER, "None") ?: "None"
    }
}
