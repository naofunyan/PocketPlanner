package com.example.pocketplanner.data.di

import android.content.Context
import androidx.room.Room
import com.example.pocketplanner.data.local.AppDatabase
import com.example.pocketplanner.data.local.dao.AlertDao
import com.example.pocketplanner.data.local.dao.ExpenseDao
import com.example.pocketplanner.data.local.dao.PlaceDao
import com.example.pocketplanner.data.local.dao.TripDao
import com.example.pocketplanner.data.local.dao.SavedPlaceDao
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    val MIGRATION_19_20 = object : androidx.room.migration.Migration(19, 20) {
        override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE trips ADD COLUMN isOpenEnded INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pocketplanner.db"
        )
        .addMigrations(MIGRATION_19_20)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideTripDao(database: AppDatabase): TripDao {
        return database.tripDao()
    }

    @Provides
    @Singleton
    fun providePlaceDao(database: AppDatabase): PlaceDao {
        return database.placeDao()
    }

    @Provides
    fun provideExpenseDao(database: AppDatabase): ExpenseDao {
        return database.expenseDao()
    }

    @Provides
    fun provideTrackingPointDao(database: AppDatabase): com.example.pocketplanner.data.local.dao.TrackingPointDao {
        return database.trackingPointDao()
    }

    @Provides
    fun provideAlertDao(appDatabase: AppDatabase): AlertDao {
        return appDatabase.alertDao()
    }

    @Provides
    fun providePlaceDetailsDao(appDatabase: AppDatabase): com.example.pocketplanner.data.local.dao.PlaceDetailsDao {
        return appDatabase.placeDetailsDao()
    }

    @Provides
    fun provideTicketDao(database: AppDatabase): com.example.pocketplanner.data.local.dao.TicketDao {
        return database.ticketDao()
    }

    @Provides
    fun provideSavedPlaceDao(database: AppDatabase): SavedPlaceDao {
        return database.savedPlaceDao()
    }
}
