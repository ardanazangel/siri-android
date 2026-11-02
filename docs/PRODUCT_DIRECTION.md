# Nova — Product direction inspired by Hex

Reference: https://github.com/anomalyco/hex (MIT). Inspiration only; no Hex code is vendored.

## Product promise
An Android voice assistant that can understand speech locally, then perform explicitly authorized phone actions. Dictation and assistant commands are different modes.

## Design principles
1. **Local-first**: download the speech model once; transcription should work offline. No background audio uploaded by default.
2. **Immediate feedback**: clear listening / transcribing / acting / failed states; show what was understood.
3. **Modular**: AudioCapture -> Transcriber -> IntentRouter -> ActionRegistry -> SpokenFeedback.
4. **Safety**: confirm sending messages, purchases, and other consequential actions; never assume success before Android confirms it.
5. **Battery-aware**: start with push-to-talk, then evaluate the default assistant role and wake-word behavior on actual hardware.
6. **Transparent**: show permissions, model sizes, microphone activity and error recovery.

## Roadmap
- P0: reproducible APK build + tests on Nothing Phone (2).
- P1: direct in-app SpeechRecognizer and explicit command schema.
- P2: offline transcription engine (benchmark candidates on Android ARM64, measure WER in Spanish, latency, RAM, model size and power).
- P3: assistant-role integration via Android APIs; verify lock-screen limitations.
- P4: optional local wake word; foreground-service restrictions, battery and privacy review.
- P5: optional cloud reasoning with opt-in and scoped action permissions.

## Acceptance criteria
- No automatic call/message sending.
- Offline voice commands work after model download (P2).
- A clear permission path and recovery UX.
- Each command has structured logs with no raw microphone recordings by default.
- No claims of always-on locked-screen support until demonstrated on target phone.

## Out of scope
Porting Hex's desktop SDK directly, copying its UI, or assuming desktop OS capabilities apply to Android.
