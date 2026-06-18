package com.zzd.tool.hook

import android.util.Log
import com.zzd.tool.hook.core.BaseHook
import com.zzd.tool.hook.core.HookFeature
import com.zzd.tool.hook.core.SettingsManager
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class DarkModeFixHook : BaseHook(HookFeature.DARK_MODE) {

    companion object {
        private const val TAG = "ZddTool"
    }

    override fun onInit(cl: ClassLoader): Boolean {
        if (!isEnabled()) return true

        // awv = ChatBubbleUtils
        // a(boolean isRightBubble, boolean isPressed) → 返回气泡背景资源 ID
        // 强制让右气泡也走左气泡逻辑
        val bubbleUtils = XposedHelpers.findClass("taurus.awv", cl)
        XposedHelpers.findAndHookMethod(
            bubbleUtils, "a",
            Boolean::class.java, Boolean::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!isEnabled()) return
                    // 第一个参数: true=右气泡, false=左气泡
                    // 强制设为 false，让右气泡使用左气泡的背景
                    param.args[0] = false
                }
            }
        )
        Log.i(TAG, "✔ ColorOS深色模式修复: ChatBubbleUtils.a() hook 完成")
        return true
    }
}
