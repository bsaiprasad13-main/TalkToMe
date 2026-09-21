# Problem Statement

## Background
Users frequently communicate using romanized Telugu ("Tenglish") in messaging applications like WhatsApp. However, typing Tenglish manually can be slow and cumbersome. While speech-to-text solutions exist, there is a lack of native, accessible tools that seamlessly translate spoken Telugu into romanized text and inject it directly into the user's active chat without switching apps.

## The Solution
**TalkToMe** is a native Android application designed to solve this friction. It provides a persistent, floating "mic bubble" that sits on top of any active application. When the user taps the bubble, speaks in Telugu, and stops recording, the app uses Sarvam AI's speech-to-text API (in transliteration mode) to transcribe the speech into correctly romanized Telugu text.

The app then automatically types this text directly into whatever text field the user currently has focused, significantly speeding up messaging workflows. The core machine learning translation has already been validated using a Python test script; the goal now is to build this into a robust, user-friendly Android experience.

## Key Goals
- **Frictionless UX:** The user should not have to leave their current app (e.g., WhatsApp) to use the tool.
- **Accurate Transliteration:** Reliably convert spoken Telugu to English-script (Tenglish) using the validated Sarvam AI API.
- **Safe Injection:** Insert the text into the text field but **do not auto-send**. The user must maintain control over reviewing and sending the message.
- **System Compatibility:** Work reliably across modern Android devices (Android 8.0+) despite varying OEM background service restrictions.
