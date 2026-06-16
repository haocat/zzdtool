package com.zzd.tool.hook

import android.util.Log
import com.zzd.tool.hook.core.DexResolver
import com.zzd.tool.hook.core.HookStatus
import com.tencent.mmkv.MMKV
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookEntry : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "ZddTool"
        private const val TARGET = "com.alibaba.taurus.zhejiang"
    }

    private val hooks = listOf(
        TabletModeHook(),
        AntiRecallHook(),
        ForwardUnlockHook(),
        MessageBadgeHook()
    )

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET) return
        val cl = lpparam.classLoader
        Log.i(TAG, "ZZD已加载")

        MMKV.initialize(lpparam.appInfo.dataDir)
        HookStatus.init(provider = detectHookProvider())
        Log.i(TAG, "Hook框架: ${HookStatus.getHookProviderName()}")

        DexResolver.init(lpparam.appInfo.sourceDir, cl,
            cacheDir = "${lpparam.appInfo.dataDir}/files")

        hooks.forEach { it.init(cl) }

        DexResolver.release()
        Log.i(TAG, "全部Hook完成")
    }

    private fun detectHookProvider(): String {
        try {
            val xposedClass = Class.forName("de.robv.android.xposed.XposedBridge")
            val tagField = xposedClass.getDeclaredField("TAG")
            val tag = tagField.get(null) as? String
            return when {
                tag?.startsWith("LSPosed") == true -> "LSPosed"
                tag?.startsWith("EdXposed") == true -> "EdXposed"
                tag?.startsWith("PineXposed") == true -> "Dreamland"
                else -> "Legacy Xposed"
            }
        } catch (_: Throwable) {
        }
        return "Unknown"
    }
}
