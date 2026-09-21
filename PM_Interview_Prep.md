# Product Management Interview Guide: TalkToMe 🎙️

## 1. Product Overview (The "Elevator Pitch")
**TalkToMe** is a hyper-accessible, system-level Android application that brings seamless, floating voice-to-text functionality to any app. Inspired by the UX of *Wispr Flow*, it eliminates the friction of traditional dictation. The app actively monitors your screen and magically reveals a floating microphone bubble *only* when your keyboard is open. You speak, confirm, and the text is instantly injected into your chat box (WhatsApp, Telegram, etc.) using the highly accurate Sarvam AI API.

## 2. The Core Problem We Solved
- **Friction in Voice Typing:** Built-in dictation tools are clunky, prone to errors, and require you to tap multiple times to edit or send. 
- **Language Barriers:** Most default keyboards struggle with transliteration or regional dialects. We integrated Sarvam AI to provide state-of-the-art transliteration (e.g., speaking in Telugu/Hindi and getting perfect English or native script out).

## 3. Product Architecture & Engineering (How it Works)
As a PM, you don't need to write the code, but you must understand the architecture:

### A. The "Wispr Flow" Visibility Engine
**How do we know when to show the bubble?**
- We use Android's **AccessibilityService**. This is a powerful, system-level service that can "see" what is on the screen.
- **The Logic:** The app scans the Android Window Manager for `TYPE_INPUT_METHOD`. This is the exact underlying window that the Soft Keyboard uses. If the keyboard is on the screen, a background `StateFlow` (a reactive data stream) emits `true`. 
- **The Result:** The floating Jetpack Compose UI immediately fades in. If you press the home button and the keyboard disappears, the bubble instantly vanishes.

### B. The Audio Pipeline (The Hardest Technical Challenge)
**Why couldn't we just use Android's default recorder?**
- **The Issue:** Android's built-in `MediaRecorder` generates highly compressed `.m4a` files. When we sent these to Sarvam AI, their strict enterprise API instantly rejected them with a `400 Bad Request`.
- **The Engineering Fix:** We built a custom audio pipeline using low-level `AudioRecord`. This captures raw 16-bit PCM audio directly from the microphone. We then manually write a 44-byte standard `WAVE` header to create a perfectly uncompressed `.wav` file that enterprise AI APIs expect.

### C. The Injection Engine
**How does the text get into WhatsApp?**
- Once the audio is uploaded via `Retrofit/OkHttp`, Sarvam returns the translated text.
- The `AccessibilityService` kicks back in. It scans the screen for the currently focused `EditText` (text box) and fires an `ACTION_SET_TEXT` or `ACTION_PASTE` command, forcing the text directly into the input field without the user typing a single letter.

## 4. Key UX Decisions
- **Accept/Reject Buttons:** Automatic dictation is scary. We added `[ ✔ ]` and `[ X ]` buttons after recording. This puts the user in control, allowing them to reject a recording before it's sent to the cloud.
- **Visual Feedback:** Network calls take time. We replaced silent loading with an animated pulsing wave while recording, and a clear loading spinner during the API call, dramatically reducing user frustration.
- **Offline History Vault:** We implemented a local `SharedPreferences` database to save the last 10 transcripts. If the accessibility injection fails (e.g., the user closed WhatsApp too fast), their data is never lost.

## 5. Tricky Engineering Bugs We Solved

We actually solved two completely separate, tricky bugs to get everything working perfectly like Wispr Flow. Here is exactly what we did:

### 1. Why it wasn't translating (The Audio Format Bug)
**The Problem:** By default, Android's `MediaRecorder` generates compressed audio files (like `.m4a` or `.3gp`). The Sarvam AI speech-to-text API is incredibly strict and was instantly rejecting your `.m4a` files with a `400 Bad Request` error because it only understands raw, uncompressed audio. Because the network call failed instantly, the loading spinner just vanished.
**The Solution:** We completely threw out Android's `MediaRecorder` and built a custom recorder from scratch using `AudioRecord`. This allowed us to capture the raw 16-bit PCM audio stream directly from your microphone. We then manually stitched a standard 44-byte WAVE header to the front of that data, forcing the app to generate a pure `.wav` file that Sarvam happily accepts!

### 2. Why it wouldn't disappear on the home page (The Wispr Flow Bug)
**The Problem:** Initially, we told the app to show the bubble whenever you clicked inside an `EditText` (a text box). However, when you press the home button, some launchers (like Google Pixel or OnePlus) keep a hidden search bar active in the background. The app saw that hidden search bar and thought, "Ah, a text box! Keep the bubble open!"
**The Solution:** We took a page right out of Wispr Flow's playbook. Instead of looking for text boxes, we upgraded the `AccessibilityService` to scan your entire Android screen for a specific type of window: `AccessibilityWindowInfo.TYPE_INPUT_METHOD`. This is the exact underlying window that your Soft Keyboard uses. Now, the app completely ignores background text boxes and only looks at whether your keyboard is physically visible on the screen. If the keyboard goes away, the bubble goes away!

### 3. The Cherry on Top
To complete the Wispr Flow experience, we also:
- Added the `[ ✔ ]` and `[ X ]` buttons so you can accept or reject recordings instead of them firing automatically.
- Built a smooth loading spinner right into the bubble so you know exactly when the network call is happening.
- Created a local history vault so your last 10 messages are saved to your Home Screen in case WhatsApp accidentally clears the text box.

It was a tough series of bugs to crack, but we ended up building a super robust architecture.

## 6. Potential Interview Questions & Answers

**Q: "Why did you build this as an Accessibility Service instead of a custom Keyboard?"**
*A: "Custom keyboards are notoriously difficult to build from scratch (handling autocorrect, emojis, etc.). By using an Accessibility Service and a Floating Window (SYSTEM_ALERT_WINDOW), we allow users to keep their favorite keyboard (like Gboard) while overlaying our superior voice layer on top."*

**Q: "How did you handle edge cases, like the network failing?"**
*A: "We engineered the UI to be state-driven. If the API fails, the loading spinner stops and the app catches the exact `HttpException` from the server, displaying a clear error to the user rather than failing silently."*

**Q: "What would be on your roadmap for V2?"**
*A: "1. Adding local on-device transcription models (like Whisper.cpp) to reduce latency and API costs. 2. Adding a 'tone changer' (e.g., translate to English but make it sound professional). 3. iOS support (though iOS handles system-level overlays very differently, requiring a custom keyboard extension)."*
