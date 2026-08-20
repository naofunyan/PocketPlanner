# PocketPlanner - Project Context & Architecture

This document contains a comprehensive breakdown of the `PocketPlanner` Android codebase, its architectural patterns, UI components, recent complex implementations, and future roadmap. It is designed to be fed to an LLM to provide immediate, deep context for continuing development.

## 1. Tech Stack & Core Libraries
- **Platform:** Android (Kotlin)
- **UI Toolkit:** Jetpack Compose (Material 3)
- **Architecture:** MVVM (Model-View-ViewModel) with `StateFlow`
- **Dependency Injection:** Dagger Hilt (`@HiltViewModel`)
- **Camera & Vision:** CameraX, Google ML Kit (Text Recognition)
- **AI Integration:** Google Gemini 1.5 (via REST / Vertex AI)
- **Networking:** `HttpURLConnection` / Retrofit (for REST calls)
- **Permissions:** Accompanist Permissions

## 2. Project Directory Structure
Below is the directory structure for `app/src/main/java/com/example/pocketplanner`:
```text
com/example/pocketplanner
|   MainActivity.kt
|   PocketPlannerApp.kt
|   
+---core
|   +---location
|   |       GeofenceBroadcastReceiver.kt
|   |       GeofenceManager.kt
|   |       LocationTrackingService.kt
|   +---notification
|   |       NotificationHelper.kt
|   +---offline
|   |       NetworkMonitor.kt
|   |       SyncScheduler.kt
|   \---utils
|           NetworkConnectivityObserver.kt
+---data
|   +---di
|   |       AIModule.kt
|   |       AppModule.kt
|   |       DatabaseModule.kt
|   |       LocationModule.kt
|   +---local
|   |   |   AppDatabase.kt
|   |   +---dao
|   |   |       AlertDao.kt, ExpenseDao.kt, PlaceDao.kt, PlaceDetailsDao.kt, TrackingPointDao.kt, TripDao.kt
|   |   \---entity
|   |           AlertEntity.kt, ExpenseEntity.kt, PlaceDetailsEntity.kt, PlaceEntity.kt, TrackingPointEntity.kt, TripEntity.kt
|   +---remote
|   +---repository
|   |       AuthRepository.kt, AuthRepositoryImpl.kt, ExpenseRepository.kt, TrackingRepository.kt, TripRepository.kt
|   \---sync
|           FirestoreSyncManager.kt
|           SyncWorker.kt
+---ui
|   +---alerts (AlertsScreen.kt, AlertsViewModel.kt)
|   +---auth (AuthScreen.kt, WelcomeScreen.kt, AuthViewModel.kt)
|   +---chat
|   |   |   AiHubScreen.kt, ChatMessage.kt, ChatScreen.kt, ChatViewModel.kt, TranslationManager.kt
|   |   +---interpreter (InterpreterScreen.kt, InterpreterState.kt, InterpreterViewModel.kt)
|   |   +---live (AudioPlayer.kt, AudioRecorder.kt, LiveVoiceScreen.kt, LiveVoiceViewModel.kt)
|   |   \---translate (TranslateScreen.kt, TranslateState.kt, TranslateViewModel.kt)
|   +---components (OfflineBannerWrapper.kt)
|   +---expense (ExpenseScreen.kt, ExpenseViewModel.kt)
|   +---explore (ExploreDetailsScreen.kt, ExploreScreen.kt)
|   +---itinerary (CreateTripDetailsScreen.kt, DayPlanScreen.kt, ImportTripScreen.kt, ItineraryScreen.kt, ItineraryViewModel.kt, TripsScreen.kt)
|   +---navigation (MainScreen.kt, Routes.kt)
|   +---search (SearchScreen.kt, SearchViewModel.kt)
|   +---theme (Color.kt, Theme.kt, Type.kt)
|   \---tracking (TrackingScreen.kt, TrackingViewModel.kt)
\---util
        ImageGenerator.kt
```

## 3. Core App Structure & Navigation
The primary entry point for AI interactions is the **AiHubScreen** which utilizes a `HorizontalPager` to navigate between four distinct modes:
1. **CHAT (Index 0):** Standard conversational AI interface.
2. **LIVE (Index 1):** Real-time voice interaction.
3. **TRANSLATE (Index 2):** (Formerly "Lens"). An AR visual translation tool using CameraX + ML Kit + Gemini.
4. **INTERPRETER (Index 3):** A split-screen bilingual voice translation interface.

**UI Quirk Note:** The `AiHubScreen` uses a custom sliding blue "pill" indicator for the tabs. The tabs are wrapped in a `Row` with `Modifier.fillMaxWidth()` and `Arrangement.SpaceEvenly`. The tabs have a `minWidth = 68.dp` so that short words (CHAT, LIVE) render as sleek pills rather than perfect circles.

