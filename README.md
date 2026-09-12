# Android Multicast Video Receiver

A hybrid Android application demonstrating low-latency IP multicast video reception over wireless local area networks (WLAN). The system couples a low-level native networking engine written in **Rust** (leveraging [mcrx-core](https://github.com/QUICast/mcrx-core/tree/main) and Mozilla's `UniFFI`) with a modern **Kotlin** frontend powered by **Google Media3 (ExoPlayer)**.

---

## Quick Start (Run in 2 Minutes)

> [!IMPORTANT]
> **Physical Device Required for Live Video:**  
> Native binaries for **ARM64** (`arm64-v8a`), **ARMEABI** ('armeabi-v7a') and **x86_64 emulators** (`x86_64`) are pre-compiled and committed to this repository. The app will install and open cleanly on an Android Studio emulator without crashing.  
> **However, live multicast video will NOT play inside an emulator.** Android Studio emulators operate behind an isolated virtual NAT network (`10.0.2.15`) that filters out local Layer-2 WiFi multicast (IGMP) packets from the host machine.  
> **To evaluate live video playback, you must run the app on a physical Android device connected to the same local WiFi network as the streaming PC.**
> **You can find a ready to install .apk file in release section in the side bar on the right side**
---

> **Note for Evaluators:**  
> All native shared libraries (`.so` files) and UniFFI Kotlin bindings are **already pre-compiled and placed in the project**.  
> **You do NOT need to install Rust, Cargo, or the Android NDK to test the application.**

### 1. Run the App via Android Studio
1. Connect a physical Android phone to your computer via USB or WLAN (enable USB or WLAN Debugging).
2. Connect your phone to the **same local WiFi network** as your streaming computer (do not use guest networks).
3. Open the `android_app/` folder in **Android Studio**.
4. Allow Gradle to sync, then click **Run** (`Shift + F10`) to deploy the app to your device.

### 2. Start the Multicast Stream from PC
In a terminal/PowerShell on your PC (with [FFmpeg](https://ffmpeg.org/) installed), broadcast an MP4 video file:
```powershell
# Replace 192.168.178.52 with your PC's actual local WiFi IPv4 address
ffmpeg -re -i .\sample_video.mp4 -c copy -f mpegts "udp://239.1.2.3:5000?pkt_size=1316&ttl=4&localaddr=192.168.178.52"
```

### 3. View the Stream
- Open **MalikVideoPlayer** on your phone.
- The default multicast IP (`239.1.2.3`) and Port (`5000`) are pre-filled.
- Tap **Start Streaming** — playback will begin within 1–2 seconds!

---
*(If you want to modify, recompile, or inspect the native Rust core from source, see the full developer guide below.)*
---


## 1. Prerequisites & Toolchain (For Development / Rebuilding)

If you plan to modify or rebuild the native Rust components from scratch:

### Host OS Requirements
- **Windows 10/11**, **Linux**, or **macOS**
- **Git**

### Android Development
- **Android Studio** (Ladybug | 2024.2.1 or newer recommended)
- **Android SDK** & **Android NDK** (API Level 24+ minimum required)
- Android physical test device (Android 7.0 / API 24 or newer, connected to the local WiFi network)

### Rust Toolchain
- **Rust stable** (`rustc` & `cargo`) installed via [rustup.rs](https://rustup.rs/)
- Android compilation targets:
  ```bash
  rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
  ```
- **cargo-ndk** (NDK compilation helper):
  ```bash
  cargo install cargo-ndk
  ```
- **uniffi-bindgen** (CLI tool matching UniFFI version `0.28.x`):
  ```bash
  cargo install uniffi-bindgen --version 0.28.3
  ```

### Media Streamer
- **FFmpeg** installed and accessible from terminal.

---

## 2. Project Structure

```

MalikVideoPlayer/                      # Android Studio project
├── rust_core/                         # Native Rust project (Not a part of the android project)
│   ├── Cargo.toml                     # Dependencies (mcrx-core, uniffi, socket2)
│   ├── Cargo.lock
│   ├── src/
│   │   ├── lib.rs                     # MulticastReceiver & UniFFI exported bindings
│   │   ├── main.rs                    # Desktop CLI receiver used for initial testing
│   │   └── bin/
│   │       └── uniffi-bindgen.rs      # Local binding generator CLI hook
│   └── mcrx-core/                     # Core receiver library crate
│
├── app/
│   ├── build.gradle.kts           # Dependencies (JNA, Media3/ExoPlayer)
│   └── src/main/
│       ├── AndroidManifest.xml    # Permissions & orientation configChanges
│       ├── java/com/example/malikvideoplayer/
│       │   ├── MainActivity.kt    # Startup screen & MulticastLock acquisition
│       │   ├── PlayerActivity.kt  # Media playback, lifecycle & recording controls
│       │   ├── RustMulticastDataSource.kt # Custom Media3 BaseDataSource implementation
│       │   └── uniffi/...         # Generated UniFFI Kotlin bindings
│       ├── jniLibs/               # Pre-compiled native libraries
│       │   ├── arm64-v8a/libmy_multicast_test.so   # Physical 64-bit devices
│       │   ├── armeabi-v7a/libmy_multicast_test.so # 32-bit devices (e.g. Fire TV)
│       │   └── x86_64/libmy_multicast_test.so      # Android Studio Emulators
│       └── res/layout/
│           ├── activity_main.xml
│           └── activity_player.xml
└── build.gradle.kts
```

---

## 3. Build Instructions (Rebuilding from Source)

### Step 1: Compile the Native Rust Library

Navigate to the `rust_core` directory:
```bash
cd rust_core
```

Compile the shared dynamic library (`.so`) using `cargo-ndk`. Ensure you specify `--platform 24` or higher (mandatory for `getifaddrs` support on Android) and use `--lib`:

**For 64-bit ARM physical devices (e.g., modern Samsung Galaxy, Pixel):**
```bash
cargo ndk --target aarch64-linux-android --platform 24 build --lib --release
```

**For x86_64 Android Studio Emulators:**
```bash
# Optional 16 KB page-size linker flag for Android 15 compatibility:
$env:RUSTFLAGS="-C link-arg=-z -C link-arg=max-page-size=16384"
cargo ndk --target x86_64-linux-android --platform 24 build --lib --release
```

*(Optional) For 32-bit ARM devices (e.g., Fire TV Stick):*
```bash
cargo ndk --target armv7-linux-androideabi --platform 24 build --lib --release
```

---

### Step 2: Generate Kotlin Bindings via UniFFI

From the `rust_core` folder, run `uniffi-bindgen`:

```bash
cargo run --features=uniffi/cli --bin uniffi-bindgen generate \
    --library target/aarch64-linux-android/release/libmy_multicast_test.so \
    --language kotlin \
    --out-dir ./generated
```

---

### Step 3: Deploy Assets to Android Studio

1. **Deploy `.so` libraries:**
   Copy the generated shared object files into your Android module's `jniLibs` directory:
    - Copy `target/aarch64-linux-android/release/libmy_multicast_test.so` to:
      `MalikVideoPlayer/app/src/main/jniLibs/arm64-v8a/libmy_multicast_test.so`
    - Copy `target/x86_64-linux-android/release/libmy_multicast_test.so` to:
      `MalikVideoPlayer/app/src/main/jniLibs/x86_64/libmy_multicast_test.so`

2. **Deploy Kotlin Bindings:**
    - Copy the generated `.kt` file from `rust_core/generated/` into:
      `android_app/app/src/main/java/com/example/malikvideoplayer/`

3. **Verify App Dependencies (`app/build.gradle.kts`):**
   ```kotlin
   dependencies {
       // Java Native Access (JNA) for loading native UniFFI binaries
       implementation("net.java.dev.jna:jna:5.14.0@aar")

       // AndroidX Media3 (ExoPlayer)
       implementation("androidx.media3:media3-exoplayer:1.4.1")
       implementation("androidx.media3:media3-ui:1.4.1")
       implementation("androidx.media3:media3-common:1.4.1")
   }
   ```

---

## 4. Network Setup & Router Configuration

Multicast requires explicit local network support:

1. **Same Subnet:** The streaming PC and the Android receiver must be on the same local subnet (e.g., `192.168.178.0/24`).
2. **Access Point Settings (e.g., AVM Fritz!Box):**
    - Ensure **"Allow wireless devices to communicate with each other"** (AP isolation OFF) is checked.
    - Under WiFi settings, ensure **"Optimize WiFi transmission for Live TV"** (IGMP Snooping) is active.
    - Do **NOT** use a Guest WiFi network, as client isolation and multicast filters are strictly enforced.
3. **Host Firewall:** Ensure outgoing UDP traffic on port `5000` is permitted through your host firewall (Windows Defender / ufw).

---

## 5. Running the Demo

### Step 1: Start the Media Streamer (PC)

Run FFmpeg to encapsulate a video file into an MPEG-TS container and broadcast it over UDP:

```powershell
# Replace 192.168.178.52 with your PC's local WiFi IPv4 address
ffmpeg -re -i .\sample_video.mp4 -c copy -f mpegts "udp://239.1.2.3:5000?pkt_size=1316&ttl=4&localaddr=192.168.178.52"
```

**Key Flag Explanations:**
- `-re`: Reads input at native frame rate (simulates real-time broadcast).
- `-c copy`: Streams without re-encoding, avoiding CPU bottlenecks and quality loss.
- `-f mpegts`: Uses 188-byte MPEG-TS packets with sync bytes (`0x47`) to allow instant mid-stream resynchronization.
- `pkt_size=1316`: Packages exactly 7 x 188 bytes per UDP datagram (940-byte payload), well below standard 1500-byte MTU to prevent IP fragmentation.
- `ttl=4`: Time-To-Live ensures frames are routed across local switches and APs.
- `localaddr=...`: Explicitly binds outgoing traffic to the active WiFi network interface card.

---

### Step 2: Run the Android App

1. Build and install the app from Android Studio onto your physical device:
   ```bash
   ./gradlew installDebug
   ```
2. Open **MalikVideoPlayer** on the device.
3. The default multicast IP (`239.1.2.3`) and port (`5000`) are pre-filled. Edit them if you changed your streamer's destination.
4. Tap **Start Streaming**.
5. The application:
    - Acquires the `WifiManager.MulticastLock`.
    - Resolves the local WiFi IP of the device.
    - Instantiates native Rust `MulticastReceiver` to join the group via IGMP.
    - Feeds incoming MPEG-TS packets directly into ExoPlayer using the custom `RustMulticastDataSource`.

---

## 6. Application Features & UI Controls

- **Zero-Drop Direct RAM Bridge:** Incoming UDP datagrams are batched in native memory (up to 64 KB) and consumed directly via a non-blocking `RustMulticastDataSource`, eliminating disk I/O bottlenecks.
- **Screen Rotation Persistence:** `PlayerActivity` registers `android:configChanges` in the manifest, allowing seamless rotation without socket teardown or stream interruption.
- **Auto-Hiding Live Recording:**
    - Tap the screen while video is playing to display the semi-transparent **REC** button.
    - Tapping **REC** streams incoming raw MPEG-TS data asynchronously into a `.ts` file saved to the device's public `Downloads/` folder.
    - Tapping **STOP** finishes the stream and automatically invokes Android's `MediaScannerConnection` so the recording is immediately visible in file managers.
- **Keep-Screen-On Mode:** Playback window sets `FLAG_KEEP_SCREEN_ON` to prevent dimming or display sleep during active playback.

---

## 7. Troubleshooting

| Issue / Symptom | Underlying Cause | Resolution |
| :--- | :--- | :--- |
| `MulticastJoinFailed: Os { code: 19, message: "No such device" }` | Android kernel does not have a default route for the multicast group. | Ensure the phone's local WiFi IP is dynamically resolved and supplied to the Rust constructor to bind `config.interface`. |
| App crashes on launch with `UnsatisfiedLinkError` | Native `.so` architecture mismatch. | Verify the target device architecture (`adb shell getprop ro.product.cpu.abi`) matches the folder inside `jniLibs/` (e.g., `arm64-v8a` or `x86_64`). |
| App opens on emulator but video never plays | Emulator virtual NAT router (`10.0.2.x`) blocks local host multicast. | Use a physical Android device connected to the same local WiFi network as the streaming host. |
| Stream connects but shows 0 received bytes | Packets dropped by WiFi power-saving or AP isolation. | Verify `MulticastLock.acquire()` is called in `MainActivity`; ensure router AP isolation is turned off; verify sender's local interface parameter (`localaddr=...`). |
| Video displays heavy macroblocking / pixelation | UDP packet loss over WiFi or MTU fragmentation. | Set `-pkt_size 1316` in FFmpeg; ensure PC is connected via Ethernet or close to a 5 GHz access point; confirm batching is implemented in `read_packets_batch()`. |
| Recorded `.ts` file not visible in "My Files" | Scoped storage index has not indexed the file yet. | Ensure `MediaScannerConnection.scanFile()` is triggered upon stopping the recording. |

---

