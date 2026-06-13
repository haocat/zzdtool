@file:Suppress("SetTextI18n")

package com.zzd.tool.ui.activity

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.zzd.tool.BuildConfig
import com.zzd.tool.R
import com.zzd.tool.config.SettingsManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvStatus = findViewById<TextView>(R.id.main_text_status)
        val tvVersion = findViewById<TextView>(R.id.main_text_version)

        tvVersion.text = getString(R.string.module_version, BuildConfig.VERSION_NAME)
        tvStatus.text = "浙政钉工具\n平板登录 + 防撤回"

        val prefs = SettingsManager.getActivityPrefs(this)

        val switchTablet = findViewById<SwitchCompat>(R.id.switch_tablet_toggle)
        val switchRecall = findViewById<SwitchCompat>(R.id.switch_recall_toggle)
        val switchForward = findViewById<SwitchCompat>(R.id.switch_forward_toggle)
        val switchBadge = findViewById<SwitchCompat>(R.id.switch_badge_toggle)
        val switchCache = findViewById<SwitchCompat>(R.id.switch_cache_toggle)

        switchTablet.isChecked = prefs.getBoolean("hook_tablet", true)
        switchRecall.isChecked = prefs.getBoolean("hook_recall", true)
        switchForward.isChecked = prefs.getBoolean("hook_forward", true)
        switchBadge.isChecked = prefs.getBoolean("hook_badge", true)
        switchCache.isChecked = prefs.getBoolean("dexkit_cache", true)

        switchTablet.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("hook_tablet", isChecked).apply()
        }
        switchRecall.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("hook_recall", isChecked).apply()
        }
        switchForward.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("hook_forward", isChecked).apply()
        }
        switchBadge.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("hook_badge", isChecked).apply()
        }
        switchCache.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dexkit_cache", isChecked).apply()
        }

        findViewById<TextView>(R.id.btn_clear_cache).setOnClickListener {
            try {
                SettingsManager.clearDexKitCache(this)
                Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, R.string.cache_clear_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}