## 4. Deep Dive: "Translate" (AR Camera) Module
The Translate feature (`TranslateScreen.kt` & `TranslateViewModel.kt`) is the most mechanically complex part of the app. It translates text from a camera feed or gallery image and overlays the translated text natively on the screen.

### 4.1. Translation Pipeline
1. **Image Capture:** User takes a picture via CameraX (or picks from Gallery).
2. **ML Kit Processing:** The bitmap is fed to ML Kit's `TextRecognition`.
3. **Data Extraction:** ML Kit extracts bounding boxes (`cornerX`, `cornerY`, `textWidth`, `textHeight`, `rotationAngle`) and the original raw text strings.
4. **Gemini Translation:** A JSON array of the recognized text blocks is sent to Gemini via a REST POST request. The prompt enforces that Gemini returns a JSON array of exact matching length, preserving context across the entire page.
5. **UI Rendering:** The translated strings are rendered back onto the screen matching the exact physical coordinates of the original text.

### 4.2. AR Text Rendering & Overlap Prevention (CRITICAL LOGIC)
Rendering Compose `Text` over exact image coordinates requires highly specific math to prevent text from overlapping, spilling out of boxes, or crashing into other paragraphs:
- **Scaling:** The image dimensions are compared to screen dimensions using `minOf(scaleX, scaleY)` to calculate the `densityValue`.
- **Bounding Box Inflation:** To make the text backgrounds look like comfortable "bubbles" instead of strict crops, the bounds are inflated outwards by `3dp` (`inflationDp`).
- **Font Sizing:** `estimatedFontSize` is calculated proportionally to the *original* physical bounding box height (NOT the inflated height). Formula: `(originalHeightDp / block.lineCount) * 0.75f * areaScaleFactor`. It is clamped to a minimum of `2f` and maximum of `24f` to ensure tiny labels and large titles render correctly.
- **Rotation:** Bounding boxes apply `.graphicsLayer(rotationZ = block.rotationAngle, transformOrigin = TransformOrigin(0f, 0f))` to perfectly match slanted text.
- **Backgrounds:** The bubbles use `Color.White` with a `RoundedCornerShape(6.dp)`. Vertical padding is explicitly removed so Android's native font descenders (like the tails on 'p' or 'y') don't get sheared off by the strict `.height()` modifier.

### 4.3. Text-to-Speech (TTS) "Karaoke" Mode
- Uses Android's native `TextToSpeech` engine.
- When the user presses "Listen", it reads all translated blocks sequentially.
- The UI highlights the currently spoken text block (and specific character ranges) in yellow using `AnnotatedString` and `SpanStyle`.
- **Lifecycle Hook:** TTS is tied to the tab's `isActive` state via `LaunchedEffect`. Swiping to a different tab immediately calls `viewModel.stopSpeaking()` to halt audio and clear highlights.

### 4.4. Language Selection Logic
- Uses a `GlobalLanguages` list of 100+ standard languages mapped to codes (e.g., "en" to "English").
- Rendered using a bottom sheet (`TranslateLanguagePickerBottomSheet`) containing a `LazyColumn` and a search `OutlinedTextField`.
- **Collision Avoidance:** The UI has two buttons: "Translate From" and "Translate To". If a user selects a Source language that is identical to the Target language (e.g., English to English), the `TranslateViewModel` actively intercepts this and immediately *swaps* the opposing language to prevent a collision, followed by re-triggering the translation.
- **Constraint:** The "Translate To" picker explicitly excludes the "Auto" detect option.

## 5. Planned / Upcoming Features (Roadmap)
1. **AI Receipt Scanner (Wallet Tab):** 
   - A tool built into the Expense/Wallet tab to scan physical receipts.
   - Will use Gemini's multimodal capabilities to extract Merchant, Date, Total, and Line Items and auto-fill expense logging forms.
2. **"Magic Add" AI Itinerary Insertion:** 
   - Natural language scheduling feature where a user can type or say "Dinner at Mario's tomorrow at 8pm for $50", and the AI will parse and inject it into the calendar/expense tracker automatically.

## 6. Development Guidelines for LLMs
- **Compose Imports:** Be explicit with imports (e.g., `androidx.compose.ui.graphics.Color`).
- **Modifier Ordering:** Order matters significantly (e.g., `offset` before `background` vs `background` before `padding`).
- **Do not overwrite math:** The `leftDp`, `topDp`, `widthDp`, `heightDp`, and `rawFontSize` math in `TranslateScreen.kt` has been fine-tuned to fix severe overlapping and cropping bugs. Modify with extreme caution.
