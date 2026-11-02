# Nova — Android voice assistant prototype

Minimal native Android proof of concept in Kotlin. No cloud keys, analytics, or third-party dependencies.

## Current capabilities
- Tap **Hablar** to invoke Android's installed speech recognition service (Spanish).
- Spoken feedback through Android TextToSpeech.
- Open camera, settings, dialer, and alarm/timer screens.
- No calls or messages are sent automatically.

## Not implemented
- Always-on hotword / background microphone / locked-screen voice activation.
- LLM-based planning, WhatsApp integration, contact lookup, or autonomous UI control.
- Accurate natural-language parsing of alarm times (currently opens the alarm screen).

## Build
Open this folder in Android Studio with Android SDK 35, then sync and run the `app` module on a physical device. Alternatively use an installed Gradle 8.11.1 distribution (`gradle assembleDebug`). No Gradle wrapper binary is included.

## Privacy
Speech recognition is delegated to the Android speech provider installed on the device, which may process audio online. No credentials are stored. Microphone permission is requested at runtime.

## Next milestones
1. Integrate `SpeechRecognizer` directly for an in-app listening UI.
2. Implement structured intent parsing and explicit confirmation for consequential actions.
3. Evaluate Android assistant role and `VoiceInteractionService` on target hardware.
4. Evaluate on-device wake word and battery/background constraints.
