# Contributing

Thanks for looking. This project is meant to be forked and made your own, so small, focused changes are the easiest to merge.

## Run it

1. Open the folder in Android Studio, or use the terminal:

   ```bash
   ./gradlew testDebugUnitTest   # unit tests
   ./gradlew assembleDebug       # build the app
   ```

2. Add your own API keys in the app's Settings screen, or put them in `local.properties` for debug builds (see the README). Never commit keys; `local.properties` is git-ignored.

## Before you open a pull request

- Keep the change small and say what problem it solves.
- Add a unit test for new rules, parsing or request building. Look at `app/src/test` for examples.
- Run `./gradlew testDebugUnitTest assembleDebug` and make sure it passes.
- If you change the database, bump the version in `Dao.kt` and add a `Migration`; existing users keep their cards.
- If you add a third-party library, add it to `THIRD_PARTY_NOTICES.md` with its licence.

## Good first changes

- Add a language to `Languages.kt`.
- Add a colour theme to `Theme.kt`.
- Try another translation, voice or AI service (see `DeepL.kt`, `ElevenLabs.kt`, `AiClients.kt`).

## Reporting bugs

Use the bug report form. Please remove any API keys from what you paste.
