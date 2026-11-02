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


## Reconocimiento local con Parakeet (experimental)

Nova utiliza ahora `SpeechRecognizer` conectado **solo** al servicio Android de Parakeetype, si está instalado. No utiliza la ventana de voz de Google ni cambia silenciosamente a un proveedor online.

1. Instala [Parakeetype desde F-Droid](https://f-droid.org/es/packages/org.schabi.parakeetype/).
2. Descarga e importa el modelo Parakeet desde las instrucciones de Parakeetype (aprox. 630 MB).
3. Concede los permisos de micrófono necesarios y comprueba que Parakeetype transcribe por sí mismo.
4. Instala la nueva APK de Nova desde GitHub Actions y pulsa **Hablar**.

**Limitación:** es una integración con un servicio local externo, no un modelo embebido en Nova. La disponibilidad y compatibilidad del servicio dependerán de la versión de Parakeetype instalada. Si el servicio no se detecta, Nova lo indicará sin recurrir a Google. La integración todavía necesita prueba real en Android.
