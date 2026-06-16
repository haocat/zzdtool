package com.zzd.tool.hook

import android.app.Application
import android.os.Bundle
import android.util.Log
import com.zzd.tool.hook.core.DexResolver
import com.zzd.tool.hook.core.SettingsManager
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
        SettingEntryHook(),
        TabletModeHook(),
        AntiRecallHook(),
        ForwardUnlockHook(),
        MessageBadgeHook()
    )

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET) return
        val cl = lpparam.classLoader
        Log.i(TAG, "ZZD已加载")

        try {
            val appClass = XposedHelpers.findClass("android.app.Application", null)
            XposedHelpers.findAndHookMethod(appClass, "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val app = param.thisObject as? Application ?: return
                        SettingsManager.initFromContentResolver(app)
                        Log.i(TAG, "Hook框架: ${detectHookProvider()}")
                    }
                })
        } catch (_: Throwable) {}

        DexResolver.init(lpparam.appInfo.sourceDir, cl,
            cacheDir = "${lpparam.appInfo.dataDir}/files")

        hooks.forEach { it.init(cl) }

        DexResolver.release()
        Log.i(TAG, "全部Hook完成")
    }

    private fun detectHookProvider(): String {
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
        } catch (_: Throwable) { "None" }
    }
}
