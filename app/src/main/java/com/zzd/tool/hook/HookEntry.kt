package com.zzd.tool.hook

import android.util.Log
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.concurrent.ConcurrentHashMap

class HookEntry : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "ZddTool"
        private const val TARGET = "com.alibaba.taurus.zhejiang"

        // AIMMsgRecallType 常量（来自 smali 逆向）
        private const val RECALL_TYPE_GROUP_OWNER = 2
        private const val RECALL_TYPE_ADMIN = 5

        // msgId → recallType 映射（有上限防泄漏）
        private const val RECALL_MAP_MAX = 5000
        private val recalledMsgs = object : ConcurrentHashMap<Long, Int>() {
            override fun put(key: Long, value: Int): Int? {
                if (size >= RECALL_MAP_MAX) {
                    val first = keys().nextElement()
                    remove(first)
                }
                return super.put(key, value)
            }
        }

        // View tag，用于标记/查找我们自己添加的 badge TextView
        private const val BADGE_TAG = "zdd_badge"

        // 资源 ID 缓存（动态获取，避免硬编码）
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

        hookTablet(cl)
        hookRecallDetect(cl)
        hookShieldStatus(cl)
        hookForward(cl)
        hookViewHolder(cl)

        Log.i(TAG, "全部Hook完成")
    }

    // ── 平板模式 ──
    private fun hookTablet(cl: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.alibaba.dinggov.util.DeviceUtil",
                cl, "a", android.content.Context::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) { param.result = true }
                })
            Log.i(TAG, "✔ 平板")
        } catch (t: Throwable) { Log.e(TAG, "✘ 平板", t) }
    }

    // ── recallStatus → 检测撤回 + 返回 0 ──
    private fun hookRecallDetect(cl: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.alibaba.wukong.im.adapter.MessageDelegate",
                cl, "recallStatus",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val aimMsg = XposedHelpers.getObjectField(param.thisObject, "mAIMMessage")
                            if (XposedHelpers.getBooleanField(aimMsg, "isRecall")) {
                                val msgId = XposedHelpers.callMethod(
                                    param.thisObject, "messageId"
                                ) as? Long ?: return@beforeHookedMethod
                                if (recalledMsgs.putIfAbsent(msgId, 0) == null) {
                                    Log.i(TAG, "📌 检测到撤回 msgId=$msgId")
                                }
                            }
                        } catch (_: Exception) {}
                        param.result = 0
                    }
                })
            Log.i(TAG, "✔ 检测")
        } catch (t: Throwable) { Log.e(TAG, "✘ 检测", t) }
    }

    // ── shieldStatus → 返回 0 + 读取真实 recallType ──
    private fun hookShieldStatus(cl: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.alibaba.wukong.im.adapter.MessageDelegate",
                cl, "shieldStatus",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val aimMsg = XposedHelpers.getObjectField(param.thisObject, "mAIMMessage")
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
            Log.i(TAG, "✔ shield")
        } catch (t: Throwable) { Log.e(TAG, "✘ shield", t) }
    }

    // ── 解除转发限制 ──
    private fun hookForward(cl: ClassLoader) {
        try {
            val msgClass = XposedHelpers.findClass("com.alibaba.wukong.im.Message", cl)
            val convClass = XposedHelpers.findClass("com.alibaba.wukong.im.Conversation", cl)

            // ckv.i(Message) → 单独转发权限（语音能单独转发靠这个）
            XposedHelpers.findAndHookMethod(
                "taurus.ckv", cl, "i", msgClass,
                XC_MethodReplacement.returnConstant(true)
            )

            // cxb.b(Conversation, Collection) → 批量转发过滤入口
            // 跳过所有过滤逻辑，直接调用 a(Conversation, List) 执行转发
            XposedHelpers.findAndHookMethod(
                "taurus.cxb", cl, "b", convClass, java.util.Collection::class.java,
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        val conv = param.args[0]
                        val msgs = param.args[1]
                        val list = java.util.ArrayList<Any?>()
                        if (msgs is java.util.Collection<*>) list.addAll(msgs) else return null
                        XposedHelpers.callMethod(param.thisObject, "a", conv, list)
                        return null
                    }
                })

            Log.i(TAG, "✔ 转发")
        } catch (t: Throwable) { Log.e(TAG, "✘ 转发", t) }
    }

    // ── 读取 recallFeature.operatorType → int ──
    private fun readRecallType(aimMsg: Any): Int {
        return try {
            val recallFeature = XposedHelpers.getObjectField(aimMsg, "recallFeature")
            val operatorType = XposedHelpers.getObjectField(recallFeature, "operatorType")
            XposedHelpers.callMethod(operatorType, "getValue") as Int
        } catch (_: Exception) { 0 }
    }

    // ── 格式化消息时间（带秒）──
    private fun formatTime(msg: Any): String {
        return try {
            val ts = XposedHelpers.callMethod(msg, "createdAt") as? Long ?: return ""
            val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            sdf.format(java.util.Date(ts))
        } catch (_: Exception) { "" }
    }

    // ── 根据 recallType 返回 badge 文案 ──
    private fun recallBadge(recallType: Int): String {
        return if (recallType == RECALL_TYPE_ADMIN || recallType == RECALL_TYPE_GROUP_OWNER) {
            "⤴管理员已撤回"
        } else {
            "⤴已撤回"
        }
    }

    // ── getView hook：在消息右下角显示撤回标记 / 发送时间 ──
    private fun hookViewHolder(cl: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "taurus.awv", cl, "getView",
                Integer.TYPE, android.view.View::class.java, android.view.ViewGroup::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val rootView = param.result as? android.view.View ?: return
                            val position = param.args[0] as Int

                            val message = XposedHelpers.callMethod(
                                param.thisObject, "b", position
                            ) as? Any ?: return
                            val msgId = XposedHelpers.callMethod(message, "messageId") as? Long ?: return
                            val recallType = recalledMsgs[msgId]
                            val displayText = if (recallType != null) recallBadge(recallType) else formatTime(message)

                            // 方案1: 自己消息 → 已读计数区域
                            val selfView = rootView.findViewById<android.widget.TextView>(
                                resId(rootView, "chatting_unreadcount_tv1")
                            )
                            if (selfView != null) {
                                if (recallType != null) {
                                    selfView.visibility = android.view.View.VISIBLE
                                    selfView.text = displayText
                                }
                                // 未撤回：不动，让 app 正常显示已读计数
                                return
                            }

                            // 方案2: 别人消息 → rl_tips 容器
                            val rlTips = rootView.findViewById<android.view.ViewGroup>(
                                resId(rootView, "rl_tips")
                            )
                            if (rlTips != null) {
                                val oldBadge = rlTips.findViewWithTag<android.widget.TextView>(BADGE_TAG)
                                if (oldBadge != null) {
                                    oldBadge.text = displayText
                                } else {
                                    val badgeView = android.widget.TextView(rootView.context).apply {
                                        text = displayText
                                        textSize = 11f
                                        setTextColor(0xFF999999.toInt())
                                        includeFontPadding = false
                                        tag = BADGE_TAG
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

                            // 方案3: 兜底
                            val fallbackTv = findFirstTextView(rootView)
                            if (fallbackTv != null && recallType != null) {
                                fallbackTv.text = "${fallbackTv.text}\n$displayText"
                                fallbackTv.setTextColor(0xFFE53935.toInt())
                            }
                        } catch (_: Exception) {}
                    }
                })
            Log.i(TAG, "✔ viewHolder")
        } catch (t: Throwable) { Log.e(TAG, "✘ viewHolder", t) }
    }

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
