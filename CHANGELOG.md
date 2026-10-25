
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

## 0.2.0 · 2026-10-25
- New `list` lesson block: an intro line plus bullet points, read aloud one bullet at a time with highlighting. Courses now explain topics with bullet lists instead of paragraphs.
- Demo course updated to 1.1.0 (bullet-list lessons).
- Phone screenshots added to the README.

## 0.3.0 · 2026-10-25
- One repo for everything: courses now live in `courses/<topic>/`; the 4-stroke engine course history was merged in.
- Sync reads the repo's `courses/` folder and downloads only folders whose git tree SHA changed (no zip, no topic search).
- Demo course is bundled into the APK at build time from `courses/` (no duplicate copy in the source tree).
- Database migration 1→2 keeps lesson progress when updating from 0.2.0 (tested upgrade on the emulator).
- Settings: "Course repo" (owner/name) replaces "GitHub account".
