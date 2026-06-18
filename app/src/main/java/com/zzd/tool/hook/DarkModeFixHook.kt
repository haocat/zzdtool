package com.zzd.tool.hook

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
        //    字段 "ab" (LinearLayout)
        hookViewHolderField(cl, "taurus.gwf", "P", "ab", false, "文本常态")
        hookViewHolderField(cl, "taurus.gwf", "M", "ab", true, "文本按压")

        // 2. gvt (ChatReplyMsgViewHolder) — 回复消息
        //    字段 "Z" (View) + "ap" (GradientDrawable)
        hookViewHolderDrawable(cl, "taurus.gvt", "M", "Z", "ap", "回复消息-gvt")

        // 2b. gvu (ChatToReplyMsgViewHolder) — 回复消息（右气泡）
        //    继承 gvt，同样用 "Z" 和 "ap"
        hookViewHolderDrawable(cl, "taurus.gvu", "M", "Z", "ap", "回复消息-gvu")

        // 3. gue (ChatToAudioMessageViewHolder) — 语音消息
        //    通过 findViewById 找 voice_play_view_container
        hookViewHolderFindViewById(cl, "taurus.gue", "K", "voice_play_view_container", true, "语音按压")
        hookViewHolderFindViewById(cl, "taurus.gue", "M", "voice_play_view_container", false, "语音常态")

        // 4. awv.a() — ChatBubbleUtils 兜底
        try {
            val bubbleUtils = XposedHelpers.findClass("taurus.awv", cl)
            XposedHelpers.findAndHookMethod(bubbleUtils, "a",
                Boolean::class.java, Boolean::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (!isEnabled()) return
                        param.args[0] = false
                    }
                })
            Log.i(TAG, "✔ awv.a() hook 完成")
        } catch (e: Throwable) {
            Log.w(TAG, "awv.a() hook 失败: ${e.message}")
        }

        Log.i(TAG, "✔ ColorOS深色模式修复: 完成")
        return true
    }

    // 直接设置字段的 View 背景（gwf 用）
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
                            val ctx = view.context ?: return
                            // 调用 awv.a(false, pressed) 获取正确的左气泡资源
                            val resId = XposedHelpers.callStaticMethod(bubbleUtils, "a", false, isPressed) as Int
                            view.setBackgroundResource(resId)
                            Log.d(TAG, "$desc: setResource(0x${Integer.toHexString(resId)})")
                        } catch (e: Throwable) {
                            Log.w(TAG, "$desc failed: ${e.message}")
                        }
                    }
                })
            Log.i(TAG, "✔ $className.$methodName() [$fieldName] hook 完成")
        } catch (e: Throwable) {
            Log.w(TAG, "$className.$methodName() hook 失败: ${e.message}")
        }
    }

    // 通过 findViewById 找容器并设置背景（gue 用）
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
                            val ctx = container.context ?: return
                            val resId = XposedHelpers.callStaticMethod(bubbleUtils, "a", false, isPressed) as Int
                            container.setBackgroundResource(resId)
                            Log.d(TAG, "$desc: setResource(0x${Integer.toHexString(resId)})")
                        } catch (e: Throwable) {
                            Log.w(TAG, "$desc failed: ${e.message}")
                        }
                    }
                })
            Log.i(TAG, "✔ $className.$methodName() [$resIdName] hook 完成")
        } catch (e: Throwable) {
            Log.w(TAG, "$className.$methodName() hook 失败: ${e.message}")
        }
    }

    // 替换 Drawable 字段（gvt/gvu 用）
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
                            val drawable = ctx.getDrawable(resId) ?: return
                            XposedHelpers.setObjectField(param.thisObject, drawableField, drawable)
                        } catch (e: Throwable) {
                            Log.w(TAG, "$desc failed: ${e.message}")
                        }
                    }
                })
            Log.i(TAG, "✔ $className.$methodName() [$drawableField] hook 完成")
        } catch (e: Throwable) {
            Log.w(TAG, "$className.$methodName() hook 失败: ${e.message}")
        }
    }
}
