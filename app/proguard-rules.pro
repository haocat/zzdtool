# Xposed 相关
-keep class de.robv.android.xposed.** { *; }
-keep class * implements de.robv.android.xposed.IXposedHookLoadPackage { *; }

# Hook 类
-keep class com.zzd.tool.hook.** { *; }

# DexKit
-keep class org.luckypray.dexkit.** { *; }

# MMKV
-keep class com.tencent.mmkv.** { *; }

# FreeReflection
-keep class me.weishu.reflection.** {*;}

# Kotlin Intrinsics
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static *** throwUninitializedProperty(...);
    public static *** throwUninitializedPropertyAccessException(...);
}

# ViewBinding
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    *** inflate(android.view.LayoutInflater);
}

-keep class * extends android.app.Activity
-keep class * implements androidx.viewbinding.ViewBinding {
    <init>();
    *** inflate(android.view.LayoutInflater);
}
