# 👻 Phantom Call

Make your Android phone **"unavailable"** for incoming calls while LTE/5G data keeps working. (Shizuku or Root.)

## How it works

- **IMS/VoLTE deregistration** — Phantom Call detaches your device from the IMS network, so the carrier no longer routes circuit-switched calls to your line while you stay attached to the data network.
- **LTE-only modem lock** — the modem is forced into an LTE-only mode that keeps data connectivity alive without falling back to a voice-capable state.
- **Clean restore of original network mask** — when you disable the mode, the original network preference mask is restored exactly, leaving no persistent modem changes behind.

## Features

- No-root operation via **Shizuku**
- **Root auto-detection** (KernelSU / Magisk / APatch)
- **Vendor presets**: Universal, Pixel, Xiaomi, Samsung, OnePlus, vivo, Legacy
- **Dual-SIM support**
- **Custom presets** with JSON export/import
- **Quick Settings tile** for one-tap toggle
- **Home screen widget**
- **Quiet-hours schedule** for automatic on/off windows
- **Auto-off timer** to prevent forgetting the mode is active
- **Persistent status notification** with live session timer
- **Diagnostics view** and command log
- **RU/EN interface**
- **Material 3** design

## Requirements

| Requirement | Minimum | Recommended |
|---|---|---|
| Android | 8.0+ | 12+ |
| Privilege | Shizuku v10+ **or** root | Shizuku v10+ |
| Carrier | VoLTE-capable | VoLTE active on your plan |

## Quick start

1. Install [Shizuku](https://shizuku.rikka.app/) and start it on your device.
2. Open Phantom Call and grant the Shizuku permission.
3. Toggle the mode **ON**.
4. Verify by calling yourself — you should hear "number unavailable" instead of a ring, while mobile data keeps working.

## Building from source

- JDK 17
- Android SDK 35

```bash
./gradlew assembleDebug
```

The debug APK is written to:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Safety & disclaimer

- **Emergency calls are not guaranteed** while the mode is active — always test and keep a secondary way to reach emergency services.
- SMS delivery may be **delayed** while messaging is routed over IMS.
- Conditional call forwarding rules on your carrier side may still route calls to voicemail even when your line reports unavailable.
- Use this app on your own hardware and at your own risk. Make sure your usage complies with your local regulations and your carrier's terms of service.

## License

[MIT](LICENSE)