package com.zzd.tool.hook

import android.util.Log
import com.zzd.tool.hook.core.BaseHook
import com.zzd.tool.hook.core.DexResolver
import com.zzd.tool.hook.core.HookFeature
import com.zzd.tool.hook.core.HookUtils
import de.robv.android.xposed.XposedHelpers

class ForwardUnlockHook : BaseHook(HookFeature.FORWARD) {

    companion object {
        private const val TAG = "ZddTool"
    }

    override fun onInit(cl: ClassLoader): Boolean {
        val msgClass = XposedHelpers.findClass("com.alibaba.wukong.im.Message", cl)

        val forwardClass = DexResolver.findClassByStrings("isForwardMsg", "supportForward")
        if (forwardClass == null) {
            Log.e(TAG, "✘ 转发: DexKit 找不到含 isForwardMsg 的类")
            return false
        }
        Log.i(TAG, "✔ 转发类: ${forwardClass.name}")

        val targetMethod = findForwardMethod(forwardClass, msgClass)
        if (targetMethod == null) {
            Log.e(TAG, "✘ 转发: 找不到 (Message)→boolean 方法")
            return false
        }

        HookUtils.hookBeforeIfEnabled(this, forwardClass, targetMethod.name, msgClass) { param ->
            param.result = true
        }

        Log.i(TAG, "✔ ${forwardClass.name}.${targetMethod.name}")
        return true
    }

    private fun findForwardMethod(forwardClass: Class<*>, msgClass: Class<*>): java.lang.reflect.Method? {
        for (m in forwardClass.declaredMethods) {
            if (m.returnType == java.lang.Boolean.TYPE && m.parameterTypes.size == 1
                && m.parameterTypes[0] == msgClass && m.name == "i"
            ) {
                return m
            }
        }
        for (m in forwardClass.declaredMethods) {
            if (m.returnType == java.lang.Boolean.TYPE && m.parameterTypes.size == 1
                && m.parameterTypes[0] == msgClass
            ) {
                Log.i(TAG, "✔ 转发fallback: ${forwardClass.name}.${m.name}")
                return m
            }
        }
        return null
    }
}
