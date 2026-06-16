package com.zzd.tool.hook.core

import android.content.Context
import android.content.pm.PackageManager

object HookStatus {

    private var isZygoteHookMode = false
    private var hookProvider: String? = null

    fun init(isZygote: Boolean, provider: String?) {
        isZygoteHookMode = isZygote
        hookProvider = provider
    }

    fun isZygoteHookMode(): Boolean = isZygoteHookMode

    fun getHookProvider(): String? = hookProvider

    fun isLegacyXposed(): Boolean {
        return try {
            Class.forName("de.robv.android.xposed.XposedBridge")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    fun isModuleEnabled(): Boolean {
        return isLegacyXposed() || isZygoteHookMode
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
        if (isZygoteHookMode) {
            return hookProvider ?: "Unknown(Zygote)"
        }
        if (isLegacyXposed()) {
            return "Legacy Xposed"
        }
        return "None"
    }
}
