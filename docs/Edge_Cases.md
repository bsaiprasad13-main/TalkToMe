# Edge Cases & Constraints

When developing and testing TalkToMe, the following edge cases and system constraints must be accounted for to ensure a robust user experience.

## 1. Operating System & OEM Constraints
- **Aggressive Battery Management:** Devices from manufacturers like Xiaomi, Samsung, and OnePlus often kill background services (including Overlay and Accessibility services) to save battery. 
  - *Mitigation:* Run the Overlay as a `ForegroundService` with a persistent notification. Educate the user to exclude the app from battery optimization if they experience drops.
- **Accessibility Service Revocation:** Android may occasionally disable accessibility services for security reasons or during OS updates.
  - *Mitigation:* The app (or bubble) should detect if the service is currently enabled before attempting injection, and prompt the user to re-enable it if necessary.

## 2. Text Injection Challenges
- **No Focused Text Field:** The user taps the bubble, speaks, but no editable field is currently focused on the screen.
  - *Mitigation:* Detect this state quickly. Show a subtle toast or visual cue (e.g., bubble turns red briefly) saying "No text field found." Fall back to copying the text to the clipboard so the user's speech isn't entirely lost.
- **ACTION_SET_TEXT Unsupported:** Some custom UI frameworks or specific apps might not support the standard `ACTION_SET_TEXT` accessibility command.
  - *Mitigation:* Implement a robust fallback to `ACTION_PASTE`. Note that this will briefly overwrite the user's clipboard contents.
- **Cursor Position:** By default, replacing the whole text might erase what the user already typed. 
  - *Mitigation:* Ideally, append the text to the existing content of the text node or insert it at the current cursor position if the API allows.

## 3. Audio & Network Issues
- **Recording Fails to Start:** The microphone is currently locked by another app (e.g., the user is on a phone call).
  - *Mitigation:* Catch the `MediaRecorder` exception and show an error to the user ("Microphone in use").
- **Long Audio / Runaway Recording:** The user forgets to tap the bubble a second time to stop recording.
  - *Mitigation:* Implement a hard timeout (max duration ~30s). When hit, auto-stop and process the audio gathered so far.
- **Network Timeout / No Internet:** The Sarvam API call fails or times out.
  - *Mitigation:* Indicate a failure state on the bubble UI and clean up the temporary audio file. Do not crash.
- **Empty Transcription:** The API returns an empty transcript (e.g., user recorded silence or background noise).
  - *Mitigation:* Ignore the injection step. Optionally show a "Didn't catch that" visual cue.

## 4. API & Data Handling
- **Transliteration Discrepancies:** Sarvam's AI model might romanize words differently than the user's personal preference (e.g., "unnavu" vs "unnav").
  - *Mitigation:* Document this as expected behavior (model trait, not a bug). Since the app explicitly avoids auto-sending, the user always has the opportunity to manually correct slight discrepancies before hitting send.
- **Storage Leaks:** `.m4a` or `.wav` files piling up in storage.
  - *Mitigation:* Ensure files are written to the app's `cacheDir` and are explicitly deleted immediately after a successful or failed API upload.
