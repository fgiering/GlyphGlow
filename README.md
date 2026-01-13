# GlyphGlow

A minimal Android app that controls the Glyph interface on Nothing devices using the Glyph Developer Kit. Designed and tested for Nothing Phone (2a) Plus, with graceful handling for other supported models.

## Features

- Three on/off switches for zones A, B, and C
- Slider for Zone C (0–24) to show progressive segments on the C ring
- Deterministic control: A/B states are applied explicitly; C progress updates do not flip A/B
- In-app status text indicating service connection, registration, and session state

## Requirements

- Nothing device on Android 14+ (Nothing’s SDK limitation)
- Foreground usage only (SDK restriction)
- Android Studio Giraffe+ with AGP 8.11.1
- JDK 17 (project is configured for Java 17 / Kotlin JVM 17)
- Material Components 1.12.0

## SDK integration

This project uses the Nothing Glyph Developer Kit:

- Repo: https://github.com/Nothing-Developer-Programme/Glyph-Developer-Kit
- SDK artifact: download `KetchumSDK_Community_YYYYMMDD.jar` from the `sdk/` folder of the repo and place it in `app/libs/`
- The build already includes `implementation(fileTree(dir: "libs", include: ["*.jar", "*.aar"]))`

### Manifest

The app manifest contains the required permission and a development key:

```xml
<uses-permission android:name="com.nothing.ketchum.permission.ENABLE" />

<application>
    <!-- Use "test" for development, replace with your real API key for production -->
    <meta-data android:name="NothingKey" android:value="test" />
</application>
```

## Build and Run

From the project root:

```bash
# Build debug APK
./gradlew :app:assembleDebug

# Install on connected device
./gradlew :app:installDebug
```

Optional (recommended for development): enable Glyph SDK debug mode on the device

```bash
adb shell settings put global nt_glyph_interface_debug_enable 1
```

Notes:

- Debug mode auto-expires after ~48h per SDK docs
- Keep the app in the foreground; background use isn’t supported

## Usage

- Launch the app
- Watch the status line:
  - "binding service…" → "service connected" → "session open"
  - If you see "register failed", see Troubleshooting
- Use switches:
  - A and B: on/off
  - C: enable the switch, then use the slider (0–24) to light progressive C segments
- Turning all switches off clears the Glyphs

## Project structure

- `app/src/main/java/.../MainActivity.kt` — GlyphManager lifecycle, on/off logic and C progress
- `app/src/main/res/layout/activity_main.xml` — Three switches (A/B/C), C slider, status text
- `app/build.gradle.kts` — Java/Kotlin 17, Material Components, local SDK inclusion from `app/libs`

## How it works

- On service connect: register for the detected device (2a Plus → `DEVICE_23113`, etc.), open session, clear any active glyphs, then apply UI state
- A/B control: build one `GlyphFrame` with A and/or B channels selected and `toggle()` once; first call `turnOff()` for determinism
- C control: render progress on the C zone using `displayProgress(cFrame, progress, false)`; does not toggle A/B

## Troubleshooting

- Status shows "register failed"
  - Ensure the manifest `<meta-data android:name="NothingKey" android:value="test" />` is present (or replace with your real key)
  - Ensure SDK JAR is present in `app/libs/`
  - Ensure device debug is enabled:
    ```bash
    adb shell settings put global nt_glyph_interface_debug_enable 1
    ```
  - App must be in the foreground
- No lights despite "session open"
  - Confirm you’re on a Nothing device with Android 14+
  - Try toggling A/B individually and set C slider > 0 with C enabled
  - If still no output, share your model/OS; we’ll verify registration target and channel mapping
- Build error about Slider attributes
  - We set Slider range in code; ensure Material Components resolves to 1.12.0 (see `gradle/libs.versions.toml`)

## Notes

- The SDK only works on Nothing devices on Android 14+
- Debug mode auto-disables after ~48h
- Only foreground apps may control glyphs

## Credits

- Nothing Glyph Developer Kit — © Nothing, used under the terms in their repository
