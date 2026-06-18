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

    private var leftBgNormalId = 0
    private var leftBgPressedId = 0

    override fun onInit(cl: ClassLoader): Boolean {
        if (!isEnabled()) return true

        try {
            val hqdE = XposedHelpers.findClass("taurus.hqd\$e", cl)
            leftBgNormalId = XposedHelpers.getStaticIntField(hqdE, "im_chatfrom_bg_normal")
            leftBgPressedId = XposedHelpers.getStaticIntField(hqdE, "im_chatfrom_bg_pressed")
            Log.i(TAG, "左气泡: normal=0x${Integer.toHexString(leftBgNormalId)}, pressed=0x${Integer.toHexString(leftBgPressedId)}")
        } catch (e: Throwable) {
            Log.e(TAG, "获取左气泡资源失败: ${e.message}")
            return false
        }

        // 1. gwf (ChatToTextMessageViewHolder) — 文本消息
        hookViewHolder(cl, "taurus.gwf", "P", "ab", leftBgNormalId, "文本常态")
        hookViewHolder(cl, "taurus.gwf", "M", "ab", leftBgPressedId, "文本按压")

        // 2. awj (BaseReplyMsgViewHolder) — 回复消息（部分）
        hookViewHolder(cl, "taurus.awj", "M", "o", leftBgNormalId, "回复消息-awj")

        // 2b. gvt (ChatReplyMsgViewHolder) — 回复消息（主要）
        hookViewHolder(cl, "taurus.gvt", "M", "o", leftBgNormalId, "回复消息-gvt")

        // 3. gue (ChatToAudioMessageViewHolder) — 语音消息
        //    通过 findViewById 找 voice_play_view_container
        hookViewHolderFindByResId(cl, "taurus.gue", "K", "voice_play_view_container", leftBgPressedId, "语音按压")
        hookViewHolderFindByResId(cl, "taurus.gue", "M", "voice_play_view_container", leftBgNormalId, "语音常态")

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

    private fun hookViewHolder(cl: ClassLoader, className: String, methodName: String,
                                fieldName: String, resId: Int, desc: String) {
        try {
            val clazz = XposedHelpers.findClass(className, cl)
            XposedHelpers.findAndHookMethod(clazz, methodName,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!isEnabled()) return
                        try {
                            val view = XposedHelpers.getObjectField(param.thisObject, fieldName) as? View
                            if (view != null) {
                                view.setBackgroundResource(resId)
                                Log.d(TAG, "$desc: setResource(0x${Integer.toHexString(resId)})")
                            } else {
                                Log.w(TAG, "$desc: view field '$fieldName' is null")
                            }
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

    private fun hookViewHolderFindByResId(cl: ClassLoader, className: String, methodName: String,
                                            resIdName: String, resId: Int, desc: String) {
        try {
            val clazz = XposedHelpers.findClass(className, cl)
            val resIdField = XposedHelpers.findClass("taurus.hqd\$f", cl)
            val containerResId = XposedHelpers.getStaticIntField(resIdField, resIdName)

            XposedHelpers.findAndHookMethod(clazz, methodName,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!isEnabled()) return
                        try {
                            val itemView = XposedHelpers.getObjectField(param.thisObject, "o") as? View ?: return
                            val container = itemView.findViewById<View>(containerResId)
                            if (container != null) {
                                container.setBackgroundResource(resId)
                                Log.d(TAG, "$desc: setResource(0x${Integer.toHexString(resId)})")
                            }
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
}
