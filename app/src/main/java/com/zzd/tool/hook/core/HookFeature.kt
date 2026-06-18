package com.zzd.tool.hook.core

enum class HookFeature(val key: String, val defaultEnabled: Boolean, val label: String) {
    TABLET("hook_tablet", true, "平板模式"),
    RECALL("hook_recall", true, "防撤回"),
    FORWARD("hook_forward", true, "转发解锁"),
    BADGE("hook_badge", true, "消息徽章"),
    DARK_MODE("hook_dark_mode", false, "ColorOS深色修复");
}
