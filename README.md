# TalkToMe 🎙️

**TalkToMe** is an intelligent, accessibility-driven Android application inspired by the UX of *Wispr Flow*. It provides a seamless voice-to-text experience that instantly floats above any app when you open your keyboard. Speak, confirm, and watch your translated text magically inject into your active chat (like WhatsApp, Telegram, etc.)!

Built using modern **Jetpack Compose** and **Kotlin Coroutines**, TalkToMe integrates the powerful **Sarvam AI** speech-to-text API for highly accurate transliteration and translation.

---

## 🎯 What Problem Does It Solve?

Imagine you are chatting on WhatsApp and want to send a message in your native language (like Telugu, Hindi, etc.), but you want to type it casually using English letters. Typing this out manually takes time and effort. 

**TalkToMe solves this by letting you just speak naturally.** Without ever leaving your current screen, it listens to your voice and instantly types out the words in English letters right into your text box.

**Why existing options fall short:**
Standard tools like built-in keyboard dictation (e.g., Gboard or Apple Keyboard) or translation apps miss the mark. They either force you to output text in the actual native script (which can be clunky to read), or they fully translate the meaning to English (changing your words entirely to "Where are you?"). They completely fail at *transliteration*—letting you speak your regional dialect and outputting those exact words in casual English letters.

**What about Wispr Flow?**
While TalkToMe is heavily inspired by Wispr Flow's amazing UX, even Wispr Flow leaves a gap here. Wispr Flow currently has a dedicated "Hinglish" setting that romanizes Hindi perfectly. However, for Telugu (and most other Indian languages), a romanized-output option simply doesn't exist yet in their app. Wispr Flow works great for Hindi speakers, but TalkToMe fills this massive gap by offering perfect "Tenglish" (and similar) transliteration for the rest of the regional languages!

**Simple Example:**
You are in WhatsApp and want to say *"Where are you?"* in Telugu. 
Instead of awkwardly typing it out on your keyboard:
1. You just tap the floating mic and speak: **"ఎక్కడ ఉన్నావ్?"**
2. The app instantly types: **"ekkada unnav?"** directly into your WhatsApp chat!

It doesn't translate the meaning into English; it keeps your natural slang, grammar, and exact words, but writes them exactly the way you casually text with your friends.

## 🤝 How TalkToMe relates to Kivi (Sarvam AI)

TalkToMe is built on Sarvam AI's Saaras v4 speech model, the same underlying model family that powers **Kivi**, Sarvam's own voice application (built in partnership with HP, announced at Sarvam Epoch). Both projects share the same foundation: Sarvam's speech recognition stack for Indian languages, including transliterated (romanized) output.

Where they differ is platform, scope, and specific use case:

| | TalkToMe | Kivi |
| :--- | :--- | :--- |
| **Platform** | Android (mobile) | Mac and windows only, (Android, API, and MCP access announced as "next") |
| **Interaction model** | Floating mic bubble that overlays any app | Voice interface across desktop apps |
| **Primary output** | Romanized text (Tenglish-style), typed directly into the focused text field | Broader: dictation, drafting, rewriting, search, and task assistance |
| **Scope** | Narrow and specific: fast, romanized voice-to-text for chat apps like WhatsApp | Broad: voice as a general interface across Docs, spreadsheets, code, and more |
| **Built by** | Personal project, using Sarvam's public API | Sarvam AI's own first-party product |

In short: **Kivi** is Sarvam's platform-level bet on voice as the primary way people interact with a computer. **TalkToMe** is a focused, single-purpose tool solving one specific gap: fast, romanized-script voice typing for Indian-language chat, on Android, where Sarvam's own product isn't available yet.

TalkToMe isn't a competing product to Kivi. It's a small, personal solution to a problem I feel every day, built on the same underlying technology Sarvam is using to solve a much bigger one.

---

## 🚀 Features
- **Context-Aware Floating UI (Wispr Flow Clone):** The microphone bubble is completely invisible until you actually need it. It appears docked to the screen edge just above the keyboard the moment a soft keyboard opens, and hides when it closes. Like Wispr Flow, it stays out of password, PIN, number and phone fields, and remembers the edge and height you drag it to.
- **Smart Audio Encoding:** Bypasses Android's standard compressed formats to capture raw 16-bit PCM audio, manually constructing a standard `.wav` header. This ensures strict API compliance with backend AI systems (like Sarvam).
- **Accessibility Text Injection:** Uses Android's `AccessibilityService` to find the focused text field and insert the text at your cursor, keeping anything you already typed. If a field doesn't support that it pastes instead, and if there is no field at all the text is copied to your clipboard so it's never lost.
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
1. **Keyboard detection:** `TalkToMeAccessibilityService` listens for `TYPE_WINDOWS_CHANGED` (sent when the keyboard window appears or disappears) plus focus events, and checks for a window of type `TYPE_INPUT_METHOD` to get the keyboard's position.
2. **Floating bubble:** The accessibility service draws the bubble itself (`BubbleOverlay`) as a `TYPE_ACCESSIBILITY_OVERLAY` window rendered with Jetpack Compose. That window sits above the keyboard, needs no "Display over other apps" permission, and lives exactly as long as the accessibility service, so there is no separate background service for Android to kill.
3. **Custom Audio Pipeline:** When you tap the mic, a custom `AudioRecord` implementation streams raw 16 kHz PCM to a temporary file and adds a 44-byte WAVE header when you stop. A short-lived `RecordingService` shows a "Listening…" notification only while you dictate.
4. **Network & Injection:** The `.wav` goes to Sarvam's API via `Retrofit`/`OkHttp`. The transcript is inserted at the cursor with `ACTION_SET_TEXT` (falling back to `ACTION_PASTE`, then the clipboard).

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
   - Accessibility Service Permission (turn on "Use TalkToMe", leave the shortcut toggle off)

   Recommended: tap **Allow** on the "Keep TalkToMe running" card to give it unrestricted battery use (on Xiaomi/Oppo/Vivo also enable Autostart).

   If the Accessibility toggle is greyed out with "Restricted setting" (Android 13+ when the APK is sideloaded), open **App info → ⋮ → Allow restricted settings**, then turn it on.

### 🤖 Troubleshooting with AI IDEs
Deploying Android apps can sometimes result in environment issues (like Gradle version mismatches, Java SDK errors, or manifest conflicts). 

If you encounter any build errors, **highly recommend** opening the project folder in an AI-first IDE like:
- **[Google Antigravity (AGY)](https://aistudio.google.com/)**
- **[Cursor](https://cursor.sh/)**

Simply paste the build error into the AI chat. Because the AI has full context of the project files, it can instantly pinpoint the fix (e.g., "Change the target SDK to 34" or "Update the Kotlin compiler version") rather than you having to search StackOverflow.
