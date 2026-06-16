package com.zzd.tool.hook

import android.content.Context
import com.zzd.tool.hook.core.BaseHook
import com.zzd.tool.hook.core.HookFeature
import com.zzd.tool.hook.core.HookUtils
import de.robv.android.xposed.XposedHelpers

class TabletModeHook : BaseHook(HookFeature.TABLET) {

    override fun onInit(cl: ClassLoader): Boolean {
        val clazz = XposedHelpers.findClass(
            "com.alibaba.dinggov.util.DeviceUtil", cl
        )
        HookUtils.hookBeforeIfEnabled(this, clazz, "a", Context::class.java) { param ->
            param.result = true
        }
        return true
    }
}
