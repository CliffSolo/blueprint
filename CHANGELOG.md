
# Changelog

## 0.1.0 · 2026-10-25
- First build: native Kotlin + Jetpack Compose app.
- Library, course and lesson screens with blueprint-paper light/dark themes.
- Live HTML labs inside lessons (WebViewAssetLoader, offline).
- Read-aloud narration: voice per role, speed, pitch, word highlighting, follow-along scrolling, long-press to read from a paragraph.
- Quick-check quizzes with explanations; lesson progress saved with Room.
- GitHub sync: finds public repos tagged `blueprint-course`, downloads changed ones every 12 h (WorkManager).
- Bundled demo course: How a 4-Stroke Engine Works.
- Unit tests for the course format, narration and zip-slip safety; GitHub Actions APK build.
- Tested on an Android 16 (API 36) Pixel 7 emulator.
