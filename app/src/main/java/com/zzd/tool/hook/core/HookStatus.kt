package com.zzd.tool.hook.core

import com.tencent.mmkv.MMKV

object HookStatus {

    private const val KEY_ACTIVATED = "module_activated"

    private val mmkv by lazy { MMKV.mmkvWithID("zzdtool_status", MMKV.MULTI_PROCESS_MODE) }

    fun init(provider: String?) {
        try {
            mmkv.encode(KEY_ACTIVATED, true)
        } catch (_: Throwable) {}
    }

    fun isModuleEnabled(): Boolean {
        return try {
            mmkv.decodeBool(KEY_ACTIVATED, false)
        } catch (_: Throwable) {
            false
        }
    }

    fun detectProvider(): String {
        return try {
            val xposedClass = ClassLoader.getSystemClassLoader()
                .loadClass("de.robv.android.xposed.XposedBridge")
            val tag = xposedClass.getDeclaredField("TAG").get(null) as? String
            when {
                tag?.startsWith("LSPosed") == true -> "LSPosed"
                tag?.startsWith("EdXposed") == true -> "EdXposed"
                tag?.startsWith("PineXposed") == true -> "Dreamland"
                else -> "Xposed"
            }
        } catch (_: Throwable) {
            "None"
        }
    }
}
