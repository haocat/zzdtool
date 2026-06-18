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

        // 获取左气泡资源 ID（从 hqd$e，和 gwf 用的同一套）
        try {
            val hqdE = XposedHelpers.findClass("taurus.hqd\$e", cl)
            leftBgNormalId = XposedHelpers.getStaticIntField(hqdE, "im_chatfrom_bg_normal")
            leftBgPressedId = XposedHelpers.getStaticIntField(hqdE, "im_chatfrom_bg_pressed")
            Log.i(TAG, "左气泡资源: normal=0x${Integer.toHexString(leftBgNormalId)}, pressed=0x${Integer.toHexString(leftBgPressedId)}")
        } catch (e: Throwable) {
            Log.e(TAG, "获取左气泡资源失败: ${e.message}")
            return false
        }

        if (leftBgNormalId == 0 || leftBgPressedId == 0) {
            Log.e(TAG, "左气泡资源 ID 为 0")
            return false
        }

        // 1. hook gwf.M() — 文本消息按压态
        hookViewHolderM(cl, "taurus.gwf", "ChatToTextMessageViewHolder")

        // 2. hook awj.M() — 回复消息（BaseReplyMsgViewHolder）
        hookViewHolderM(cl, "taurus.awj", "BaseReplyMsgViewHolder")

        // 3. hook bez — 语音消息（UserVoiceToViewHolder）
        // bez 没有自己的 M()，需要 hook 父类
        hookViewHolderM(cl, "taurus.bfa", "UserVoiceToViewHolder父类")

        // 4. hook awv.a() — ChatBubbleUtils 兜底
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

    private fun hookViewHolderM(cl: ClassLoader, className: String, desc: String) {
        try {
            val clazz = XposedHelpers.findClass(className, cl)
            XposedHelpers.findAndHookMethod(clazz, "M",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!isEnabled()) return
                        try {
                            // awj 把背景 View 存在字段 "o" 里
                            val view = XposedHelpers.getObjectField(param.thisObject, "o") as? View
                            if (view != null) {
                                view.setBackgroundResource(leftBgNormalId)
                            }
                        } catch (e: Throwable) {
                            Log.w(TAG, "$desc.M hook failed: ${e.message}")
                        }
                    }
                })
            Log.i(TAG, "✔ $desc.M() hook 完成")
        } catch (e: Throwable) {
            Log.w(TAG, "$desc.M() hook 失败: ${e.message}")
        }
    }
}
