<div align="center">

# 🤖 MAYA AI Assistant
### *Intelligent Multimodal Voice & Screen Assistant for Android*

<img src="maya_hero_banner.jpg" width="100%" alt="MAYA AI Hero Banner" />

[![Build and Release Android APK](https://github.com/herovanshu83-ops/maya_ai/actions/workflows/build_and_release.yml/badge.svg)](https://github.com/herovanshu83-ops/maya_ai/actions/workflows/build_and_release.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%2520Compose-M3-blue.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Gemini AI](https://img.shields.io/badge/Gemini%25202.5-Flash-orange.svg?style=flat&logo=google)](https://ai.google.dev)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

</div>

---

## 🌟 Overview

**MAYA** is an advanced, offline-first Android assistant designed for seamless voice interactions, contextual on-screen actions, and real-time streaming intelligence powered by **Gemini 2.5 Flash**. Built with modern Jetpack Compose and Material Design 3, MAYA delivers a futuristic cyberpunk-inspired voice interface with complete local data privacy.

---

## ✨ Key Features & Capabilities

- **🗣️ Continuous Voice & Speech-to-Text (STT)**: Real-time speech transcription with acoustic echo suppression and automatic retry loops.
- **⚡ Gemini 2.5 Flash Streaming AI**: Generates instant, streaming intelligence and conversational responses.
- **🔊 Natural Text-to-Speech (TTS) & Floating Overlays**: Floating system service for quick voice interaction from anywhere on your device.
- **🛠️ Intelligent Action Orchestration**: Automatically classifies user intent to control system settings, open apps, and manage routines.
- **🗄️ Offline-First Architecture**: Secure local storage powered by **Room Database** and encrypted encryption keys.
- **🎨 Futuristic Cyberpunk UI**: Immersive dark theme, glowing orb animations, and dynamic color palettes.

---

## 🏗️ Architecture & Tech Stack

| Component | Technology |
| :--- | :--- |
| **UI Framework** | Jetpack Compose, Material 3, Animation APIs |
| **Architecture** | MVVM with Kotlin Coroutines & `StateFlow` |
| **AI Intelligence** | Firebase Generative AI (`gemini-2.5-flash`) |
| **Local Persistence** | Room Database (SQLite) with KSP |
| **Build System** | Gradle Kotlin DSL (`build.gradle.kts`) |
| **CI/CD** | GitHub Actions Automated Build & Release Pipeline |

---

## 🚀 Automated CI/CD & Releases

This project includes a fully automated **GitHub Actions Workflow** (`.github/workflows/build_and_release.yml`) that:
1. Sets up JDK 17 and Android SDK environments.
2. Generates the Gradle Wrapper and builds the debug APK (`./gradlew assembleDebug`).
3. Uploads built APK artifacts.
4. Automatically releases the APK to GitHub Releases whenever a version tag (e.g., `v1.0.0`) is pushed.

---

## 📱 Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/herovanshu83-ops/maya_ai.git
   ```
2. **Open in Android Studio** (Koala / Jellyfish or newer).
3. **Build & Run**: Sync Gradle and run on an Android 8.0+ (API 26+) device or emulator.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
