# TalkToMe 🎙️

**TalkToMe** is an intelligent, accessibility-driven Android application inspired by the UX of *Wispr Flow*. It provides a seamless voice-to-text experience that instantly floats above any app when you open your keyboard. Speak, confirm, and watch your translated text magically inject into your active chat (like WhatsApp, Telegram, etc.)!

Built using modern **Jetpack Compose** and **Kotlin Coroutines**, TalkToMe integrates the powerful **Sarvam AI** speech-to-text API for highly accurate transliteration and translation.

---

## 🚀 Features
- **Context-Aware Floating UI (Wispr Flow Clone):** The microphone bubble is completely invisible until you actually need it. The app monitors the Android Window Manager and instantly reveals the floating mic only when a Soft Keyboard is actively on screen.
- **Smart Audio Encoding:** Bypasses Android's standard compressed formats to capture raw 16-bit PCM audio, manually constructing a standard `.wav` header. This ensures strict API compliance with backend AI systems (like Sarvam).
- **Accessibility Text Injection:** Uses Android's `AccessibilityService` to intelligently find the currently focused `EditText` and programmatically inject the translated text, entirely replacing the need to manually type. If it can't find the field, it safely falls back to your clipboard.
- **Micro-interactions & UX:** Features an animated pulsing wave while recording, a smooth horizontal expansion to reveal Accept/Reject buttons, and a loading spinner for instant network feedback.
- **Local History Vault:** Offline-first architecture saves your last 10 successful transcriptions locally using `SharedPreferences`, accessible via the sleek Home Screen.

---

## ✨ The Magic: Transliteration (Telugu Audio to Roman Script)
TalkToMe breaks down typing barriers for regional languages. By leveraging Sarvam AI's `translit` mode, it performs direct script conversion from spoken Telugu into casual English letters.

| You speak (Telugu audio) | App types into WhatsApp (Roman script) |
| :--- | :--- |
| ఏం చేస్తున్నావ్? | em chesthunnav? |
| ఎక్కడ ఉన్నావ్? | ekkada unnav? |
| తిన్నావా? | thinnava? |
| నాకు ఆకలిగా ఉంది | naaku aakaliga undi |
| రేపు కలుద్దామా? | repu kaluddama? |
| నువ్వు ఇంటికి ఎప్పుడు వస్తున్నావ్? | nuvvu intiki eppudu vastunnav? |
| సరే, తర్వాత మాట్లాడదాం | sare, tarvatha matladdam |
| నాకు ఇది అస్సలు నచ్చలేదు | naaku idi assalu nachaledu |

So it's not translating meaning into English (*"what are you doing?"*) — it's keeping the Telugu words, Telugu grammar, Telugu sentence structure, just written with English letters, the way you and your friends already text casually. Same slang, same colloquial contractions (like *"chesthunnav"* not the formal *"cheyuchunnavu"*), exactly how you'd naturally type it by hand if you weren't in a hurry.

That's the target output the Sarvam `translit` mode is meant to produce directly from your spoken Telugu — no separate translation step, no meaning-conversion, just script conversion.

---

## 💡 How to Use the App (Example Workflow)
TalkToMe is designed to be completely invisible until you need to type something. 

**Example Workflow:**
1. **Open an App:** You open WhatsApp (or Telegram, Chrome, etc.) and tap on the text box.
2. **Keyboard Appears:** Your standard Android keyboard (like Gboard) slides up. TalkToMe detects the keyboard and magically reveals a small floating microphone bubble on the edge of your screen.
3. **Record:** You tap the bubble and speak (e.g., *"Bhojanam chesava?"*). The bubble pulses to show it is listening.
4. **Process:** Tap the stop button. A loading spinner appears while Sarvam AI processes and transliterates your voice in real-time.
5. **Confirm & Inject:** The bubble expands to show `[ ✔ ]` and `[ X ]`. When you tap `✔`, the transliterated text (*"Bhojanam chesava?"*) is instantly typed into the WhatsApp text box without you touching the keyboard!
6. **Vanish:** You press the back button or send the message, the keyboard disappears, and the floating bubble instantly vanishes out of your way.

---
## 🛠️ How it Works (Under the Hood)
1. **Accessibility Observer:** `TalkToMeAccessibilityService` constantly monitors the screen state. When it detects a window of type `TYPE_INPUT_METHOD` (the keyboard), it broadcasts an event via a Kotlin `StateFlow`.
2. **Floating Window Manager:** `BubbleService` (a Foreground Service) observes this state. When true, it uses Jetpack Compose inside a `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` to draw the floating UI.
3. **Custom Audio Pipeline:** When you tap the mic, a custom `AudioRecord` implementation streams raw microphone data to a temporary file, bypassing the standard `MediaRecorder`. When stopped, it prepends a 44-byte WAVE header to create a pure `.wav` file.
4. **Network & Injection:** The app sends the `.wav` to Sarvam's API using `Retrofit` and `OkHttp`. Upon success, the translated text is sent back to the `AccessibilityService`, which uses `ACTION_SET_TEXT` or `ACTION_PASTE` to forcefully inject it into the active text box.

---

## 💻 How to Deploy & Run
To install this on your personal Android device, you'll need a computer and a USB cable.

### Prerequisites
1. Download and install **[Android Studio](https://developer.android.com/studio)**.
2. Enable **Developer Options** and **USB Debugging** on your Android phone.
3. Obtain a Sarvam AI API Key (from the Sarvam dashboard).

### Setup Instructions
1. Clone or download this repository to your computer.
2. Open **Android Studio**, click `Open`, and select the `TalkToMe` folder.
3. Let Gradle sync and download all dependencies.
4. **API Key Setup:**
   In the root of the project, open the `local.properties` file (or create it if it doesn't exist). Add the following line, replacing `your_api_key_here` with your actual Sarvam API key:
   ```properties
   SARVAM_API_KEY=your_api_key_here
   ```
   *(Note: `local.properties` is gitignored so your key remains safe).*
5. Connect your phone via USB. You should see your device name at the top of Android Studio.
6. Click the green **Play/Run** button (Shift + F10).
7. The app will install on your phone. When prompted, you MUST grant the app:
   - Microphone Permission
   - Display Over Other Apps Permission
   - Accessibility Service Permission

### 🤖 Troubleshooting with AI IDEs
Deploying Android apps can sometimes result in environment issues (like Gradle version mismatches, Java SDK errors, or manifest conflicts). 

If you encounter any build errors, **highly recommend** opening the project folder in an AI-first IDE like:
- **[Google Antigravity (AGY)](https://aistudio.google.com/)**
- **[Cursor](https://cursor.sh/)**

Simply paste the build error into the AI chat. Because the AI has full context of the project files, it can instantly pinpoint the fix (e.g., "Change the target SDK to 34" or "Update the Kotlin compiler version") rather than you having to search StackOverflow.
