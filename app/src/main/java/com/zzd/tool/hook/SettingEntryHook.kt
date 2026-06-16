package com.zzd.tool.hook

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.zzd.tool.hook.core.BaseHook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class SettingEntryHook : BaseHook() {

    companion object {
        private const val TAG = "ZddTool"
        private const val COMMON_ACTIVITY = "com.alibaba.taurus.user.kit.activity.CommonActivity"
    }

    override val label = "设置入口"

    override fun onInit(cl: ClassLoader): Boolean {
        val clazz = XposedHelpers.findClass(COMMON_ACTIVITY, cl)
        XposedHelpers.findAndHookMethod(clazz, "onCreate", Bundle::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val activity = param.thisObject as? Activity ?: return
                        injectEntry(activity)
                    } catch (e: Throwable) {
                        Log.e(TAG, "Inject failed", e)
                    }
                }
            })
        return true
    }

    private fun injectEntry(activity: Activity) {
        val rootView = activity.findViewById<View>(android.R.id.content) ?: return
        val scrollView = findScrollView(rootView) ?: return
        val container = scrollView.getChildAt(0) as? ViewGroup ?: return

        if (container.findViewWithTag<View>("zzd_tool_entry") != null) return

        container.addView(createEntryView(activity), 0)
        Log.i(TAG, "✔ 设置入口已注入")
    }

    private fun findScrollView(view: View): ScrollView? {
        if (view is ScrollView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findScrollView(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    private fun createEntryView(activity: Activity): View {
        val dp = { value: Int ->
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(),
                activity.resources.displayMetrics).toInt()
        }

        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setBackgroundColor(Color.WHITE)
            tag = "zzd_tool_entry"
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val intent = Intent().apply {
                    component = ComponentName("com.zzd.tool", "com.zzd.tool.ui.activity.MainActivity")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try { activity.startActivity(intent) } catch (e: Throwable) {
                    Log.e(TAG, "Launch settings failed", e)
                }
            }
            addView(TextView(activity).apply {
                text = "ZZD Tool 设置"
                setTextColor(Color.parseColor("#1A73E8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = Typeface.DEFAULT_BOLD
            })
            addView(TextView(activity).apply {
                text = "平板模式 · 防撤回 · 转发解锁 · 消息徽章"
                setTextColor(Color.parseColor("#666666"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(2), 0, 0)
            })
        }
    }
}
