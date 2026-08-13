package com.example.pocketplanner.ui.chat

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationManager @Inject constructor() {
    private val languageIdentifier = LanguageIdentification.getClient()

    suspend fun detectLanguage(text: String): String? {
        return try {
            val languageCode = languageIdentifier.identifyLanguage(text).await()
            if (languageCode == "und") null else languageCode
        } catch (e: Exception) {
            null
        }
    }

    suspend fun translate(text: String, sourceLang: String, targetLang: String): String {
        if (sourceLang == targetLang) return text
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()
        val translator = Translation.getClient(options)

        return try {
            translator.downloadModelIfNeeded().await()
            translator.translate(text).await()
        } catch (e: Exception) {
            text // Fallback
        } finally {
            translator.close()
        }
    }
}