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

    private lateinit var switchTablet: SwitchCompat
    private lateinit var switchRecall: SwitchCompat
    private lateinit var switchForward: SwitchCompat
    private lateinit var switchBadge: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvStatus = findViewById<TextView>(R.id.main_text_status)
        val tvVersion = findViewById<TextView>(R.id.main_text_version)

        tvVersion.text = getString(R.string.module_version, BuildConfig.VERSION_NAME)
        tvStatus.text = getString(R.string.module_subtitle)

        switchTablet = findViewById(R.id.switch_tablet_toggle)
        switchRecall = findViewById(R.id.switch_recall_toggle)
        switchForward = findViewById(R.id.switch_forward_toggle)
        switchBadge = findViewById(R.id.switch_badge_toggle)

        loadAndBindSwitches()

        findViewById<TextView>(R.id.btn_clear_cache).setOnClickListener {
            try {
                SettingsManager.clearDexKitCache(this)
                Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, R.string.cache_clear_failed, Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<TextView>(R.id.btn_reset_settings).setOnClickListener {
            try {
                SettingsManager.resetSettings(this)
                // 先移除监听器，避免写回旧值
                switchTablet.setOnCheckedChangeListener(null)
                switchRecall.setOnCheckedChangeListener(null)
                switchForward.setOnCheckedChangeListener(null)
                switchBadge.setOnCheckedChangeListener(null)
                // 复位开关到默认值
                switchTablet.isChecked = true
                switchRecall.isChecked = true
                switchForward.isChecked = true
                switchBadge.isChecked = true
                Toast.makeText(this, R.string.settings_reset, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, R.string.settings_reset_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadAndBindSwitches() {
        val settings = SettingsManager.loadSettings(this)
        switchTablet.isChecked = settings.optBoolean("hook_tablet", true)
        switchRecall.isChecked = settings.optBoolean("hook_recall", true)
        switchForward.isChecked = settings.optBoolean("hook_forward", true)
        switchBadge.isChecked = settings.optBoolean("hook_badge", true)

        switchTablet.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveSetting(this, "hook_tablet", isChecked)
        }
        switchRecall.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveSetting(this, "hook_recall", isChecked)
        }
        switchForward.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveSetting(this, "hook_forward", isChecked)
        }
        switchBadge.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveSetting(this, "hook_badge", isChecked)
        }
    }
}
