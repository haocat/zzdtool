package com.zzd.tool.hook.core

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object HookUtils {

    fun hookBeforeIfEnabled(
        hook: BaseHook,
        clazz: Class<*>,
        methodName: String,
        vararg paramTypes: Any,
        action: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        XposedHelpers.findAndHookMethod(
            clazz, methodName, *paramTypes,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!hook.isEnabled()) return
                    try {
                        action(param)
                    } catch (e: Throwable) {
                        hook.traceError(e)
                    }
                }
            }
        )
    }

    fun hookAfterIfEnabled(
        hook: BaseHook,
        clazz: Class<*>,
        methodName: String,
        vararg paramTypes: Any,
        action: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        XposedHelpers.findAndHookMethod(
            clazz, methodName, *paramTypes,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!hook.isEnabled()) return
                    try {
                        action(param)
                    } catch (e: Throwable) {
                        hook.traceError(e)
                    }
                }
            }
        )
    }

    fun hookBeforeAlways(
        hook: BaseHook,
        clazz: Class<*>,
        methodName: String,
        vararg paramTypes: Any,
        action: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        XposedHelpers.findAndHookMethod(
            clazz, methodName, *paramTypes,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        action(param)
                    } catch (e: Throwable) {
                        hook.traceError(e)
                    }
                }
            }
        )
    }

    fun hookAfterAlways(
        hook: BaseHook,
        clazz: Class<*>,
        methodName: String,
        vararg paramTypes: Any,
        action: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        XposedHelpers.findAndHookMethod(
            clazz, methodName, *paramTypes,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        action(param)
                    } catch (e: Throwable) {
                        hook.traceError(e)
                    }
                }
            }
        )
    }

    fun hookBeforeByClass(
        hook: BaseHook,
        className: String,
        cl: ClassLoader,
        methodName: String,
        vararg paramTypes: Any,
        action: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        val clazz = XposedHelpers.findClass(className, cl)
        hookBeforeIfEnabled(hook, clazz, methodName, *paramTypes, action = action)
    }
}
