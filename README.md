# ZZD Tool

浙政钉（ZZD）Xposed 辅助模块，基于 DexKit 运行时反混淆。

## 功能

| 功能 | 说明 |
|------|------|
| 平板模式 | 强制启用平板登录支持 |
| 消息防撤回 | 阻止消息被撤回，撤回的消息在聊天列表显示标记 |
| 解除转发限制 | 移除消息转发限制 |
| 消息信息徽章 | 在聊天列表右下角显示消息时间或撤回状态 |

## 环境要求

- Android 8.0+
- 已 root 的设备
- Xposed 框架（推荐 LSPosed）

## 安装

1. 安装 LSPosed 框架
2. 安装本模块 APK
3. 在 LSPosed 中启用模块，勾选浙政钉（`com.alibaba.taurus.zhejiang`）作为作用域
4. 强制停止并重新打开浙政钉

## 架构

```
ZZD 通用设置页
  ┌──────────────────────────────┐
  │  ZZD Tool 设置  ← 注入的入口  │
  ├──────────────────────────────┤
  │  专注模式 / 密聊设置 / ...    │
  └──────────────────────────────┘
        ↓ 点击
  模块 MainActivity（独立进程）
        ↓ ContentProvider + MMKV 跨进程通信
  HookEntry（宿主进程）读取设置
```

### 源码结构

```
app/src/main/java/com/zzd/tool/
├── hook/
│   ├── HookEntry.kt              Xposed 入口，Application.onCreate 后初始化
│   ├── SettingEntryHook.kt       注入 ZZD 通用设置页入口
│   ├── TabletModeHook.kt         平板模式
│   ├── AntiRecallHook.kt         防撤回（hook recallStatus/shieldStatus）
│   ├── ForwardUnlockHook.kt      转发解锁（DexKit 反混淆定位）
│   ├── MessageBadgeHook.kt       消息徽章（时间戳/撤回标记）
│   └── core/
│       ├── BaseHook.kt           抽象基类（初始化、开关、错误追踪）
│       ├── HookFeature.kt        枚举（统一管理 Hook Key）
│       ├── HookUtils.kt          工具方法（hookBeforeIfEnabled 等）
│       ├── DexResolver.kt        DexKit 封装（MMKV 缓存 + 按需 bridge）
│       └── SettingsManager.kt    跨进程设置读写（ContentProvider + MMKV）
├── config/
│   └── SettingsProvider.kt       ContentProvider（MMKV 存储，宿主进程查询）
└── ui/activity/
    └── MainActivity.kt           模块设置页（MMKV 直读）
```

### 核心设计

- **延迟初始化**：所有 functional hook 在 `Application.onCreate` 后注册，确保宿主 Context 可用
- **DexKit 缓存**：MMKV 存储混淆类名映射，APK 更新后自动失效重扫
- **跨进程设置**：ContentProvider + MMKV，模块 App 写入 MMKV，宿主进程通过 ContentProvider 读取
- **BaseHook 基类**：统一管理初始化状态、开关检查、错误追踪
- **HookUtils 工具类**：封装 `hookBeforeIfEnabled` / `hookAfterIfEnabled`，运行时检查开关状态

### 防撤回机制

浙政钉的撤回逻辑是给消息打 `isRecall=true` 标记，UI 层通过 `recallStatus()` 和 `shieldStatus()` 控制显示。模块通过 hook 这两个方法返回 0 来阻止消息被隐藏。

### 设计参考

架构参照 [QAuxiliary](https://github.com/cinit/QAuxiliary) 的设计。

## 编译

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本（需配置签名）
./gradlew assembleRelease
```

### 依赖

- JDK 17+
- Android SDK（compileSdk 36）
- DexKit 2.2.0
- MMKV 1.3.9
- Xposed API 82（compileOnly）

## 设置说明

- 设置页位于模块独立 App 中，也通过「通用设置 → ZZD Tool 设置」入口访问
- 修改设置后需**重启浙政钉**生效
- DexKit 缓存在浙政钉更新后建议清除

## 免责声明

本项目仅供学习和研究用途。使用本软件即表示您同意自行承担所有法律责任。

## License

MIT License
