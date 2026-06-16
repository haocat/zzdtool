package com.zzd.tool.application

import android.app.Application
import com.tencent.mmkv.MMKV

class DefaultApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
    }
}
