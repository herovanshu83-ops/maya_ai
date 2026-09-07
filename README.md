# 🤖 MAYA AI Assistant — Intelligent Android Voice & Screen Assistant

MAYA AI Assistant is a powerful, offline-first, multimodal Android assistant built with **Jetpack Compose**, **Kotlin Coroutines**, **Room Database**, and **Firebase Generative AI (Gemini 2.5 Flash)**. It combines real-time voice speech recognition, text-to-speech feedback, system automation, and contextual on-screen action execution.

---

## ✨ Key Features

- **🗣️ Continuous Voice & STT Engine**: Real-time speech-to-text transcription with custom acoustic echo suppression and automatic retry loops.
- **⚡ Firebase Generative AI Streaming**: Integrates Gemini 2.5 Flash (`generateContentStream`) to stream intelligent assistant responses instantly.
- **🔊 Text-to-Speech (TTS) & Floating Overlays**: Natural voice feedback combined with a floating overlay service for system-wide accessibility.
- **🛠️ Intelligent Action Execution**: Automatically classifies user intent to execute system controls, open applications, manage routines, and recall stored memories.
- **🗄️ Offline-First Local Storage**: Built with **Room Database** and encrypted storage for robust local history, command logs, and user memory.
- **🎨 Modern Material Design 3 (M3)**: Crafted with a sleek dark-themed futuristic UI, dynamic color palettes, custom launcher icons, and smooth animations.

---

## 🏗️ Architecture & Tech Stack

- **UI Framework**: Jetpack Compose & Material 3
- **Architecture**: MVVM (Model-View-ViewModel) with StateFlow
- **AI Integration**: Firebase AI Logic (`com.google.firebase:firebase-ai`) with `gemini-2.5-flash`
- **Database**: Room Database with KSP
- **Async Processing**: Kotlin Coroutines & Flows
- **Testing**: Robolectric & JUnit unit testing

---

## 📱 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/Manish04ii/maya_ai.git
   ```
2. Open the project in **Android Studio Koala / Jellyfish** or newer.
3. Configure your Firebase project and add your API credentials.
4. Build and run the app on your Android device or emulator (`minSdk 26`, `targetSdk 34`).

---

## 📄 License

This project is open-source and available under the MIT License.
