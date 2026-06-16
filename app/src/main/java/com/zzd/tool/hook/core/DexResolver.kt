package com.zzd.tool.hook.core

import android.util.Log
import com.tencent.mmkv.MMKV
import de.robv.android.xposed.XposedHelpers
import org.luckypray.dexkit.DexKitBridge
import java.io.File

object DexResolver {

    private const val TAG = "ZddTool"

    private var bridge: DexKitBridge? = null
    private var _loader: ClassLoader? = null
    private var _apkKey: String? = null
    private var _apkPath: String? = null
    private var mmkv: MMKV? = null

    private val classCache = HashMap<String, Class<*>>()

    init {
        try {
            System.loadLibrary("dexkit")
            Log.i(TAG, "DexKit native 库已加载")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "✘ DexKit native 库: ${e.message}")
        }
    }

    fun init(apkPath: String, classLoader: ClassLoader, cacheDir: String? = null) {
        _loader = classLoader
        _apkPath = apkPath
        val apkFile = File(apkPath)
        _apkKey = "${apkFile.name}_${apkFile.length()}_${apkFile.lastModified()}"

        try {
            mmkv = MMKV.mmkvWithID("zzdtool_dexkit")
            val cachedApkKey = mmkv?.decodeString("apk_key")
            if (cachedApkKey != _apkKey) {
                mmkv?.clearAll()
                mmkv?.encode("apk_key", _apkKey ?: "")
                Log.i(TAG, "DexResolver: 缓存已失效 (APK 变化)")
            } else {
                val count = mmkv?.allKeys()?.size?.minus(1) ?: 0
                Log.i(TAG, "DexResolver: 使用 MMKV 缓存 ($count 条)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "DexResolver MMKV init failed: ${e.message}")
        }

        if (mmkv == null) {
            ensureBridge()
        }
    }

    private fun ensureBridge(): Boolean {
        if (bridge != null) return true
        val path = _apkPath ?: return false
        Log.i(TAG, "DexResolver: 扫描 $path")
        val start = System.currentTimeMillis()
        bridge = DexKitBridge.create(path) ?: return false
        Log.i(TAG, "✔ DexResolver 初始化完成 (${System.currentTimeMillis() - start}ms)")
        return true
    }

    fun findClassByStrings(vararg traitStrings: String): Class<*>? {
        return findClassesByStrings(*traitStrings).firstOrNull()
    }

    fun findClassesByStrings(vararg traitStrings: String): List<Class<*>> {
        val key = traitStrings.joinToString("|")
        val l = _loader ?: return emptyList()

        val cached = classCache[key]
        if (cached != null) return listOf(cached)

        val cachedName = mmkv?.decodeString(key)
        if (cachedName != null) {
            try {
                val clazz = XposedHelpers.findClass(cachedName, l)
                classCache[key] = clazz
                return listOf(clazz)
            } catch (_: Throwable) {
                mmkv?.remove(key)
            }
        }

        if (!ensureBridge()) return emptyList()
        val b = bridge ?: return emptyList()

        val result = mutableListOf<Class<*>>()
        try {
            for (cd in b.findClass {
                searchPackages("taurus")
                matcher { usingStrings(*traitStrings) }
            }) {
                try {
                    val clazz = XposedHelpers.findClass(cd.name, l)
                    classCache[key] = clazz
                    mmkv?.encode(key, cd.name)
                    result.add(clazz)
                } catch (_: Throwable) { continue }
            }
        } catch (e: Exception) {
            Log.e(TAG, "✘ [$key]: ${e.message}")
        }
        if (result.isNotEmpty()) Log.i(TAG, "✔ classes [$key] → ${result.size} 个")
        return result
    }

    fun release() {
        bridge?.close()
        bridge = null
        Log.i(TAG, "DexResolver 已释放")
    }
}
