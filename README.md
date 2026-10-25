
# Blueprint

**See how anything works, one moving diagram at a time.**

Blueprint is a native Android learning app. Every topic is a short course, called a *blueprint*, built around live, interactive animations: a cutaway engine you can slow down, gears you can re-ratio, a valve-timing wheel you can drag through the RPM range. Lessons can be read aloud in the voice you choose, with each word highlighted as it's spoken.

New topics show up on their own. Each course lives in its own public GitHub repo, and the app finds and downloads new ones in the background.

<!-- Screenshots: drop PNGs into docs/screenshots/ with these names. -->
| Library | Lesson + live lab | Step-through lab | Quick check | Voices |
|---|---|---|---|---|
| ![Library](docs/screenshots/library.png) | ![Lesson](docs/screenshots/lesson-lab.png) | ![Steps](docs/screenshots/step-lab.png) | ![Quiz](docs/screenshots/quiz.png) | ![Voices](docs/screenshots/voices.png) |

> Demo course: [How a 4-Stroke Engine Works](https://github.com/CliffSolo/blueprint-4-stroke-engine), 5 lessons, 4 animated labs, ~25 min.

## Features

- **Live labs:** each concept has an animation you can play, pause, step through and tweak with sliders. Labs run offline inside the app.
- **Read aloud:** Android text-to-speech with a **separate voice for each role** (lesson text, tips/notes, code walk-throughs), adjustable speed and pitch, and word-by-word highlighting. Long-press any paragraph to start reading from there.
- **Follow along:** the lesson scrolls to whatever is being read.
- **Quick checks:** multiple-choice questions with instant feedback and explanations. Scores are saved per lesson.
- **Progress:** lessons tick off as you finish them; "Continue" picks up where you left off.
- **Self-updating library:** courses are public repos tagged `blueprint-course`. The app checks GitHub a few times a day (WorkManager, network-only) and downloads only repos that changed. No account or token needed.
- **Offline-first:** a demo course ships inside the APK; everything downloaded stays on the phone.
- **Light and dark** "blueprint paper" themes, edge-to-edge.

## How it works

```mermaid
flowchart LR
    T["Topic idea<br/>(e.g. rocket engines)"] --> C["Course repo<br/>course.json + lessons + labs"]
    C -->|tagged blueprint-course| GH[(GitHub)]
    GH -->|Search API + zip download<br/>WorkManager, every 12 h| S[GithubSync]
    S --> F["files/courses/<br/>(on the phone)"]
    A["APK assets<br/>(bundled demo)"] --> CS[CourseStore]
    F --> CS
    CS --> UI["Compose UI<br/>Library · Course · Lesson"]
    UI --> W["WebView labs<br/>via WebViewAssetLoader"]
    UI --> N["Narrator<br/>TextToSpeech"]
    UI --> DB[("Room<br/>progress")]
```

### Architecture

| Layer | What it does |
|---|---|
| `data/Models.kt` | Course format: `CourseManifest`, `Lesson`, and a sealed `Block` type (text, callout, steps, math, lab, quiz, code). Unknown block types degrade to `Unsupported`, so old app versions can still open newer courses. |
| `data/CourseStore.kt` | Finds bundled and downloaded courses; a downloaded copy overrides a bundled one with the same id. |
| `data/Database.kt` | Room: lesson progress, last-opened lesson, and which repo versions are installed. |
| `sync/GithubSync.kt` | Topic search → compare `pushed_at` → download zip → validate `course.json` → swap in. Guards against zip-slip. |
| `tts/Narrator.kt` | Speaks one utterance at a time so it can switch voices per role, skip, pause and resume; exposes word ranges for highlighting. |
| `ui/…` | Jetpack Compose + Material 3 screens with ViewModels and `StateFlow`. Labs are served from a fake `https://appassets.androidplatform.net` origin, so they behave like normal web pages without file access. |

### Design decisions

- **Native shell, web labs.** Screens, storage, sync and speech are native Kotlin. The animations are small HTML canvas labs, the best tool for 2-D motion graphics, and the same files work in any browser, so each course repo doubles as a website.
- **One repo per course.** Courses update without an app release, each one has its own history and README, and a course can be shared on its own.
- **Spoken text = displayed text.** The inline markdown parser strips markers identically for display and speech, so the TTS engine's character ranges line up with the screen. There's a unit test for it.
- **No icon library.** The ten icons the app needs are defined as vector paths, which keeps the APK lean.

## Tech stack

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Room (KSP) · DataStore · WorkManager · kotlinx.serialization · AndroidX WebKit · Android TextToSpeech · GitHub REST API · GitHub Actions

## Course format

```
blueprint-<topic>/
├── course.json        # id, title, minutes, accent colour, lesson list
├── lessons/*.json     # blocks: text, heading, callout, steps, math, lab, quiz, code
└── labs/*.html        # self-contained animated labs (+ shared kit.js / kit.css)
```

Tag the repo with the `blueprint-course` topic and the app picks it up.

## Build

Requires JDK 17 and the Android SDK (compileSdk 37).

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
tools/bundle-demo.sh          # refresh the bundled demo course from ../blueprint-4-stroke-engine
```

Every push to `main` runs the tests and builds the APK in GitHub Actions. The APK is attached to the workflow run.

## Roadmap

- More blueprints: wing lift, pin-tumbler locks, gears, WW2 bomber navigation, discrete math, function reading
- Code walk-through lessons with line-by-line narration
- Signed releases on the Releases page
- Full-screen lab mode

## Built with AI

Designed and directed by [CliffSolo](https://github.com/CliffSolo); code and course content written with [Claude Code](https://claude.com/claude-code).
