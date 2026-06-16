package com.zzd.tool.hook

import android.util.Log
import com.zzd.tool.hook.core.BaseHook
import com.zzd.tool.hook.core.HookFeature
import com.zzd.tool.hook.core.HookUtils
import de.robv.android.xposed.XposedHelpers
import java.util.concurrent.ConcurrentHashMap

class AntiRecallHook : BaseHook(HookFeature.RECALL) {

    companion object {
        private const val TAG = "ZddTool"
        private const val RECALL_MAP_MAX = 5000

        val recalledMsgs = object : ConcurrentHashMap<Long, Int>() {
            override fun put(key: Long, value: Int): Int? {
                if (size >= RECALL_MAP_MAX) {
                    val first = keys().nextElement()
                    remove(first)
                }
                return super.put(key, value)
            }
        }

        const val RECALL_TYPE_GROUP_OWNER = 2
        const val RECALL_TYPE_ADMIN = 5
    }

    override fun onInit(cl: ClassLoader): Boolean {
        hookRecallStatus(cl)
        hookShieldStatus(cl)
        return true
    }

    private fun hookRecallStatus(cl: ClassLoader) {
        HookUtils.hookBeforeByClass(
            this,
            "com.alibaba.wukong.im.adapter.MessageDelegate",
            cl,
            "recallStatus"
        ) { param ->
            try {
                val aimMsg = XposedHelpers.getObjectField(param.thisObject, "mAIMMessage")
                if (XposedHelpers.getBooleanField(aimMsg, "isRecall")) {
                    val msgId = XposedHelpers.callMethod(param.thisObject, "messageId") as? Long
                        ?: return@hookBeforeByClass
                    if (recalledMsgs.putIfAbsent(msgId, 0) == null) {
                        Log.i(TAG, "📌 撤回 msgId=$msgId")
                    }
                }
            } catch (_: Exception) {
            }
            param.result = 0
        }
    }

    private fun hookShieldStatus(cl: ClassLoader) {
        HookUtils.hookBeforeByClass(
            this,
            "com.alibaba.wukong.im.adapter.MessageDelegate",
            cl,
            "shieldStatus"
        ) { param ->
            try {
                val aimMsg = XposedHelpers.getObjectField(param.thisObject, "mAIMMessage")
                if (XposedHelpers.getBooleanField(aimMsg, "isRecall")) {
                    val msgId = XposedHelpers.callMethod(param.thisObject, "messageId") as? Long
                        ?: return@hookBeforeByClass
                    val recallType = readRecallType(aimMsg)
                    if (recallType > 0) {
                        recalledMsgs[msgId] = recallType
                        Log.i(TAG, "!!! 撤回 msgId=$msgId type=$recallType")
                    }
                }
            } catch (_: Exception) {
            }
            param.result = 0
        }
    }

    private fun readRecallType(aimMsg: Any): Int = try {
        val rf = XposedHelpers.getObjectField(aimMsg, "recallFeature")
        val ot = XposedHelpers.getObjectField(rf, "operatorType")
        XposedHelpers.callMethod(ot, "getValue") as Int
    } catch (_: Exception) {
        0
    }

    private fun recallBadge(type: Int): String =
        if (type == RECALL_TYPE_ADMIN || type == RECALL_TYPE_GROUP_OWNER)
            "⤴管理员已撤回" else "⤴已撤回"
}
