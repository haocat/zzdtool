package com.zzd.tool.hook

import android.app.Application
import android.util.Log
import com.tencent.mmkv.MMKV
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

    private var apkPath: String? = null
    private var hostCl: ClassLoader? = null

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET) return
        hostCl = lpparam.classLoader
        apkPath = lpparam.appInfo.sourceDir
        Log.i(TAG, "ZZD已加载")

        SettingEntryHook().init(hostCl!!)

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
                        if (app.packageName != TARGET) return

                        MMKV.initialize(app)

                        DexResolver.init(apkPath!!, hostCl!!)
                        SettingsManager.initFromContentResolver(app)

                        writeActivationStatus(app, provider)

                        functionalHooks.forEach { it.init(hostCl!!) }
                        DexResolver.release()
                        Log.i(TAG, "全部Hook完成")
                    }
                })
        } catch (_: Throwable) {}
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

    private fun detectHookProvider(): String {
        try {
            ClassLoader.getSystemClassLoader().loadClass("org.lsposed.lspd.service.LSPosedService")
            return "LSPosed"
        } catch (_: Throwable) {}

        try {
            ClassLoader.getSystemClassLoader().loadClass("com.swiftshader.edxposed.EdXposed")
            return "EdXposed"
        } catch (_: Throwable) {}

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

        try {
            ClassLoader.getSystemClassLoader().loadClass("de.robv.android.xposed.XposedBridge")
            return "Xposed"
        } catch (_: Throwable) {}

        return "Unknown"
    }
}
