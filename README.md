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
- **Context-Aware Floating Bubble (Wispr Flow style):** The microphone bubble is invisible until you need it. It appears docked to the screen edge just above the keyboard the moment a keyboard opens, and hides when it closes. Like Wispr Flow, it stays out of password, PIN, number and phone fields.
- **Drag & Remember:** Drag the bubble anywhere; it snaps to the nearest edge and remembers the side and height you chose, while never covering the keys.
- **On/Off Switch:** Turn the floating bubble off from the app's home screen whenever you don't want it, and back on when you do. The change takes effect instantly and is remembered across restarts.
- **Keyboard-Style Typing:** On Android 13+ your words are typed through the accessibility input connection, exactly like a keyboard typing at your cursor. Anything you already typed is kept, and placeholder text such as WhatsApp's grey "Message" never sneaks into the result. Older phones fall back to inserting at the cursor, then pasting, and finally copying to the clipboard so your words are never lost.
- **Smart Audio Encoding:** Captures raw 16 kHz, 16-bit PCM and writes a standard `.wav` header, exactly what Sarvam's API expects. Recordings stop automatically just before Sarvam's 30-second limit.
- **Self-Diagnosing Home Screen:** Shows whether TalkToMe is ready, warns if Android has stopped the accessibility service (with a one-tap fix), and prompts for an unrestricted battery setting so the bubble keeps working in the background.
- **Clear Feedback:** Friendly messages for a busy microphone, accidental short taps, "didn't catch that", no internet, or an invalid API key instead of silent failures.
- **Privacy-Minded:** No "Display over other apps" permission needed. The microphone is only used while you are dictating, with a "Listening…" notification shown only during that time.
- **Micro-interactions & UX:** Animated waveform while recording, a smooth expansion to reveal Cancel/Insert buttons, haptic taps, and a loading spinner while Sarvam processes your voice.
- **Local History Vault:** Your last 10 transcriptions are saved on your phone and appear instantly on the home screen; tap one to copy it again.

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
2. **Keyboard Appears:** Your normal keyboard (like Gboard) slides up, and TalkToMe's blue mic bubble appears on the edge of the screen, just above the keyboard.
3. **Record:** Tap the mic and speak (e.g., *"Bhojanam chesava?"*). The bubble expands into a pill with `[ ✕ ]`, a moving waveform, and `[ ✔ ]`.
4. **Insert or Cancel:** Tap `✔` when you're done speaking (or `✕` to throw the recording away). A spinner shows while Sarvam AI transliterates your voice.
5. **Done:** The text (*"Bhojanam chesava?"*) is typed into the WhatsApp box at your cursor, ready for you to review and send. TalkToMe never sends messages for you.
6. **Vanish:** Send the message or press back; when the keyboard closes, the bubble disappears out of your way.

> 💡 **Tip:** Don't want the bubble for a while? Open TalkToMe and flip the **Floating bubble** switch off. Flip it back on whenever you want it.

---
## 🛠️ How it Works (Under the Hood)
1. **Keyboard detection:** `TalkToMeAccessibilityService` listens for `TYPE_WINDOWS_CHANGED` (sent when the keyboard window appears or disappears) plus focus events, and checks for a window of type `TYPE_INPUT_METHOD` to get the keyboard's position.
2. **Floating bubble:** The accessibility service draws the bubble itself (`BubbleOverlay`) as a `TYPE_ACCESSIBILITY_OVERLAY` window rendered with Jetpack Compose. That window sits above the keyboard, needs no "Display over other apps" permission, and lives exactly as long as the accessibility service, so there is no separate background service for Android to kill.
3. **Custom Audio Pipeline:** When you tap the mic, a custom `AudioRecord` implementation streams raw 16 kHz PCM to a temporary file and adds a 44-byte WAVE header when you stop. A short-lived `RecordingService` shows a "Listening…" notification only while you dictate.
4. **Network:** The `.wav` goes to Sarvam's `speech-to-text` API (`saaras:v4`, `translit` mode, `te-IN`) via `Retrofit`/`OkHttp`.
5. **Typing the text:** On Android 13+ the service uses its own accessibility input connection (`flagInputMethodEditor` + `commitText`) to type at the cursor like a keyboard. Otherwise it inserts with `ACTION_SET_TEXT`, then falls back to `ACTION_PASTE`, then the clipboard.
6. **Settings & state:** `SettingsRepository` (bubble on/off, docked side and height) and `HistoryRepository` are shared, process-wide singletons, so the home screen and the bubble always agree instantly.

---

## 💻 How to Deploy & Run
To install this on your personal Android device, you'll need a computer and a USB cable.

### Prerequisites
1. Download and install **[Android Studio](https://developer.android.com/studio)**.
2. An Android phone running **Android 8.0 or newer** (Android 13+ recommended for the best typing), with **Developer Options** and **USB Debugging** enabled.
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
8. Open TalkToMe. The top card should say **Ready**. Tap any text box in WhatsApp and the mic appears above your keyboard.

> 🔄 **Updating an existing install?** After installing a new version, turn TalkToMe off and back on once in **Settings → Accessibility** so Android picks up the latest service settings.

### 🩺 Troubleshooting
| Problem | What to do |
| :--- | :--- |
| Bubble doesn't appear | Check that the **Floating bubble** switch is on and the top card says **Ready**. Make sure you're in a normal text box (the bubble hides in password and number fields). |
| Home screen says "Service isn't running" | Tap **Open Accessibility settings**, turn TalkToMe off and back on. |
| Bubble stops working after a while | Tap **Allow** on the "Keep TalkToMe running" card. On Samsung set battery to **Unrestricted**; on Xiaomi/Oppo/Vivo also enable **Autostart**. |
| Text takes a long time to appear | Almost always the network or Sarvam's servers, not the app. Try Wi-Fi or try again shortly. |
| "Sarvam rejected the API key" | Check `SARVAM_API_KEY` in `local.properties`, then rebuild and reinstall. |
| "Couldn't type into this box" | Some apps block typing from accessibility services. The text is on your clipboard; long-press the box and paste. |

### 🤝 Sharing TalkToMe
Share this GitHub repository and have each person build the app with **their own** free Sarvam key in their own `local.properties`.

> ⚠️ **Don't send anyone an APK you built yourself.** Your API key is compiled into your build, so anyone with your APK could extract it and use your Sarvam credits.

### 🤖 Troubleshooting with AI IDEs
Deploying Android apps can sometimes result in environment issues (like Gradle version mismatches, Java SDK errors, or manifest conflicts). 

If you encounter any build errors, **highly recommend** opening the project folder in an AI-first IDE like:
- **[Google Antigravity (AGY)](https://aistudio.google.com/)**
- **[Cursor](https://cursor.sh/)**

Simply paste the build error into the AI chat. Because the AI has full context of the project files, it can instantly pinpoint the fix (e.g., "Change the target SDK to 34" or "Update the Kotlin compiler version") rather than you having to search StackOverflow.
