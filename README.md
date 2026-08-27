# Virtual Friend — Android Native Kotlin/Compose

A small floating Android companion using Kotlin, Jetpack Compose, a foreground service, Android's official overlay window API, DataStore, coroutines, and a modular offline chat engine.

## What is included

- Native Android app — not a web/PWA/Electron app.
- Jetpack Compose settings/onboarding UI.
- `LifecycleService` foreground service for the floating companion.
- `TYPE_APPLICATION_OVERLAY` windows with Android permission flow.
- Draggable Itachi pet and draggable summon button.
- Position persistence and screen clamping.
- Modular animation states: `IDLE`, `BLINK`, `WALK_LEFT`, `WALK_RIGHT`, `TALK`, `HAPPY`, `SLEEPY`, `SURPRISED`, `ENTER`, `EXIT`.
- Front and side reference assets derived from the supplied images.
- Automatic check-ins and water reminders.
- Quiet hours and manual pause controls.
- Compact chat panel with offline/basic responses.
- AI repository boundary with no API key stored in the APK.
- Boot receiver that attempts to restore the companion when the user has enabled it and overlay permission is still granted.
- Recovery action: Bring friend back.

## 1. Install Android Studio

Install a current stable Android Studio release from the official Android developer site. During setup, install the Android SDK Platform for API 35 and an emulator or USB device support.

## 2. Open the project

Open the `VirtualFriend` folder in Android Studio. Let Android Studio import the Gradle project and download the declared dependencies.

Recommended build environment:

- JDK 17 or the JDK bundled with your Android Studio version.
- Android SDK 35.
- Gradle 8.9+ compatible with Android Gradle Plugin 8.7.3.

This environment cannot download Gradle/Google Maven artifacts, so the project files were statically checked here rather than running a full Android build. Android Studio will perform the real dependency resolution and compilation on your machine.

## 3. Connect an Android phone

On the phone, enable Developer Options and USB debugging. Connect the phone with USB, accept the debugging prompt, select the phone in Android Studio, then press **Run**.

You can also use an Android Studio emulator.

## 4. Overlay permission

The app uses Android's official `SYSTEM_ALERT_WINDOW` permission. On first launch, tap **Enable floating friend**. Android opens the system page where you explicitly grant the permission.

The app never attempts to bypass this permission.

## 5. Foreground service

Android requires a foreground service notification for reliable long-running behavior. The service uses the Android 14+ `specialUse` foreground-service type because the app's purpose is a persistent floating companion overlay.

Notification permission is requested on Android 13+ only for the service notification. If notification permission is denied, overlay behavior can still be restricted by the device/OS policy.

## 6. Change the character asset

Assets live in:

`app/src/main/res/drawable-nodpi/`

Current files:

- `itachi_front.png` — front reference image.
- `itachi_side.png` — side reference image.

To replace the character, keep the same filenames or update the resource references in `OverlayViews.kt`.

The animation system is transform-based, so you can later add sprite frames without rewriting the service/window architecture.

## 7. Add sprite frames later

Create a small asset provider, for example:

- idle frames
- blink frames
- walk frames
- happy frames
- sleepy frames
- surprised frames

Then make the provider select a frame for each `FriendAnimationState`. The service and drag/window code do not need to change.

## 8. Change personality

The offline personality currently lives in `BasicChatEngine.kt`. Edit its response rules to change the companion's voice.

For a larger configuration system, move the personality text and message lists into a JSON/asset file and load them through a small `PersonalityConfig` class.

Default personality:

> You are a tiny mobile companion. You are friendly, slightly playful and concise. You help the user remember to drink water, stretch and take breaks, but you are not annoying or overly enthusiastic. Most responses should be 1-2 sentences. Talk like a small companion living on the user's phone, not like an AI assistant.

## 9. AI mode

`AIChatRepository.kt` deliberately contains no provider key.

Production architecture:

Android app → HTTPS secure backend → AI provider → HTTPS secure backend → Android app

Do not put an OpenAI/Gemini/Anthropic/etc. production API key into `BuildConfig`, source code, resources, or the APK. Add authentication, rate limiting, validation, and server-side secret storage to your backend.

`BackendAIChatRepository` is the swap point for the real network implementation.

## 10. Settings

The settings screen stores values with Android DataStore Preferences:

- enabled state
- friend size
- animation speed
- visit interval
- water interval
- check-ins
- sound toggle
- AI toggle
- summon button
- quiet hours
- pause state
- pet position
- summon position

Conversation history intentionally stays in memory only.

## 11. Quiet mode

Automatic visits/reminders stop during quiet hours and while paused. Manual summon remains available when the friend is enabled.

## 12. Battery optimization

Android and phone manufacturers can stop background services or delay work. Samsung, Xiaomi, Oppo, Vivo, OnePlus and other vendors may apply additional battery policies. If the companion stops after long periods, check the phone's battery/background settings for this app.

Do not disable Android security features or use undocumented bypasses.

## 13. Build an APK

In Android Studio:

1. Sync the project.
2. Select **Build → Build APK(s)**.
3. Android Studio will show the generated APK location.

For a Play Store release, create a signed App Bundle (`.aab`) and review current Google Play foreground-service and overlay policies before publishing.

## 14. Troubleshooting

### Pet does not appear

- Open Android Settings → Apps → Special app access → Display over other apps.
- Enable Virtual Friend.
- Return to the app and make sure Friend enabled is on.
- Tap Bring friend back.

### Pet disappears after reboot

- Confirm Friend enabled is on.
- Confirm overlay permission is still granted.
- Check battery/background restrictions.

### Chat keyboard does not open

- Tap the pet to open the compact chat panel.
- Make sure the device is not in a restricted input mode.
- Test on a normal Android device/emulator rather than a heavily customized ROM.

### Build errors after opening

- Let Android Studio finish Gradle dependency downloads.
- Make sure compile SDK 35 is installed.
- Use a compatible JDK.
- Run Gradle sync again.

## Project tree

```text
VirtualFriend/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/virtualfriend/
│   │   │   ├── MainActivity.kt
│   │   │   ├── chat/
│   │   │   │   ├── AIChatRepository.kt
│   │   │   │   ├── BasicChatEngine.kt
│   │   │   │   └── ChatManager.kt
│   │   │   ├── data/
│   │   │   │   └── SettingsRepository.kt
│   │   │   ├── model/
│   │   │   │   └── FriendState.kt
│   │   │   └── overlay/
│   │   │       ├── BootReceiver.kt
│   │   │       ├── FriendAnimationController.kt
│   │   │       ├── FriendOverlayService.kt
│   │   │       └── OverlayViews.kt
│   │   ├── res/
│   │   │   ├── drawable-nodpi/
│   │   │   │   ├── itachi_front.png
│   │   │   │   └── itachi_side.png
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   └── xml/backup_rules.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── README.md
```
