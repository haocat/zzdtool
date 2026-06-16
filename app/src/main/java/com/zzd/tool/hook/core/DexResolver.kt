package com.zzd.tool.hook.core

import android.util.Log
import de.robv.android.xposed.XposedHelpers
import org.json.JSONObject
import org.luckypray.dexkit.DexKitBridge
import java.io.File

/**
 * DexKit 封装：运行时搜索混淆后的类。
 * 支持缓存到文件，APK 更新后自动失效重扫。
 * bridge 按需创建：至少有一个搜索键在缓存中没命中时，才创建 bridge 扫描。
 */
object DexResolver {

    private const val TAG = "ZddTool"
    private const val CACHE_FILE = "zzdtool_dexkit.json"

    private var bridge: DexKitBridge? = null
    private var _loader: ClassLoader? = null
    private var _cacheDir: String? = null
    private var _apkKey: String? = null
    private var _apkPath: String? = null

    private val classCache = HashMap<String, Class<*>>()
    private val classNameCache = HashMap<String, String>()

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
        _cacheDir = cacheDir
        _apkPath = apkPath
        val apkFile = File(apkPath)
        _apkKey = "${apkFile.name}_${apkFile.length()}_${apkFile.lastModified()}"

        if (cacheDir != null && loadCache()) {
            Log.i(TAG, "DexResolver: 使用缓存")
        } else {
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

        val cachedName = classNameCache[key]
        if (cachedName != null) {
            try {
                val clazz = XposedHelpers.findClass(cachedName, l)
                classCache[key] = clazz
                return listOf(clazz)
            } catch (_: Throwable) {
                classNameCache.remove(key)
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
                    classNameCache[key] = cd.name
                    result.add(clazz)
                } catch (_: Throwable) { continue }
            }
        } catch (e: Exception) {
            Log.e(TAG, "✘ [$key]: ${e.message}")
        }
        if (result.isNotEmpty()) Log.i(TAG, "✔ classes [$key] → ${result.size} 个")
        return result
    }

    private fun cacheFile(): File? {
        val dir = _cacheDir ?: return null
        return File(dir, CACHE_FILE)
    }

    private fun loadCache(): Boolean {
        val file = cacheFile() ?: return false
        if (!file.exists()) return false
        return try {
            val json = JSONObject(file.readText())
            if (json.optString("apk_key", "") != _apkKey) {
                file.delete()
                return false
            }
            val clsJson = json.optJSONObject("classes") ?: return false
            val keys = clsJson.keys()
            while (keys.hasNext()) {
                val k = keys.next() as String
                classNameCache[k] = clsJson.getString(k)
            }
            Log.i(TAG, "✔ 缓存加载成功 (${classNameCache.size} 条)")
            true
        } catch (_: Exception) {
            file.delete()
            false
        }
    }

    private fun saveCache() {
        if (classNameCache.isEmpty()) return
        val file = cacheFile() ?: return
        try {
            file.parentFile?.mkdirs()
            val json = JSONObject().apply {
                put("apk_key", _apkKey)
                put("classes", JSONObject().apply {
                    for ((k, v) in classNameCache) put(k, v)
                })
            }
            file.writeText(json.toString(2))
            Log.i(TAG, "✔ 缓存已保存 (${classNameCache.size} 条)")
        } catch (e: Exception) {
            Log.e(TAG, "✘ 缓存保存失败: ${e.message}")
        }
    }

    fun release() {
        saveCache()
        bridge?.close()
        bridge = null
        Log.i(TAG, "DexResolver 已释放")
    }
}
