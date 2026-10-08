# Third-party notices

This project is built on the open-source libraries, fonts and online services listed below. They keep their own licenses and terms; nothing here changes them. Version numbers are the ones in `app/build.gradle.kts` and `build.gradle.kts` at the time of writing.

## Libraries that ship inside the app

| Library | Version | Used for | License |
|---|---|---|---|
| [Jetpack Compose](https://developer.android.com/jetpack/compose) (BOM, UI, Material 3) | BOM 2026.09.00 | All screens | Apache 2.0 |
| [Material Icons Core](https://developer.android.com/jetpack/androidx/releases/compose-material) | via the Compose BOM | Back arrow, plus, drop-down arrow | Apache 2.0 |
| [AndroidX Activity Compose](https://developer.android.com/jetpack/androidx/releases/activity) | 1.13.0 | Hosting Compose, back handling | Apache 2.0 |
| [AndroidX Lifecycle ViewModel Compose](https://developer.android.com/jetpack/androidx/releases/lifecycle) | 2.11.0 | View models | Apache 2.0 |
| [AndroidX Room](https://developer.android.com/jetpack/androidx/releases/room) (runtime, KTX) | 2.8.5 | Local database | Apache 2.0 |
| [AndroidX Media3 Session](https://developer.android.com/media/media3) | 1.11.1 | Android Auto media session | Apache 2.0 |
| [Kotlin standard library and coroutines](https://kotlinlang.org) | with Kotlin | Language runtime, background work | Apache 2.0 |

## Build tools (not shipped in the app)

| Tool | Version | License |
|---|---|---|
| Android Gradle Plugin | 9.4.1 | Apache 2.0 |
| Kotlin Compose compiler plugin | 2.4.20 | Apache 2.0 |
| [KSP](https://github.com/google/ksp) (Room's code generator) | 2.3.12 | Apache 2.0 |

## Test-only dependencies (not shipped in the app)

| Library | Version | License |
|---|---|---|
| JUnit 4 | 4.13.2 | Eclipse Public License 1.0 |
| AndroidX Test (JUnit extension, runner) | 1.3.0 / 1.7.0 | Apache 2.0 |
| [android-json](https://github.com/vaadin/android-json) (a copy of Android's own `org.json`) | 0.0.20131108.vaadin1 | Apache 2.0. Lets unit tests parse JSON on a computer; on a phone the app uses Android's built-in `org.json`. |

## Font

**Plus Jakarta Sans**, copyright 2020 The Plus Jakarta Sans Project Authors ([github.com/tokotype/PlusJakartaSans](https://github.com/tokotype/PlusJakartaSans)), bundled in `app/src/main/res/font/plus_jakarta_sans.ttf`. It is licensed under the **SIL Open Font License 1.1**. If you redistribute the font, keep this notice and include the license text ([openfontlicense.org](https://openfontlicense.org)).

## Online services (called over the internet, not bundled)

The app talks to these services with each user's own API key. Using them is subject to each provider's terms, pricing and privacy policy, and some have free tiers with conditions of their own (for example attribution or usage limits), so check before you publish or sell an app built on this code.

| Service | Used for |
|---|---|
| [DeepL API](https://www.deepl.com/pro-api) | Translating between the two languages of a deck |
| [ElevenLabs](https://elevenlabs.io) | Spoken audio. The default voice is the premade "Sarah" voice. |
| [Anthropic (Claude)](https://www.anthropic.com), [Google (Gemini)](https://ai.google.dev), [OpenAI](https://openai.com) | Writing a card's Latin-letter reading |

## Design, icons and emoji

- **Design system:** the "Jadran Lexicon" look (palette, type scale) was designed for this project with Google Stitch.
- **App icon and trash icon:** original vector drawings in this repository (`res/drawable/ic_launcher_*.xml` and the trash icon in `DeckListScreen.kt`).
- **Flags and small symbols:** the country flags and symbols such as 🎨 ⚙️ 🧊 🔊 are ordinary Unicode emoji. They are drawn by the phone's own emoji font, so no image files are bundled and their look differs by device.

## Tools used during development (not part of the project)

- Android Studio, the Android SDK and the Android Auto **Desktop Head Unit** emulator (Google tools used for testing; not included in this repository).
