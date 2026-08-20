# Visual Translate (Lens Tab) — Implementation Plan

Build a Google Lens-style visual translation feature with three modes: live camera overlay, capture-and-translate, and gallery import.

## Reference Screenshots

````carousel
![Live camera mode — real-time translated text overlay on camera preview](C:/Users/sherl/.gemini/antigravity/brain/b39d65e6-9b16-4517-a198-a9db9fd67452/.user_uploaded/media_1787194070447.png)
<!-- slide -->
![Capture mode — frozen image with translated overlay + bottom panel with Select All, Listen, Share](C:/Users/sherl/.gemini/antigravity/brain/b39d65e6-9b16-4517-a198-a9db9fd67452/.user_uploaded/media_1787194158062.png)
````

---

## Existing Infrastructure (Already in Project)

| Component | Status | Details |
|---|---|---|
| CameraX | ✅ Installed | `camera-core`, `camera2`, `lifecycle`, `view` (1.3.1) |
| ML Kit Translation | ✅ Installed | `translate:17.0.3` via `TranslationManager.kt` |
| ML Kit Text Recognition | ❌ **Missing** | Needs `text-recognition:16.0.0` |
| Camera Permission | ✅ Declared | Already in `AndroidManifest.xml` |
| Accompanist Permissions | ✅ Installed | `accompanist-permissions:0.34.0` |
| CameraX Reference Code | ✅ Available | Working `PreviewView` setup in `LiveVoiceScreen.kt` |

---

## Proposed Approach

### Three Modes

| Mode | How it works |
|---|---|
| **Live Camera** | CameraX `Preview` + `ImageAnalysis`. ML Kit OCR runs on each frame (~300ms throttle). Translated text is drawn as a Compose `Canvas` overlay directly on top of detected text regions. |
| **Capture** | User taps shutter → CameraX `ImageCapture` freezes the frame. ML Kit OCR runs once on the full-res image. Translated overlay is rendered on the frozen image. A bottom panel shows the full translated text with "Select All", "Listen", and "Share" actions. |
| **Gallery Import** | User taps gallery icon → Android Photo Picker. Selected image is loaded via Coil/BitmapFactory. Same OCR + overlay pipeline as Capture mode. |

### Performance Strategy for Live Mode

> [!IMPORTANT]
> Live translation is the most performance-sensitive part. The plan uses these optimizations:

- **Frame throttling**: Process only 1 frame every ~300ms via `ImageAnalysis`, dropping intermediate frames
- **Translation caching**: A `HashMap<String, String>` caches already-translated strings so the same text block isn't re-translated every frame
- **Async pipeline**: OCR runs on `Dispatchers.Default`, translation on `Dispatchers.IO`, UI updates on `Dispatchers.Main`
- **Coroutine debounce**: If a new frame arrives before the previous one finishes processing, the old job is cancelled

---

## Proposed Changes

### Dependency Addition

#### [MODIFY] [build.gradle.kts](file:///e:/PocketPlanner/app/build.gradle.kts)

Add the ML Kit Text Recognition dependency:

```diff
+implementation("com.google.mlkit:text-recognition:16.0.1")
```

> [!NOTE]
> This is the Latin-script recognizer. If you want to support Vietnamese diacritics more accurately, the Latin bundle is sufficient. For Chinese/Japanese/Korean text recognition, separate bundles exist (`text-recognition-chinese`, etc.) but are not needed for EN↔VI.

---

### New Package: `ui/chat/lens/`

#### [NEW] [LensState.kt](file:///e:/PocketPlanner/app/src/main/java/com/example/pocketplanner/ui/chat/lens/LensState.kt)

Data models for the Lens feature:

- `DetectedTextBlock` — Holds the original text, translated text, and the bounding box (`Rect`) from ML Kit so we know exactly where to draw the overlay
- `LensMode` enum — `LIVE`, `CAPTURED`, `GALLERY`
- `LensUiState` — Combines the current mode, list of detected blocks, source/target language, and the captured/imported bitmap

---

#### [NEW] [LensViewModel.kt](file:///e:/PocketPlanner/app/src/main/java/com/example/pocketplanner/ui/chat/lens/LensViewModel.kt)

The brain of the Lens feature. Key responsibilities:

