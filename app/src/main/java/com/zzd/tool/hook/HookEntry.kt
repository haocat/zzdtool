package com.zzd.tool.hook

import android.app.Application
import android.os.Bundle
import android.util.Log
import com.zzd.tool.hook.core.DexResolver
import com.zzd.tool.hook.core.HookStatus
import com.tencent.mmkv.MMKV
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
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

        HookStatus.init(isZygote = true, provider = detectHookProvider())
        Log.i(TAG, "Hook框架: ${HookStatus.getHookProviderName()}")

        try {
            val appClass = XposedHelpers.findClass("android.app.Application", null)
            XposedHelpers.findAndHookMethod(appClass, "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val app = param.thisObject as? Application ?: return
                        MMKV.initialize(app)
                    }
                })
        } catch (_: Throwable) {
        }

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
