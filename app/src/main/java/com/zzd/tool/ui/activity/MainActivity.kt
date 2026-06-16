@file:Suppress("SetTextI18n")

package com.zzd.tool.ui.activity

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.tencent.mmkv.MMKV
import com.zzd.tool.BuildConfig
import com.zzd.tool.R
import com.zzd.tool.hook.core.SettingsManager

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var switchTablet: SwitchCompat
    private lateinit var switchRecall: SwitchCompat
    private lateinit var switchForward: SwitchCompat
    private lateinit var switchBadge: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MMKV.initialize(this)

        tvStatus = findViewById(R.id.main_text_status)
        val tvVersion = findViewById<TextView>(R.id.main_text_version)
        tvVersion.text = getString(R.string.module_version, BuildConfig.VERSION_NAME)

        switchTablet = findViewById(R.id.switch_tablet_toggle)
        switchRecall = findViewById(R.id.switch_recall_toggle)
        switchForward = findViewById(R.id.switch_forward_toggle)
        switchBadge = findViewById(R.id.switch_badge_toggle)

        loadAndBindSwitches()

        findViewById<TextView>(R.id.btn_clear_cache).setOnClickListener {
            try {
                SettingsManager.clearDexKitCache(this)
                Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(this, R.string.cache_clear_failed, Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<TextView>(R.id.btn_reset_settings).setOnClickListener {
            try {
                SettingsManager.resetAll(this)
                switchTablet.setOnCheckedChangeListener(null)
                switchRecall.setOnCheckedChangeListener(null)
                switchForward.setOnCheckedChangeListener(null)
                switchBadge.setOnCheckedChangeListener(null)
                switchTablet.isChecked = true
                switchRecall.isChecked = true
                switchForward.isChecked = true
                switchBadge.isChecked = true
                Toast.makeText(this, R.string.settings_reset, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(this, R.string.settings_reset_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshActivationStatus()
    }

    private fun loadAndBindSwitches() {
        val settings = SettingsManager.loadAll(this)
        switchTablet.isChecked = settings["hook_tablet"] ?: true
        switchRecall.isChecked = settings["hook_recall"] ?: true
        switchForward.isChecked = settings["hook_forward"] ?: true
        switchBadge.isChecked = settings["hook_badge"] ?: true

        switchTablet.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.putBoolean(this, "hook_tablet", isChecked)
        }
        switchRecall.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.putBoolean(this, "hook_recall", isChecked)
        }
        switchForward.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.putBoolean(this, "hook_forward", isChecked)
        }
        switchBadge.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.putBoolean(this, "hook_badge", isChecked)
        }
    }

    private fun refreshActivationStatus() {
        SettingsManager.initFromContentResolver(this)
        val isActive = SettingsManager.isActivated()
        val provider = SettingsManager.getProvider()
        tvStatus.text = if (isActive)
            getString(R.string.module_is_activated) + " ($provider)"
        else
            getString(R.string.module_not_activated) + "\n请先打开 ZZD 后重启"
    }
}
