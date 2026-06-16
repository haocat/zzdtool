@file:Suppress("SetTextI18n")

package com.zzd.tool.ui.activity

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.zzd.tool.BuildConfig
import com.zzd.tool.R
import com.zzd.tool.hook.core.SettingsManager
import com.zzd.tool.hook.core.HookFeature

class MainActivity : AppCompatActivity() {

    private val switchViews = mutableMapOf<HookFeature, SwitchCompat>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvStatus = findViewById<TextView>(R.id.main_text_status)
        val tvVersion = findViewById<TextView>(R.id.main_text_version)

        tvVersion.text = getString(R.string.module_version, BuildConfig.VERSION_NAME)
        tvStatus.text = getString(R.string.module_subtitle)

        switchViews[HookFeature.TABLET] = findViewById(R.id.switch_tablet_toggle)
        switchViews[HookFeature.RECALL] = findViewById(R.id.switch_recall_toggle)
        switchViews[HookFeature.FORWARD] = findViewById(R.id.switch_forward_toggle)
        switchViews[HookFeature.BADGE] = findViewById(R.id.switch_badge_toggle)

        loadAndBindSwitches()

        findViewById<TextView>(R.id.btn_clear_cache).setOnClickListener {
            try {
                SettingsManager.clearDexKitCache(filesDir)
                Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, R.string.cache_clear_failed, Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<TextView>(R.id.btn_reset_settings).setOnClickListener {
            try {
                SettingsManager.resetAll()
                switchViews.values.forEach { it.setOnCheckedChangeListener(null) }
                switchViews.forEach { (feature, switch) ->
                    switch.isChecked = feature.defaultEnabled
                }
                Toast.makeText(this, R.string.settings_reset, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, R.string.settings_reset_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadAndBindSwitches() {
        val settings = SettingsManager.loadAll()
        switchViews.forEach { (feature, switch) ->
            switch.isChecked = settings[feature.key] ?: feature.defaultEnabled
            switch.setOnCheckedChangeListener { _, isChecked ->
                SettingsManager.putBoolean(feature.key, isChecked)
            }
        }
    }
}
