# Architecture

## App Identity & Platform
- **App Name:** TalkToMe
- **Package Name:** `com.saiprasad.talktome`
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Min SDK:** 26 (Android 8.0) - required for `AccessibilityService` text injection and `TYPE_APPLICATION_OVERLAY` to function reliably.

## Core Components

The application is structured into four primary technical components:

### 1. Floating Bubble (Overlay Service)
- **Mechanism:** A background `Service` (ideally a Foreground Service for reliability) that renders a small circular view using `WindowManager` and `TYPE_APPLICATION_OVERLAY`.
- **Permissions:** Requires `SYSTEM_ALERT_WINDOW`.
- **UX:** 
  - Draggable across the screen to prevent obstructing underlying content (similar to Messenger Chat Heads).
  - Tapping toggles recording state.
  - Visual indicators change to show when it is actively "listening".

### 2. Audio Recorder
- **Mechanism:** Utilizes Android's `MediaRecorder` API.
- **Output:** Saves temporary audio to local storage in `.m4a` or `.wav` format.
- **Permissions:** Requires runtime `RECORD_AUDIO` permission.
- **Constraints:** Implements an automatic safety cutoff after ~30 seconds of recording to prevent runaway recordings and excessive API payloads.

### 3. Sarvam API Client
- **Mechanism:** A REST API client built with OkHttp and Retrofit.
- **Authentication:** Uses an API Key passed in the `api-subscription-key` header. The key is securely loaded from `local.properties` via `BuildConfig` to prevent exposing it in version control.
- **Payload:** Uses multipart file upload to send the local audio file to Sarvam's speech-to-text endpoint.
- **Parameters:** 
  - `model` = `"saaras:v3"`
  - `mode` = `"translit"`
- **Response Handling:** Parses the JSON response to extract the `transcript` field.

### 4. Text Injection (AccessibilityService)
- **Mechanism:** A custom `AccessibilityService` declared in `AndroidManifest.xml`.
- **Permissions:** Requires the `BIND_ACCESSIBILITY_SERVICE` permission and manual user activation in system settings.
- **Workflow:**
  1. Upon receiving a successful transcript from the API client.
  2. Traverses the screen using `rootInActiveWindow` to find the node where `isFocused && isEditable` is true.
  3. Injects the transcript using `ACTION_SET_TEXT` (API 21+).
  4. If `ACTION_SET_TEXT` is unsupported by the target app, falls back to injecting via clipboard paste (`ACTION_PASTE`).
- **Safety:** Explicitly does *not* attempt to simulate a "Send" button press.

## Permissions Required
- `RECORD_AUDIO`
- `SYSTEM_ALERT_WINDOW`
- `INTERNET`
- `BIND_ACCESSIBILITY_SERVICE`
- `FOREGROUND_SERVICE`
