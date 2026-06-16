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

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET) return
        val cl = lpparam.classLoader
        Log.i(TAG, "ZZD已加载")

        // 写入激活状态
        writeActivationStatus(lpparam)

        DexResolver.init(lpparam.appInfo.sourceDir, cl,
            cacheDir = "${lpparam.appInfo.dataDir}/files")

        // 设置入口 hook（不依赖 settings）
        SettingEntryHook().init(cl)

        // 其余 hook 需要 settings，延迟到 Application.onCreate 后注册
        val functionalHooks = listOf(
            TabletModeHook(),
            AntiRecallHook(),
            ForwardUnlockHook(),
            MessageBadgeHook()
        )

        try {
            val appClass = XposedHelpers.findClass("android.app.Application", null)
            XposedHelpers.findAndHookMethod(appClass, "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val app = param.thisObject as? Application ?: return
                        SettingsManager.initFromContentResolver(app)
                        Log.i(TAG, "Hook框架: ${detectHookProvider()}")
                        functionalHooks.forEach { it.init(cl) }
                        Log.i(TAG, "全部Hook完成")
                    }
                })
        } catch (_: Throwable) {}

        DexResolver.release()
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

    private fun writeActivationStatus(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val provider = detectHookProvider()
            val appClass = Class.forName("android.app.ActivityThread")
            val currentApp = appClass.getDeclaredMethod("currentApplication").invoke(null)
            val ctx = currentApp as? android.content.Context ?: return
            val uri = android.net.Uri.parse("content://com.zzd.tool.settings/settings")
            ctx.contentResolver.call(uri, "activate", provider, null)
            Log.i(TAG, "激活状态已写入: $provider")
        } catch (e: Throwable) {
            Log.w(TAG, "写入激活状态失败: ${e.message}")
        }
    }
}
