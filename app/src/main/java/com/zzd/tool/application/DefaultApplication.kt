package com.zzd.tool.application

import android.app.Application
import android.util.Log
import com.tencent.mmkv.MMKV

class DefaultApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 用宿主的 dataDir，与 HookEntry 保持一致
        try {
            MMKV.initialize("/data/data/com.alibaba.taurus.zhejiang/files")
        } catch (e: Throwable) {
            Log.w("ZddTool", "MMKV init failed: ${e.message}")
            MMKV.initialize(this)
        }
    }
}
