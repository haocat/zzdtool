package com.zzd.tool.hook

import android.util.Log
import android.widget.TextView
import com.zzd.tool.config.SettingsManager
import com.zzd.tool.hook.core.DexResolver
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class HookEntry : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "ZddTool"
        private const val TARGET = "com.alibaba.taurus.zhejiang"
        private const val RECALL_TYPE_GROUP_OWNER = 2
        private const val RECALL_TYPE_ADMIN = 5
        private const val RECALL_MAP_MAX = 5000
        private const val BADGE_TAG = "zdd_badge"

        private val recalledMsgs = object : ConcurrentHashMap<Long, Int>() {
            override fun put(key: Long, value: Int): Int? {
                if (size >= RECALL_MAP_MAX) {
                    val first = keys().nextElement()
                    remove(first)
                }
                return super.put(key, value)
            }
        }

        private val resIdCache = HashMap<String, Int>()
        private fun resId(view: android.view.View, name: String): Int {
            return resIdCache.getOrPut(name) {
                view.resources.getIdentifier(name, "id", TARGET)
            }
        }
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET) return
        val cl = lpparam.classLoader
        Log.i(TAG, "浙政钉已加载")

        val settingsAvailable = try {
            val app = XposedHelpers.callStaticMethod(
                XposedHelpers.findClass("android.app.ActivityThread", null),
                "currentApplication"
            ) as? android.app.Application
            if (app != null) SettingsManager.initHooks(app) else false
        } catch (e: Throwable) {
            Log.w(TAG, "Cannot get context, all features enabled")
            false
        }

        DexResolver.init(lpparam.appInfo.sourceDir, cl,
            cacheDir = "${lpparam.appInfo.dataDir}/files")

        if (!settingsAvailable || SettingsManager.isTabletEnabled()) hookTablet(cl)
        if (!settingsAvailable || SettingsManager.isRecallEnabled()) {
            hookRecall(cl)
            hookShield(cl)
        }
        if (!settingsAvailable || SettingsManager.isForwardEnabled()) hookForward(cl)
        if (!settingsAvailable || SettingsManager.isBadgeEnabled()) hookViewHolder(cl)

        DexResolver.release()
        Log.i(TAG, "全部Hook完成")
    }

    // ══════════════════════════════════════════════
    // 1. 平板模式
    // ══════════════════════════════════════════════
    private fun hookTablet(cl: ClassLoader) {
        try {
            val target = DexResolver.findClassByStrings("config_deviceInfo")
                ?: XposedHelpers.findClass("com.alibaba.dinggov.util.DeviceUtil", cl)
            XposedHelpers.findAndHookMethod(target, "a",
                android.content.Context::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = true
                    }
                })
            Log.i(TAG, "✔ 平板")
        } catch (t: Throwable) { Log.e(TAG, "✘ 平板", t) }
    }

    // ══════════════════════════════════════════════
    // 2. 撤回检测 — recallStatus
    //    MessageDelegate 是 SDK 类，不混淆，直接硬编码
    // ══════════════════════════════════════════════
    private fun hookRecall(cl: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.alibaba.wukong.im.adapter.MessageDelegate",
                cl, "recallStatus",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val aimMsg = XposedHelpers.getObjectField(
                                param.thisObject, "mAIMMessage")
                            if (XposedHelpers.getBooleanField(aimMsg, "isRecall")) {
                                val msgId = XposedHelpers.callMethod(
                                    param.thisObject, "messageId"
                                ) as? Long ?: return@beforeHookedMethod
                                if (recalledMsgs.putIfAbsent(msgId, 0) == null) {
                                    Log.i(TAG, "📌 撤回 msgId=$msgId")
                                }
                            }
                        } catch (_: Exception) {}
                        param.result = 0
                    }
                })
            Log.i(TAG, "✔ recallStatus")
        } catch (t: Throwable) { Log.e(TAG, "✘ recallStatus", t) }
    }

    // ══════════════════════════════════════════════
    // 3. shield 处理 — shieldStatus
    //    MessageDelegate 是 SDK 类不混淆，直接用硬编码名
    // ══════════════════════════════════════════════
    private fun hookShield(cl: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.alibaba.wukong.im.adapter.MessageDelegate",
                cl, "shieldStatus",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val aimMsg = XposedHelpers.getObjectField(
                                param.thisObject, "mAIMMessage")
                            if (XposedHelpers.getBooleanField(aimMsg, "isRecall")) {
                                val msgId = XposedHelpers.callMethod(
                                    param.thisObject, "messageId"
                                ) as? Long ?: return@beforeHookedMethod
                                val recallType = readRecallType(aimMsg)
                                if (recallType > 0) {
                                    recalledMsgs[msgId] = recallType
                                    Log.i(TAG, "!!! 撤回 msgId=$msgId type=$recallType")
                                }
                            }
                        } catch (_: Exception) {}
                        param.result = 0
                    }
                })
            Log.i(TAG, "✔ shieldStatus")
        } catch (t: Throwable) { Log.e(TAG, "✘ shield", t) }
    }

    // ══════════════════════════════════════════════
    // 4. 解除转发限制
    // ══════════════════════════════════════════════
    private fun hookForward(cl: ClassLoader) {
        try {
            val msgClass = XposedHelpers.findClass("com.alibaba.wukong.im.Message", cl)
            val convClass = XposedHelpers.findClass("com.alibaba.wukong.im.Conversation", cl)

            // ── 单独转发：ckx.i(Message)Z ──
            // 用 返回值(boolean)+参数(Message)+数值(0x642) 三重定位，唯一标识此方法
            var methodName = DexResolver.findMethodNameBySignature(
                "boolean", arrayOf("com.alibaba.wukong.im.Message"), 0x642)
            if (methodName == null) {
                // fallback: 试 "i"（两版 APK 都没变）
                methodName = "i"
            }
            try {
                XposedHelpers.findAndHookMethod("taurus.ckx", cl, methodName, msgClass,
                    XC_MethodReplacement.returnConstant(true))
                Log.i(TAG, "✔ ckx.$methodName")
            } catch (_: NoSuchMethodError) {
                // 方法名可能变了 → fallback: 搜签名 (Message)→boolean
                val ckxClass = XposedHelpers.findClass("taurus.ckx", cl)
                for (m in ckxClass.declaredMethods) {
                    if (m.returnType == java.lang.Boolean.TYPE && m.parameterTypes.size == 1
                        && m.parameterTypes[0] == msgClass) {
                        try {
                            XposedHelpers.findAndHookMethod(ckxClass, m.name, msgClass,
                                XC_MethodReplacement.returnConstant(true))
                            Log.i(TAG, "✔ ckx.${m.name}")
                            break
                        } catch (_: Throwable) {}
                    }
                }
            }

            // ── 批量转发：cxb.b() ──
            try {
                val cxbNames = listOf("taurus.cxb", "taurus.cww", "taurus.cwy")
                var cxbClass: Class<*>? = null
                for (name in cxbNames) {
                    try { cxbClass = XposedHelpers.findClass(name, cl); break }
                    catch (_: Throwable) {}
                }
                if (cxbClass != null) {
                    XposedHelpers.findAndHookMethod(cxbClass, "b", convClass,
                        java.util.Collection::class.java,
                        object : XC_MethodReplacement() {
                            override fun replaceHookedMethod(param: MethodHookParam): Any? {
                                val conv = param.args[0]
                                val list = java.util.ArrayList<Any?>()
                                (param.args[1] as? java.util.Collection<*>)?.let {
                                    list.addAll(it)
                                } ?: return null
                                XposedHelpers.callMethod(param.thisObject, "a", conv, list)
                                return null
                            }
                        })
                }
            } catch (_: Throwable) {}

            Log.i(TAG, "✔ 转发")
        } catch (t: Throwable) { Log.e(TAG, "✘ 转发", t) }
    }

    // ══════════════════════════════════════════════
    // 5. 右下角 badge / 时间
    // ══════════════════════════════════════════════
    private fun hookViewHolder(cl: ClassLoader) {
        try {
            val target = DexResolver.findClassByStrings("getItemViewType position: ")
                ?: findClassByFallback(cl, "taurus.awv") ?: run {
                Log.e(TAG, "✘ viewHolder: 找不到类")
                return
            }
            XposedHelpers.findAndHookMethod(target, "getView",
                Integer.TYPE, android.view.View::class.java,
                android.view.ViewGroup::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val rootView = param.result as? android.view.View ?: return
                            val position = (param.args[0] as? Int) ?: return
                            val message = XposedHelpers.callMethod(
                                param.thisObject, "b", position
                            ) as? Any ?: return
                            val msgId = XposedHelpers.callMethod(message, "messageId"
                            ) as? Long ?: return
                            val recallType = recalledMsgs[msgId]
                            val displayText = if (recallType != null) {
                                recallBadge(recallType)
                            } else {
                                formatTime(message)
                            }
                            val selfView = rootView.findViewById<TextView>(
                                resId(rootView, "chatting_unreadcount_tv1"))
                            if (selfView != null) {
                                if (recallType != null) {
                                    selfView.visibility = android.view.View.VISIBLE
                                    selfView.text = displayText
                                }
                                return
                            }
                            val rlTips = rootView.findViewById<android.view.ViewGroup>(
                                resId(rootView, "rl_tips"))
                            if (rlTips != null) {
                                val oldBadge = rlTips.findViewWithTag<TextView>(BADGE_TAG)
                                if (oldBadge != null) {
                                    oldBadge.text = displayText
                                } else {
                                    val badgeView = TextView(rootView.context).apply {
                                        text = displayText; textSize = 11f
                                        setTextColor(0xFF999999.toInt())
                                        includeFontPadding = false; tag = BADGE_TAG
                                    }
                                    val lp = android.widget.RelativeLayout.LayoutParams(
                                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                                    )
                                    lp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START)
                                    lp.addRule(android.widget.RelativeLayout.CENTER_VERTICAL)
                                    rlTips.addView(badgeView, lp)
                                }
                                return
                            }
                            val fallback = findFirstTextView(rootView)
                            if (fallback != null && recallType != null) {
                                fallback.text = "${fallback.text}\n$displayText"
                                fallback.setTextColor(0xFFE53935.toInt())
                            }
                        } catch (_: Exception) {}
                    }
                })
            Log.i(TAG, "✔ viewHolder")
        } catch (t: Throwable) { Log.e(TAG, "✘ viewHolder", t) }
    }

    // ══════════════════════════════════════════════
    // 工具方法
    // ══════════════════════════════════════════════

    private fun findClassByFallback(cl: ClassLoader, vararg names: String): Class<*>? {
        for (n in names) {
            try { return XposedHelpers.findClass(n, cl) } catch (_: Throwable) {}
        }
        return null
    }

    private fun readRecallType(aimMsg: Any): Int = try {
        val rf = XposedHelpers.getObjectField(aimMsg, "recallFeature")
        val ot = XposedHelpers.getObjectField(rf, "operatorType")
        XposedHelpers.callMethod(ot, "getValue") as Int
    } catch (_: Exception) { 0 }

    private fun recallBadge(type: Int) = if (type == RECALL_TYPE_ADMIN || type == RECALL_TYPE_GROUP_OWNER)
        "⤴管理员已撤回" else "⤴已撤回"

    private fun formatTime(msg: Any): String = try {
        val ts = XposedHelpers.callMethod(msg, "createdAt") as? Long ?: return ""
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(ts))
    } catch (_: Exception) { "" }

    private fun findFirstTextView(view: android.view.View): TextView? {
        if (view is TextView) return view
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findFirstTextView(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }
}
