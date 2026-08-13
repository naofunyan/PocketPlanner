package com.example.pocketplanner.data.di

import com.example.pocketplanner.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.vertexai.vertexAI
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.type.generationConfig
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIModule {

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel {
        // We use the Gemini 1.5 Flash model for fast, standard chat and text tasks
        return Firebase.vertexAI.generativeModel(
            modelName = "gemini-1.5-flash",
            generationConfig = generationConfig {
                temperature = 0.7f // Balanced creativity
            }
        )
    }

    @Provides
    @Singleton
    fun provideLanguageIdentifier(): LanguageIdentifier {
        return LanguageIdentification.getClient()
    }
}