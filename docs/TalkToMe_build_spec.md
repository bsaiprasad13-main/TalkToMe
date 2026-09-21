# TalkToMe — Build Spec for Antigravity

## What this app does
A floating mic bubble (like Wispr Flow) that sits on top of any app. Tap it,
speak in Telugu, and it types the romanized ("Tenglish") version directly
into whatever text field is focused — primarily for fast WhatsApp typing.

Example: user speaks "ఏం చేస్తున్నావ్?" out loud → app types
"Em chesthunnaav?" into the WhatsApp message box.

This has already been validated end-to-end via a Python test script using
Sarvam AI's speech-to-text API in translit mode — the API reliably returns
correctly romanized Telugu text with high language-detection confidence.
This spec is for porting that validated approach into a native Android app.

---

## App identity
- **App name:** TalkToMe
- **Package name:** com.saiprasad.talktome
- **Language:** Kotlin
- **UI framework:** Jetpack Compose (consistent with prior app, Sensei)
- **Min SDK:** 26 (Android 8.0) — required for AccessibilityService text
  injection and overlay windows to work reliably

---

## Core architecture — 4 components

### 1. Floating Bubble (Overlay Service)
- A `Service` that adds a small circular view to the screen via
  `WindowManager`, using `TYPE_APPLICATION_OVERLAY`.
- Requires `SYSTEM_ALERT_WINDOW` permission (user must grant this manually
  in system settings — there's a standard Android flow for this, show the
  user a button that deep-links to the permission screen).
- Bubble is draggable (like Messenger chat heads) so it doesn't block content.
- Tap = start recording (bubble changes color/icon to show "listening").
- Tap again = stop recording and trigger transcription.

### 2. Audio Recorder
- Use `MediaRecorder` to record to a local `.m4a` or `.wav` file while the
  bubble is in "listening" state.
- Requires `RECORD_AUDIO` permission (runtime permission prompt).
- Recording should auto-stop after a max duration (~30s) as a safety limit.

### 3. Sarvam API Client
- **No official Android/Kotlin SDK exists** — Sarvam's SDK is Python/Node
  only. On Android, call their REST API directly using OkHttp or Retrofit
  with a multipart file upload.
- Endpoint: speech-to-text transcription endpoint (same one used in the
  validated Python test) — confirm exact URL from Sarvam's API Reference
  page (indus.sarvam.ai/developer) since REST paths can differ slightly
  from the SDK's internal routing.
- Required params, matching what was already validated in Python:
  - `model = "saaras:v3"`
  - `mode = "translit"`
  - `file` = the recorded audio file (multipart)
- Auth header: `api-subscription-key: <API_KEY>` (same key from the
  Sarvam dashboard used in testing)
- Store the API key in `local.properties` / `BuildConfig`, NOT hardcoded
  in source — this is a portfolio project that may end up on GitHub.
- Parse the JSON response for the `transcript` field (confirmed field
  name from the validated test run — response included
  `transcript='Em chesthunnaav'`, `language_code='te-IN'`,
  `language_probability=0.957`).

### 4. Text Injection (AccessibilityService)
- Register a custom `AccessibilityService` in the manifest with
  `BIND_ACCESSIBILITY_SERVICE` permission.
- User must manually enable this service in
  Settings → Accessibility → TalkToMe (standard Android flow — deep-link
  the user there with a clear in-app prompt explaining why).
- On receiving the transcribed text:
  1. Find the currently focused editable node via
     `rootInActiveWindow` → search for `isFocused && isEditable`.
  2. Use `ACTION_SET_TEXT` (API 21+) to insert the text, or fall back to
     clipboard-paste (`ACTION_PASTE`) if `ACTION_SET_TEXT` isn't
     supported on the target field.
  3. Do NOT auto-send — just insert the text and let the user review
     before hitting send in WhatsApp. (Safety: avoids accidental sends
     from misheard audio.)

---

## Permissions checklist (AndroidManifest.xml)
- `RECORD_AUDIO`
- `SYSTEM_ALERT_WINDOW`
- `INTERNET`
- `BIND_ACCESSIBILITY_SERVICE` (on the AccessibilityService declaration)
- `FOREGROUND_SERVICE` (if the bubble service runs as a foreground
  service for reliability — recommended, same pattern as Sensei's
  StatusService)

---

## User onboarding flow (first launch)
1. Explain what the app does in one screen (mic → Telugu → Tenglish text).
2. Request `RECORD_AUDIO` permission.
3. Prompt to enable overlay permission (deep-link to system settings).
4. Prompt to enable the AccessibilityService (deep-link to system settings,
   with a short explanation of *why* this permission is needed — Android
   shows scary-sounding accessibility warnings, so context matters here).
5. Show the floating bubble once all three are granted.

---

## Known constraints / things to flag during build
- AccessibilityService text injection can behave differently across
  Android versions/OEM skins (same class of issue you hit with Sensei's
  call screening — Xiaomi/Samsung sometimes restrict background
  services more aggressively). Test on a real device, not just emulator.
- Sarvam's translit romanization style may not always match a given
  user's personal typing habits (e.g. "unnavu" vs "unnav") — this is a
  model behavior, not a bug, and doesn't need to be "fixed," just
  expected.
- No auto-send, by design — reduces risk from misheard audio being sent
  before review.

---

## Already validated (don't redo this step)
Python test confirmed the Sarvam API call and response shape work as
expected:
```python
from sarvamai import SarvamAI

client = SarvamAI(api_subscription_key="...")
response = client.speech_to_text.transcribe(
    file=open("myvoice_1.ogg", "rb"),
    model="saaras:v3",
    mode="translit",
)
# response.transcript == 'Em chesthunnaav'
# response.language_code == 'te-IN'
# response.language_probability == 0.957
```
Antigravity should treat this as the reference for what the REST call
needs to replicate in Kotlin — same params, same expected response shape.
