# ZZD Tool

An Xposed module for ZheZhengDing (ZZD), built with DexKit.

## Features

| Feature | Description |
|---------|-------------|
| Tablet Mode | Enables tablet login support |
| Anti-Recall | Prevents messages from being recalled; recalled messages display a badge |
| Forward Unlock | Removes message forwarding restrictions |
| Message Info Badge | Shows message timestamp or recall status in the chat list |

## Requirements

- A rooted Android device
- Xposed framework (LSPosed recommended)
- Android 8.0+

## Installation

1. Install LSPosed framework
2. Install this module APK
3. Enable the module in LSPosed and select ZZD (`com.alibaba.taurus.zhejiang`) as the target scope
4. Force stop and relaunch ZZD

## Settings

Open the module app to configure:

- **Feature Switches** — Enable or disable each feature individually
- **Clear Cache** — Clear DexKit scan cache (recommended after target app updates)
- **Reset Settings** — Restore all settings to defaults

## Disclaimer

This project is provided **for educational and research purposes only**. It is intended for use by individuals located **outside of mainland China**. By using this software, you acknowledge that:

- You are solely responsible for compliance with all applicable laws and regulations in your jurisdiction.
- The author assumes no responsibility or liability for any misuse of this software.
- This project does not endorse or encourage any unauthorized access, modification, or interference with any software or system in violation of applicable terms of service or laws.

## License

MIT License
