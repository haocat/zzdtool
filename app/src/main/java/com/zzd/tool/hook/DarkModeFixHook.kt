package com.zzd.tool.hook

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import com.zzd.tool.hook.core.BaseHook
import com.zzd.tool.hook.core.HookFeature
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class DarkModeFixHook : BaseHook(HookFeature.DARK_MODE) {

    companion object {
        private const val TAG = "ZddTool"
    }

    override fun onInit(cl: ClassLoader): Boolean {
        if (!isEnabled()) return true

        Log.i(TAG, "=== 深色模式调试：开始监听所有背景设置 ===")

        // 监听所有 View 的 setBackgroundResource 调用
        XposedHelpers.findAndHookMethod(
            View::class.java, "setBackgroundResource", Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!isEnabled()) return
                    val resId = param.args[0] as? Int ?: return
                    val view = param.thisObject as? View ?: return
                    val ctx = view.context ?: return
                    val pkg = ctx.packageName ?: return
                    if (pkg != "com.alibaba.taurus.zhejiang") return

                    // 只打印右气泡相关的资源
                    val resName = try { ctx.resources.getResourceEntryName(resId) } catch (_: Throwable) { "?" }
                    if (resName.contains("chatto") || resName.contains("chatfrom")) {
                        val stackTrace = Throwable().stackTrace.take(6).joinToString("\n") { "    ${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" }
                        Log.d(TAG, "setBackgroundResource: $resName(0x${Integer.toHexString(resId)}) view=${view.javaClass.simpleName}\n$stackTrace")
                    }
                }
            })

        // 监听所有 View 的 setBackground 调用
        XposedHelpers.findAndHookMethod(
            View::class.java, "setBackground", Drawable::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!isEnabled()) return
                    val drawable = param.args[0] as? Drawable ?: return
                    val view = param.thisObject as? View ?: return
                    val ctx = view.context ?: return
                    val pkg = ctx.packageName ?: return
                    if (pkg != "com.alibaba.taurus.zhejiang") return

                    val drawableName = drawable.javaClass.name
                    if (drawableName.contains("GradientDrawable") || drawableName.contains("ColorDrawable")) {
                        val stackTrace = Throwable().stackTrace.take(6).joinToString("\n") { "    ${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" }
                        Log.d(TAG, "setBackground: ${drawableName.substringAfterLast('.')} view=${view.javaClass.simpleName}\n$stackTrace")
                    }
                }
            })

        // 监听 gue 的 K() 和 M()
        hookMethod(cl, "taurus.gue", "K", "语音K(常态)")
        hookMethod(cl, "taurus.gue", "M", "语音M(按压)")

        // 监听 gvt/gvu 的 M()
        hookMethod(cl, "taurus.gvt", "M", "回复gvt.M")
        hookMethod(cl, "taurus.gvu", "M", "回复gvu.M")

        // 监听 gwf 的 P() 和 M()
        hookMethod(cl, "taurus.gwf", "P", "文本gwf.P")
        hookMethod(cl, "taurus.gwf", "M", "文本gwf.M")

        Log.i(TAG, "=== 深色模式调试：监听完成，等待触发 ===")
        return true
    }

    private fun hookMethod(cl: ClassLoader, className: String, methodName: String, desc: String) {
        try {
            val clazz = XposedHelpers.findClass(className, cl)
            XposedHelpers.findAndHookMethod(clazz, methodName,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        Log.d(TAG, ">>> $desc 被调用")
                    }
                    override fun afterHookedMethod(param: MethodHookParam) {
                        Log.d(TAG, "<<< $desc 完成")
                    }
                })
            Log.i(TAG, "✔ $desc hook 完成")
        } catch (e: Throwable) {
            Log.w(TAG, "$desc hook 失败: ${e.message}")
        }
    }
}
