package com.zzd.tool.hook.core

import com.tencent.mmkv.MMKV

object HookStatus {

    private const val KEY_ACTIVATED = "module_activated"
    private const val KEY_PROVIDER = "hook_provider"

    private val mmkv by lazy { MMKV.mmkvWithID("zzdtool_status", MMKV.MULTI_PROCESS_MODE) }

    fun init(provider: String?) {
        mmkv.encode(KEY_ACTIVATED, true)
        mmkv.encode(KEY_PROVIDER, provider ?: "Unknown")
    }

    fun isModuleEnabled(): Boolean = mmkv.decodeBool(KEY_ACTIVATED, false)

    fun getHookProviderName(): String = mmkv.decodeString(KEY_PROVIDER, "None") ?: "None"
}