- **`processFrame(imageProxy: ImageProxy)`** — Called by CameraX `ImageAnalysis` on every frame (throttled). Runs ML Kit `TextRecognizer` to detect text blocks, then batch-translates them using the existing `TranslationManager`. Pushes `DetectedTextBlock` list to the UI via StateFlow.
- **`processCapturedImage(bitmap: Bitmap)`** — Same OCR + translation pipeline but on a full-resolution bitmap (for Capture and Gallery modes).
- **`translationCache: HashMap<String, String>`** — Avoids re-translating identical strings across frames.
- **`setSourceLanguage(lang)` / `setTargetLanguage(lang)`** — Language selection (defaults: Vietnamese → English).
- **`getFullTranslatedText(): String`** — Returns all detected blocks' translated text concatenated, for the bottom panel in Capture mode.
- Injects `TranslationManager` via Hilt (already a `@Singleton`).

---

#### [NEW] [LensScreen.kt](file:///e:/PocketPlanner/app/src/main/java/com/example/pocketplanner/ui/chat/lens/LensScreen.kt)

The main Compose UI. Structure:

```
┌─────────────────────────────────┐
│  [Vietnamese →  English]  [+]  │  ← Top bar with language selector & gallery button
├─────────────────────────────────┤
│                                 │
│     CameraX Preview             │
│     + Canvas Overlay            │  ← Translated text drawn on detected regions
│       (or frozen Bitmap         │
│        in capture mode)         │
│                                 │
├─────────────────────────────────┤
│  [Shutter Button]  [文A icon]   │  ← Bottom controls
├─────────────────────────────────┤
│  Translated text (capture only) │  ← Bottom panel: Select All, Listen, Share
└─────────────────────────────────┘
```

Key composables:
- **`LensScreen`** — Top-level container. Manages CameraX lifecycle, permission requests (using Accompanist), and mode switching.
- **`CameraPreviewWithOverlay`** — Wraps `AndroidView` for CameraX `PreviewView` and layers a `Canvas` composable on top that draws semi-transparent white rectangles + translated text at each `DetectedTextBlock`'s bounding box coordinates.
- **`CapturedImageView`** — Displays the frozen bitmap with the same Canvas overlay. Shown when mode is `CAPTURED` or `GALLERY`.
- **`TranslationBottomPanel`** — Slide-up panel in Capture/Gallery mode showing the full translated text with action buttons: **T Select All**, **🔊 Listen** (uses TTS), **↗ Share** (Android share intent).
- **`LanguageBar`** — Top bar showing `[◆ Vietnamese] → [English]` with `+` button to import from gallery.

---

### Navigation Wiring

#### [MODIFY] [AiHubScreen.kt](file:///e:/PocketPlanner/app/src/main/java/com/example/pocketplanner/ui/chat/AiHubScreen.kt)

- Replace `VisualTranslatePlaceholder(onNavigateBack)` with `com.example.pocketplanner.ui.chat.lens.LensScreen(onNavigateBack = onNavigateBack)` in the HorizontalPager's `when` block.
- Delete the `VisualTranslatePlaceholder` composable function entirely.

---

## Design Decisions (Confirmed)

- **Overlay rendering style**: White rectangle painted over the original text with black translated text on top — exactly matching Google Lens behavior.
- **Language direction**: Auto-detect the source language from the OCR text using ML Kit Language ID (already installed as `language-id:17.0.6`). No manual source language toggle needed — the user only picks the **target** language they want to translate into.

---

## Verification Plan

### Build Verification
```bash
cd e:\PocketPlanner && .\gradlew assembleDebug
```

### Manual Verification
1. **Live Mode**: Point camera at printed English or Vietnamese text → verify translated overlay appears in real-time
2. **Capture Mode**: Tap shutter → verify image freezes with overlay, bottom panel shows full text with working Listen/Share
3. **Gallery Mode**: Tap `+` → select image from gallery → verify same overlay + bottom panel
4. **Language Toggle**: Switch languages → verify OCR re-processes with new target language
5. **Performance**: Verify live mode doesn't cause visible lag or frame drops on a mid-range device

### Artifact Update
Update the `hybrid_interpreter_guide.md` artifact (or create a new `lens_guide.md`) with the complete copy-paste code for all new files.
