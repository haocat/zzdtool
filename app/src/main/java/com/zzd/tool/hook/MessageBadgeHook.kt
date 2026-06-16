package com.zzd.tool.hook

import android.util.Log
import android.widget.RelativeLayout
import android.widget.TextView
import com.zzd.tool.hook.core.BaseHook
import com.zzd.tool.hook.core.DexResolver
import com.zzd.tool.hook.core.HookFeature
import com.zzd.tool.hook.core.SettingsManager
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageBadgeHook : BaseHook(HookFeature.BADGE) {

    companion object {
        private const val TAG = "ZddTool"
        private const val BADGE_TAG = "zdd_badge"
        private val resIdCache = HashMap<String, Int>()
        private val timeFormat = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue() = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        }

        fun resId(view: android.view.View, name: String): Int {
            return resIdCache.getOrPut(name) {
                view.resources.getIdentifier(name, "id", "com.alibaba.taurus.zhejiang")
            }
        }
    }

    override fun onInit(cl: ClassLoader): Boolean {
        val target = DexResolver.findClassByStrings("getItemViewType position: ")
            ?: findClassByFallback(cl, "taurus.awv")
            ?: run {
                Log.e(TAG, "✘ viewHolder: 找不到类")
                return false
            }

        XposedHelpers.findAndHookMethod(
            target, "getView",
            Integer.TYPE, android.view.View::class.java,
            android.view.ViewGroup::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!isEnabled()) return
                    try {
                        onAfterGetView(param)
                    } catch (_: Exception) {
                    }
                }
            }
        )
        return true
    }

    private fun onAfterGetView(param: XC_MethodHook.MethodHookParam) {
        val rootView = param.result as? android.view.View ?: return
        val position = (param.args[0] as? Int) ?: return
        val message = XposedHelpers.callMethod(param.thisObject, "b", position) as? Any ?: return
        val msgId = XposedHelpers.callMethod(message, "messageId") as? Long ?: return

        val recallEnabled = SettingsManager.getBoolean("hook_recall")
        val recallType = if (recallEnabled) AntiRecallHook.recalledMsgs[msgId] else null
        val displayText = if (recallType != null) {
            if (recallType == AntiRecallHook.RECALL_TYPE_ADMIN ||
                recallType == AntiRecallHook.RECALL_TYPE_GROUP_OWNER
            ) "⤴管理员已撤回" else "⤴已撤回"
        } else {
            formatTime(message)
        }

        val selfView = rootView.findViewById<TextView>(resId(rootView, "chatting_unreadcount_tv1"))
        if (selfView != null) {
            if (recallType != null) {
                selfView.visibility = android.view.View.VISIBLE
                selfView.text = displayText
            }
            return
        }

        val rlTips = rootView.findViewById<android.view.ViewGroup>(resId(rootView, "rl_tips"))
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
                val lp = RelativeLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.addRule(RelativeLayout.ALIGN_PARENT_START)
                lp.addRule(RelativeLayout.CENTER_VERTICAL)
                rlTips.addView(badgeView, lp)
            }
            return
        }

        val fallback = findFirstTextView(rootView)
        if (fallback != null && recallType != null) {
            fallback.text = "${fallback.text}\n$displayText"
            fallback.setTextColor(0xFFE53935.toInt())
        }
    }

    private fun formatTime(msg: Any): String = try {
        val ts = XposedHelpers.callMethod(msg, "createdAt") as? Long ?: return ""
        timeFormat.get()!!.format(Date(ts))
    } catch (_: Exception) {
        ""
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

    private fun findClassByFallback(cl: ClassLoader, vararg names: String): Class<*>? {
        for (n in names) {
            try {
                return XposedHelpers.findClass(n, cl)
            } catch (_: Throwable) {
            }
        }
        return null
    }
}
