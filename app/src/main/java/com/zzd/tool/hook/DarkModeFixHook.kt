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

        // 右气泡资源 → 左气泡资源映射
        private val rightToLeftMap = mapOf(
            // v2 系列
            "im_chatto_bg_normal_white_mode_v2" to "im_chatfrom_bg_normal_white_mode_v2",
            "im_chatto_bg_pressed_white_mode_v2" to "im_chatfrom_bg_pressed_white_mode_v2",
            "im_chatto_bg_normal_gray_mode_v2" to "im_chatfrom_bg_normal_gray_mode_v2",
            "im_chatto_bg_pressed_gray_mode_v2" to "im_chatfrom_bg_pressed_gray_mode_v2",
            // 非 v2 系列
            "im_chatto_bg_normal_white_mode" to "im_chatfrom_bg_normal_white_mode",
            "im_chatto_bg_pressed_white_mode" to "im_chatfrom_bg_pressed_white_mode",
            "im_chatto_bg_normal_gray_mode" to "im_chatfrom_bg_normal_gray_mode",
            "im_chatto_bg_pressed_gray_mode" to "im_chatfrom_bg_pressed_gray_mode",
        )
    }

    private var resIdCache = HashMap<Int, Int>() // 右气泡resId → 左气泡resId

    override fun onInit(cl: ClassLoader): Boolean {
        if (!isEnabled()) return true

        // 构建 resId 映射
        buildResIdMap(cl)

        // hook View.setBackgroundResource 拦截所有背景设置
        XposedHelpers.findAndHookMethod(
            View::class.java, "setBackgroundResource", Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!isEnabled()) return
                    val resId = param.args[0] as? Int ?: return
                    val mapped = resIdCache[resId]
                    if (mapped != null) {
                        param.args[0] = mapped
                    }
                }
            }
        )
        Log.i(TAG, "✔ ColorOS深色模式修复: View.setBackgroundResource hook 完成, 映射 ${resIdCache.size} 个资源")
        return true
    }

    private fun buildResIdMap(cl: ClassLoader) {
        try {
            val resClass = XposedHelpers.findClass("taurus.auy\$e", cl)
            for ((rightName, leftName) in rightToLeftMap) {
                try {
                    val rightField = resClass.getDeclaredField(rightName)
                    val leftField = resClass.getDeclaredField(leftName)
                    val rightId = rightField.getInt(null)
                    val leftId = leftField.getInt(null)
                    if (rightId != 0 && leftId != 0) {
                        resIdCache[rightId] = leftId
                    }
                } catch (_: Throwable) {}
            }
            Log.i(TAG, "深色模式资源映射: ${resIdCache.size} 对")
        } catch (e: Throwable) {
            Log.e(TAG, "构建资源映射失败: ${e.message}")
        }
    }
}
