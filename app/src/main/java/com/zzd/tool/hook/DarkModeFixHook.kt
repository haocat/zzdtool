package com.zzd.tool.hook

import android.app.Activity
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

        // 1. gwf (ChatToTextMessageViewHolder) — 文本消息
        hookViewHolderField(cl, "taurus.gwf", "P", "ab", false, "文本常态")
        hookViewHolderField(cl, "taurus.gwf", "M", "ab", true, "文本按压")

        // 2. gvu (ChatToReplyMsgViewHolder) — 回复消息
        hookViewHolderDrawable(cl, "taurus.gvu", "M", "Z", "ap", "回复消息")

        // 3. gue (ChatToAudioMessageViewHolder) — 语音消息
        hookViewHolderFindViewById(cl, "taurus.gue", "K", "voice_play_view_container", false, "语音常态")
        hookViewHolderFindViewById(cl, "taurus.gue", "M", "voice_play_view_container", true, "语音按压")

        // 4. View.setBackgroundResource — 拦截所有背景设置
        XposedHelpers.findAndHookMethod(
            View::class.java, "setBackgroundResource", Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!isEnabled()) return
                    val resId = param.args[0] as? Int ?: return
                    val view = param.thisObject as? View ?: return
                    val ctx = view.context ?: return
                    if (ctx.packageName != "com.alibaba.taurus.zhejiang") return

                    val resName = try { ctx.resources.getResourceEntryName(resId) } catch (_: Throwable) { "?" }
                    if (resName.startsWith("im_chatto_bg_")) {
                        try {
                            val bubbleUtils = XposedHelpers.findClass("taurus.awv", ctx.classLoader)
                            val isPressed = resName.contains("pressed")
                            val leftResId = XposedHelpers.callStaticMethod(bubbleUtils, "a", false, isPressed) as Int
                            param.args[0] = leftResId
                            Log.d(TAG, "替换: $resName → 0x${Integer.toHexString(leftResId)}")
                        } catch (_: Throwable) {}
                    }
                }
            })

        // 5. gvu (ChatToReplyMsgViewHolder) — 初始背景修复 (b() 方法用 chatto_bg 天蓝色)
        try {
            val gvuClass = XposedHelpers.findClass("taurus.gvu", cl)
            val bubbleUtils = XposedHelpers.findClass("taurus.awv", cl)
            XposedHelpers.findAndHookMethod(gvuClass, "b",
                Activity::class.java,
                XposedHelpers.findClass("com.alibaba.wukong.im.Message", cl),
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!isEnabled()) return
                        try {
                            val view = XposedHelpers.getObjectField(param.thisObject, "Z") as? View ?: return
                            val resId = XposedHelpers.callStaticMethod(bubbleUtils, "a", false, false) as Int
                            view.setBackgroundResource(resId)
                        } catch (e: Throwable) { Log.w(TAG, "回复初始: ${e.message}") }
                    }
                })
            Log.i(TAG, "✔ 回复消息初始背景 hook 完成")
        } catch (e: Throwable) { Log.w(TAG, "回复消息初始背景 hook 失败: ${e.message}") }

        // 6. gue (ChatToAudioMessageViewHolder) — 初始背景修复 (XML 布局中的初始背景)
        try {
            val gucClass = XposedHelpers.findClass("taurus.guc", cl)
            val bubbleUtils = XposedHelpers.findClass("taurus.awv", cl)
            XposedHelpers.findAndHookMethod(gucClass, "a",
                Activity::class.java,
                View::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!isEnabled()) return
                        if (param.thisObject.javaClass.name != "taurus.gue") return
                        try {
                            val container = XposedHelpers.getObjectField(param.thisObject, "ae") as? View ?: return
                            val resId = XposedHelpers.callStaticMethod(bubbleUtils, "a", false, false) as Int
                            container.setBackgroundResource(resId)
                        } catch (e: Throwable) { Log.w(TAG, "语音初始: ${e.message}") }
                    }
                })
            Log.i(TAG, "✔ 语音消息初始背景 hook 完成")
        } catch (e: Throwable) { Log.w(TAG, "语音消息初始背景 hook 失败: ${e.message}") }

        Log.i(TAG, "✔ ColorOS深色模式修复完成")
        return true
    }

    private fun hookViewHolderField(cl: ClassLoader, className: String, methodName: String,
                                     fieldName: String, isPressed: Boolean, desc: String) {
        try {
            val clazz = XposedHelpers.findClass(className, cl)
            val bubbleUtils = XposedHelpers.findClass("taurus.awv", cl)
            XposedHelpers.findAndHookMethod(clazz, methodName,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!isEnabled()) return
                        try {
                            val view = XposedHelpers.getObjectField(param.thisObject, fieldName) as? View ?: return
                            val resId = XposedHelpers.callStaticMethod(bubbleUtils, "a", false, isPressed) as Int
                            view.setBackgroundResource(resId)
                        } catch (e: Throwable) { Log.w(TAG, "$desc: ${e.message}") }
                    }
                })
            Log.i(TAG, "✔ $desc hook 完成")
        } catch (e: Throwable) { Log.w(TAG, "$desc hook 失败: ${e.message}") }
    }

    private fun hookViewHolderDrawable(cl: ClassLoader, className: String, methodName: String,
                                        viewField: String, drawableField: String, desc: String) {
        try {
            val clazz = XposedHelpers.findClass(className, cl)
            val bubbleUtils = XposedHelpers.findClass("taurus.awv", cl)
            XposedHelpers.findAndHookMethod(clazz, methodName,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (!isEnabled()) return
                        try {
                            val view = XposedHelpers.getObjectField(param.thisObject, viewField) as? View ?: return
                            val ctx = view.context ?: return
                            val resId = XposedHelpers.callStaticMethod(bubbleUtils, "a", false, false) as Int
                            XposedHelpers.setObjectField(param.thisObject, drawableField, ctx.getDrawable(resId))
                        } catch (e: Throwable) { Log.w(TAG, "$desc: ${e.message}") }
                    }
                })
            Log.i(TAG, "✔ $desc hook 完成")
        } catch (e: Throwable) { Log.w(TAG, "$desc hook 失败: ${e.message}") }
    }

    private fun hookViewHolderFindViewById(cl: ClassLoader, className: String, methodName: String,
                                            resIdName: String, isPressed: Boolean, desc: String) {
        try {
            val clazz = XposedHelpers.findClass(className, cl)
            val resIdField = XposedHelpers.findClass("taurus.hqd\$f", cl)
            val containerResId = XposedHelpers.getStaticIntField(resIdField, resIdName)
            val bubbleUtils = XposedHelpers.findClass("taurus.awv", cl)
            XposedHelpers.findAndHookMethod(clazz, methodName,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!isEnabled()) return
                        try {
                            val itemView = XposedHelpers.getObjectField(param.thisObject, "o") as? View ?: return
                            val container = itemView.findViewById<View>(containerResId) ?: return
                            val resId = XposedHelpers.callStaticMethod(bubbleUtils, "a", false, isPressed) as Int
                            container.setBackgroundResource(resId)
                        } catch (e: Throwable) { Log.w(TAG, "$desc: ${e.message}") }
                    }
                })
            Log.i(TAG, "✔ $desc hook 完成")
        } catch (e: Throwable) { Log.w(TAG, "$desc hook 失败: ${e.message}") }
    }
}
