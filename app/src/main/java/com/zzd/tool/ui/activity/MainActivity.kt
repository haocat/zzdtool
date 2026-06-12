@file:Suppress("SetTextI18n")

package com.zzd.tool.ui.activity

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zzd.tool.BuildConfig
import com.zzd.tool.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvStatus = findViewById<TextView>(R.id.main_text_status)
        val tvVersion = findViewById<TextView>(R.id.main_text_version)
        val tvApi = findViewById<TextView>(R.id.main_text_api_way)

        tvVersion.text = getString(R.string.module_version, BuildConfig.VERSION_NAME)
        tvStatus.text = "浙政钉工具\n平板登录 + 防撤回"
        tvApi.visibility = android.view.View.GONE
    }
}
