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

        DexResolver.init(lpparam.appInfo.sourceDir, cl,
            cacheDir = "${lpparam.appInfo.dataDir}/files")

        SettingEntryHook().init(cl)

        val functionalHooks = listOf(
            TabletModeHook(),
            AntiRecallHook(),
            ForwardUnlockHook(),
            MessageBadgeHook()
        )

        val provider = detectHookProvider()
        Log.i(TAG, "Hook框架: $provider")

        try {
            val appClass = XposedHelpers.findClass("android.app.Application", null)
            XposedHelpers.findAndHookMethod(appClass, "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val app = param.thisObject as? Application ?: return
                        writeActivationStatus(app, provider)
                        SettingsManager.initFromContentResolver(app)
                        functionalHooks.forEach { it.init(cl) }
                        Log.i(TAG, "全部Hook完成")
                    }
                })
        } catch (_: Throwable) {}

        DexResolver.release()
    }

    private fun detectHookProvider(): String {
        // 方法1: 检查 LSPosed 专属类
        try {
            ClassLoader.getSystemClassLoader().loadClass("org.lsposed.lspd.service.LSPosedService")
            return "LSPosed"
        } catch (_: Throwable) {}

        // 方法2: 检查 EdXposed 专属类
        try {
            ClassLoader.getSystemClassLoader().loadClass("com.swiftshader.edxposed.EdXposed")
            return "EdXposed"
        } catch (_: Throwable) {}

        // 方法3: 检查 TAG 字段
        try {
            val xposedClass = ClassLoader.getSystemClassLoader()
                .loadClass("de.robv.android.xposed.XposedBridge")
            val tag = xposedClass.getDeclaredField("TAG").get(null) as? String
            if (tag != null) {
                return when {
                    tag.contains("LSPosed") -> "LSPosed"
                    tag.contains("EdXposed") -> "EdXposed"
                    tag.contains("Pine") -> "Dreamland"
                    tag.contains("Xposed") -> "Xposed"
                    else -> tag
                }
            }
        } catch (_: Throwable) {}

        // 方法4: 检查类名是否被混淆（LSPosed 会混淆类名）
        try {
            ClassLoader.getSystemClassLoader().loadClass("de.robv.android.xposed.XposedBridge")
            return "Xposed"
        } catch (_: Throwable) {}

        return "Unknown"
    }

    private fun writeActivationStatus(app: Application, provider: String) {
        try {
            val uri = android.net.Uri.parse("content://com.zzd.tool.settings/settings")
            app.contentResolver.call(uri, "activate", provider, null)
            Log.i(TAG, "激活状态已写入: $provider")
        } catch (e: Throwable) {
            Log.w(TAG, "写入激活状态失败: ${e.message}")
        }
    }
}
