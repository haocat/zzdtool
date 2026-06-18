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
        //    字段 "ab" (LinearLayout) 来自父类 gwe
        hookViewHolder(cl, "taurus.gwf", "P", "ab", leftBgNormalId, "文本常态")
        hookViewHolder(cl, "taurus.gwf", "M", "ab", leftBgPressedId, "文本按压")

        // 2. awj (BaseReplyMsgViewHolder) — 回复消息
        //    字段 "o" (View)
        hookViewHolder(cl, "taurus.awj", "M", "o", leftBgNormalId, "回复消息")

        // 3. bez (UserVoiceToViewHolder) — 语音消息
        //    字段 "ab" (View) 来自父类 bfa
        //    方法名是 K() 和 L()，不是 P() 和 M()
        hookViewHolder(cl, "taurus.bez", "K", "ab", leftBgPressedId, "语音按压")
        hookViewHolder(cl, "taurus.bez", "L", "ab", leftBgNormalId, "语音常态")

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
}
