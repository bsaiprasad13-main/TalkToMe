# Implementation Plan (Phase-Wise)

## Phase 1: Project Initialization & Onboarding UI
**Goal:** Set up the basic project structure and ensure the user can grant all necessary permissions gracefully.

1. **Project Setup:**
   - [x] Initialize a new Kotlin Android project using Jetpack Compose.
   - [x] Set Min SDK to 26 and configure the package name to `com.saiprasad.talktome`.
   - [x] Setup `local.properties` and `BuildConfig` integration for the Sarvam API key.
2. **Onboarding Screens:**
   - [x] Build a welcoming introductory screen explaining the app's purpose (mic → Telugu → Tenglish text).
   - [x] Implement the permissions flow sequence:
     - Request `RECORD_AUDIO` (Standard runtime permission prompt).
     - Request Overlay Permission (`SYSTEM_ALERT_WINDOW`): Show an explanation and deep-link the user to `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`.
     - Request Accessibility Permission: Show a clear explanation of *why* this is needed (to type text into other apps) to mitigate the scary OS warning, then deep-link to the Accessibility Settings.
3. **State Management:**
   - [x] Implement logic to check if all three permissions are granted and transition to the active state (launching the bubble).

## Phase 2: Floating Overlay & Audio Recording
**Goal:** Implement the core user interface (the bubble) and hardware interaction (the mic).

1. **Overlay Service:**
   - [x] Create a `ForegroundService` to host the floating bubble.
   - [x] Use `WindowManager` to inflate a Jetpack Compose view (or XML if Compose in WindowManager causes issues) using `TYPE_APPLICATION_OVERLAY`.
   - [x] Implement drag-and-drop touch listeners to allow the user to move the bubble around the screen.
2. **Audio Recording Logic:**
   - [x] Implement a `MediaRecorder` wrapper class.
   - [x] Configure it to output a `.m4a` or `.wav` file to the app's cache directory.
   - [x] Add a 30-second safety timeout that automatically stops recording.
3. **UI Integration:**
   - [x] Wire the bubble tap to start/stop the `MediaRecorder`.
   - [x] Add visual feedback (icon/color change) to clearly indicate when the app is actively listening.

## Phase 3: Sarvam API Integration
**Goal:** Connect the app to the cloud to perform transliteration.

1. **Network Client Setup:**
   - [x] Add dependencies for Retrofit, OkHttp, and Gson/Moshi.
   - [x] Define the API interface mapping to Sarvam's speech-to-text REST endpoint.
2. **API Execution:**
   - [x] Implement a repository that takes the local audio file path.
   - [x] Construct the multipart request with the file, `model="saaras:v3"`, and `mode="translit"`.
   - [x] Add an OkHttp Interceptor to inject the `api-subscription-key` header.
3. **Error Handling & Parsing:**
   - [x] Parse the JSON response to retrieve the `transcript`, `language_code`, and `language_probability`.
   - [x] Handle network failures, timeouts, and invalid API keys gracefully (log errors and prepare UI feedback mechanisms).

## Phase 4: Text Injection (AccessibilityService)
**Goal:** Complete the loop by pushing the transcribed text into the target app.

1. **Service Registration:**
   - [x] Create the `TalkToMeAccessibilityService` extending `AccessibilityService`.
   - [x] Register it in `AndroidManifest.xml` with the required intent filter and meta-data XML configuration.
2. **Node Traversal & Injection:**
   - [x] Implement a method to traverse `rootInActiveWindow`.
   - [x] Locate the currently active `AccessibilityNodeInfo` that matches `isFocused() == true` and `isEditable() == true`.
   - [x] Execute `performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)` with the transcribed text.
3. **Clipboard Fallback:**
   - [x] If `ACTION_SET_TEXT` fails or is unsupported, implement a fallback that copies the transcript to the Clipboard and executes `ACTION_PASTE`.
4. **Integration:**
   - [x] Bridge the API response from the Service/Repository to the `AccessibilityService` (e.g., via Broadcast, EventBus, or shared StateFlow depending on architecture) so the text is injected immediately upon successful transcription.

## Phase 5: Polish & Testing
**Goal:** Ensure reliability across the fragmented Android ecosystem.

1. **Physical Device Testing:**
   - [x] Test end-to-end on real devices, specifically targeting aggressive OEM skins like Xiaomi (MIUI/HyperOS) and Samsung (OneUI) to ensure background services are not prematurely killed. *(Delegated to user)*
2. **Edge Case Handling:**
   - [x] Add Toast messages or bubble animations to inform the user of errors (e.g., "Network error", "No text field found", "Audio unclear").
3. **Cleanup:**
   - [x] Ensure temporary audio files are deleted after the API call completes to prevent storage bloat.
