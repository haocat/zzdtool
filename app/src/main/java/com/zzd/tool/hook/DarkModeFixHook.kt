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
    private var rightResIds = HashSet<Int>()
    private var leftResIds = HashMap<Int, Int>() // rightId → leftId (same as resIdCache)

    override fun onInit(cl: ClassLoader): Boolean {
        if (!isEnabled()) return true

        buildResIdMap(cl)

        // hook View.setBackgroundResource(int)
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

        // hook View.getBackground() — 拦截 Drawable 捕获
        // 当 awj 捕获背景时，如果是右气泡资源，替换为左气泡
        XposedHelpers.findAndHookMethod(
            View::class.java, "getBackground",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!isEnabled()) return
                    val result = param.result ?: return
                    val view = param.thisObject as? View ?: return
                    val ctx = view.context ?: return

                    // 尝试从 constantState 获取资源 ID
                    try {
                        val cs = result.constantState ?: return
                        val fieldName = cs.javaClass.simpleName
                        // 如果是右气泡资源，替换为左气泡
                        for ((rightId, leftId) in resIdCache) {
                            try {
                                val rightDrawable = ctx.getDrawable(rightId) ?: continue
                                if (rightDrawable.constantState?.javaClass?.name == cs.javaClass?.name) {
                                    // 可能匹配，尝试用资源名验证
                                    val resEntryName = ctx.resources.getResourceEntryName(rightId)
                                    if (fieldName.contains(resEntryName.substringAfter("_bg_", ""))) {
                                        param.result = ctx.getDrawable(leftId)
                                        return
                                    }
                                }
                            } catch (_: Throwable) {}
                        }
                    } catch (_: Throwable) {}
                }
            }
        )

        Log.i(TAG, "✔ ColorOS深色模式修复: hook 完成, 映射 ${resIdCache.size} 对")
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
                        rightResIds.add(rightId)
                    }
                } catch (_: Throwable) {}
            }
            Log.i(TAG, "深色模式资源映射: ${resIdCache.size} 对")
        } catch (e: Throwable) {
            Log.e(TAG, "构建资源映射失败: ${e.message}")
        }
    }
}
