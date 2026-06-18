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

        // 右气泡资源名 → 左气泡资源名
        private val rightToLeftMap = mapOf(
            "im_chatto_bg_normal_white_mode_v2" to "im_chatfrom_bg_normal_white_mode_v2",
            "im_chatto_bg_pressed_white_mode_v2" to "im_chatfrom_bg_pressed_white_mode_v2",
            "im_chatto_bg_normal_gray_mode_v2" to "im_chatfrom_bg_normal_gray_mode_v2",
            "im_chatto_bg_pressed_gray_mode_v2" to "im_chatfrom_bg_pressed_gray_mode_v2",
            "im_chatto_bg_normal_white_mode" to "im_chatfrom_bg_normal_white_mode",
            "im_chatto_bg_pressed_white_mode" to "im_chatfrom_bg_pressed_white_mode",
            "im_chatto_bg_normal_gray_mode" to "im_chatfrom_bg_normal_gray_mode",
            "im_chatto_bg_pressed_gray_mode" to "im_chatfrom_bg_pressed_gray_mode",
        )
    }

    private var resIdCache = HashMap<Int, Int>()
    private var leftBgNormalId = 0
    private var leftBgPressedId = 0

    override fun onInit(cl: ClassLoader): Boolean {
        if (!isEnabled()) return true

        buildResIdMap(cl)

        // 1. hook awj.M() — 回复消息气泡
        // awj = BaseReplyMsgViewHolder, M() 设置 this.o 的背景
        try {
            val awjClass = XposedHelpers.findClass("taurus.awj", cl)
            XposedHelpers.findAndHookMethod(awjClass, "M",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!isEnabled()) return
                        try {
                            val view = XposedHelpers.getObjectField(param.thisObject, "o") as? View ?: return
                            val ctx = view.context ?: return
                            view.setBackgroundResource(leftBgNormalId)
                        } catch (e: Throwable) {
                            Log.w(TAG, "awj.M hook failed: ${e.message}")
                        }
                    }
                })
            Log.i(TAG, "✔ awj.M() hook 完成")
        } catch (e: Throwable) {
            Log.w(TAG, "awj.M() hook 失败: ${e.message}")
        }

        // 2. hook bez 类 — 语音消息气泡
        // bez = UserVoiceToViewHolder, 继承 bfa → axw → avz
        try {
            val bezClass = XposedHelpers.findClass("taurus.bez", cl)
            XposedHelpers.findAndHookMethod(bezClass, "M",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!isEnabled()) return
                        try {
                            val view = XposedHelpers.getObjectField(param.thisObject, "o") as? View ?: return
                            view.setBackgroundResource(leftBgNormalId)
                        } catch (e: Throwable) {
                            Log.w(TAG, "bez.M hook failed: ${e.message}")
                        }
                    }
                })
            Log.i(TAG, "✔ bez.M() hook 完成")
        } catch (e: Throwable) {
            Log.w(TAG, "bez.M() hook 失败: ${e.message}")
        }

        // 3. hook awv.a(boolean, boolean) — ChatBubbleUtils
        // 强制右气泡走左气泡逻辑
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

    private fun buildResIdMap(cl: ClassLoader) {
        try {
            val resClass = XposedHelpers.findClass("taurus.auy\$e", cl)
            for ((rightName, leftName) in rightToLeftMap) {
                try {
                    val rightId = resClass.getDeclaredField(rightName).getInt(null)
                    val leftId = resClass.getDeclaredField(leftName).getInt(null)
                    if (rightId != 0 && leftId != 0) {
                        resIdCache[rightId] = leftId
                    }
                } catch (_: Throwable) {}
            }
            // 保存左气泡普通/按压态资源 ID
            leftBgNormalId = XposedHelpers.getStaticIntField(resClass, "im_chatfrom_bg_normal_white_mode_v2")
            leftBgPressedId = XposedHelpers.getStaticIntField(resClass, "im_chatfrom_bg_pressed_white_mode_v2")
            if (leftBgNormalId == 0) {
                leftBgNormalId = XposedHelpers.getStaticIntField(resClass, "im_chatfrom_bg_normal_white_mode")
            }
            if (leftBgPressedId == 0) {
                leftBgPressedId = XposedHelpers.getStaticIntField(resClass, "im_chatfrom_bg_pressed_white_mode")
            }
            Log.i(TAG, "深色模式: leftBgNormal=0x${Integer.toHexString(leftBgNormalId)}, leftBgPressed=0x${Integer.toHexString(leftBgPressedId)}")
        } catch (e: Throwable) {
            Log.e(TAG, "构建资源映射失败: ${e.message}")
        }
    }
}
