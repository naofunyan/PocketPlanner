# Interpreter Screen — Real-time Conversation Translation

Build a Google Translate–style **Interpreter / Conversation Mode** screen inside the existing AI Hub pager (tab index 3), replacing the current `InterpreterPlaceholder`.

The screen is split into two halves: the **top half** (flipped upside-down so the person across the table can read it) shows Language A, and the **bottom half** shows Language B. Each half has its own mic button. When the user speaks, the Gemini Live API transcribes and translates in real-time, displaying the result in both panels.

---

## Proposed Changes

### Component 1: Interpreter UI

#### [NEW] [`InterpreterScreen.kt`](file:///e:/PocketPlanner/app/src/main/java/com/example/pocketplanner/ui/chat/interpreter/InterpreterScreen.kt)

The main Composable screen, structured as:

```
┌──────────────────────────────┐
│  ▼ Language A (flipped 180°) │  ← Person across table reads this
│  "English (United Kingdom)"  │
│                              │
│  Translated text appears     │
│  here (upside-down for them) │
│                              │
│         [🔊] [🎙]           │  ← Speaker A's mic (also flipped)
├──────────────────────────────┤
│         [🎙] [🔊]           │  ← Speaker B's mic
│                              │
│  Translated text appears     │
│  here (right-side-up for you)│
│                              │
│  "Vietnamese"                │
│  ▼ Language B selector       │
└──────────────────────────────┘
```

**Key UI details:**
- Top half uses `Modifier.graphicsLayer(rotationZ = 180f)` to flip all content upside-down
- A thin divider or gradient separates the two halves
- Each half contains: a language selector dropdown, a scrollable conversation transcript area, and a mic button
- Language selector shows a dropdown of supported languages (powered by ML Kit's `TranslateLanguage` list)
- Mic buttons use the app's `MaterialTheme.colorScheme.primary` when actively recording (pulsing animation), neutral/dark when idle
- A speaker icon next to each mic plays back the last translated audio using Android TTS

---

#### [NEW] [`InterpreterViewModel.kt`](file:///e:/PocketPlanner/app/src/main/java/com/example/pocketplanner/ui/chat/interpreter/InterpreterViewModel.kt)

ViewModel managing the interpreter session state:

**State:**
- `languageA: MutableStateFlow<String>` — BCP-47 code, default `"en"`
- `languageB: MutableStateFlow<String>` — BCP-47 code, default `"vi"`
- `conversationMessages: MutableStateFlow<List<InterpreterMessage>>` — ordered list of all messages
- `isRecordingA: MutableStateFlow<Boolean>` — is Speaker A's mic active
- `isRecordingB: MutableStateFlow<Boolean>` — is Speaker B's mic active
- `interpreterState: MutableStateFlow<InterpreterState>` — `IDLE`, `LISTENING_A`, `LISTENING_B`, `TRANSLATING`, `ERROR`

**Data class:**
```kotlin
data class InterpreterMessage(
    val speaker: Speaker, // A or B
    val originalText: String,
    val translatedText: String,
    val timestamp: Long
)
```

**Core logic — Speech-to-Text + Translation approach:**

> [!IMPORTANT]
> **Design Decision: How to handle speech recognition + translation**
> 
> There are two possible approaches:
> 
> **Option A — Gemini Live API (reuse existing WebSocket):**
> Configure the Gemini Live API system prompt to act as an interpreter. When Speaker A talks in English, Gemini responds in Vietnamese audio, and vice versa. This reuses the existing `LiveVoiceViewModel` WebSocket infrastructure and the `generateAccessToken()` backend.
> - ✅ Reuses existing code, produces natural-sounding audio output
> - ⚠️ Uses API quota/billing, requires internet
> 
> **Option B — On-device ML Kit + Android SpeechRecognizer + TTS:**
> Use Android's built-in `SpeechRecognizer` for speech-to-text, ML Kit `Translation` for text translation, and Android `TextToSpeech` for speaking the result.
> - ✅ Works offline (after model download), no API cost
> - ⚠️ Translation quality may be lower, TTS sounds robotic
> 
> **Recommendation: Option A (Gemini Live API)** — it produces far superior translation quality and natural speech output. The user already has the backend infrastructure deployed.

**Functions:**
- `startListening(speaker: Speaker)` — begins recording audio from mic, streams to Gemini
- `stopListening()` — stops recording, waits for Gemini response
- `setLanguageA(code: String)` / `setLanguageB(code: String)` — update languages and reconfigure Gemini system prompt
- `clearConversation()` — reset transcript
- Internally reuses `AudioRecorder` and `AudioPlayer` from the `live` package
- Connects to the same Gemini Live WebSocket with a specialized system prompt:
  ```
  "You are a real-time interpreter. When you hear speech in {languageA}, 
   respond ONLY with the translation in {languageB}, and vice versa. 
   Do not add commentary. Translate naturally and conversationally."
  ```

---

### Component 2: Wire into Navigation

#### [MODIFY] [`AiHubScreen.kt`](file:///e:/PocketPlanner/app/src/main/java/com/example/pocketplanner/ui/chat/AiHubScreen.kt)

- Replace `InterpreterPlaceholder(onNavigateBack)` (line 223) with the new `InterpreterScreen(onNavigateBack = onNavigateBack)`
- Delete the `InterpreterPlaceholder` composable function (lines 266–299)

---

## Open Questions

> [!IMPORTANT]
> **Which approach do you prefer for the translation engine?**
> - **Option A**: Gemini Live API (better quality, uses your existing backend, requires internet)
> - **Option B**: Fully offline with Android SpeechRecognizer + ML Kit + TTS (works without internet, lower quality)
> - **Option C**: Hybrid — use Gemini when online, fall back to offline ML Kit when no internet

> [!NOTE]
> **Language list:** Should we support all ~60 languages that ML Kit Translation supports, or limit it to a curated list of common travel languages (English, Spanish, French, Japanese, Korean, Vietnamese, Thai, Chinese, Arabic, etc.)?

---

## Verification Plan

### Manual Verification
- Open the app → tap the AI circle → swipe to the INTERPRET tab
- Select two different languages
- Tap the bottom mic → speak in Language B → verify translated text appears in the top panel and audio plays
- Tap the top mic → speak in Language A → verify translated text appears in the bottom panel
- Test language switching mid-conversation
- Verify the top panel is correctly flipped so someone across the table can read it
