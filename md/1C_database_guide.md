# Step 1C: Core Data Layer Guide (Room)

Now we need a place to store our user's travel plans so they work perfectly offline. We are using **Room**, which is Google's official SQLite database library for Android.

To keep this manageable, we'll start by creating just two core tables: **Trips** and **Places**. We can add the Expenses and Tracking tables later when we build those specific features.

## 1. Create the Database Packages
In Android Studio, let's create a clean package structure for our data.
1. Right-click on `pocketplanner` -> **New** -> **Package**. Name it: `data.local`
2. Right-click on `data.local` -> **New** -> **Package**. Name it: `entity`
3. Right-click on `data.local` -> **New** -> **Package**. Name it: `dao`

---

## 2. Create the Entities (Tables)
An "Entity" is just a data class that represents a table in the database. 

### A. The Trip Table
Inside the `entity` package, create a new Kotlin Data Class named `TripEntity.kt`:

```kotlin
package com.example.pocketplanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String, // We'll use UUIDs or Firestore IDs
    val userId: String, // To support multiple users/guests
    val destination: String,
    val startDate: Long, // Stored as Unix timestamp
    val endDate: Long,
    val budget: Double,
    val currency: String = "VND",
    val status: String = "UPCOMING", // UPCOMING, ACTIVE, PAST
    val isSyncedWithCloud: Boolean = false
)
```

### B. The Place Table
Inside the `entity` package, create another Data Class named `PlaceEntity.kt`. This will store restaurants, attractions, etc.

```kotlin
package com.example.pocketplanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: String,
    val tripId: String, // Which trip this place belongs to
    val dayNumber: Int, // Which day of the trip (1, 2, 3...)
    val name: String,
    val lat: Double,
    val lng: Double,
    val category: String, // e.g. "Restaurant", "Attraction"
    val estimatedCost: Double = 0.0,
    val notes: String = ""
)
```

---

## 3. Create the DAOs (Data Access Objects)
DAOs are interfaces that tell Room how to read and write to the tables.

### A. Trip DAO
Inside the `dao` package, create a new Kotlin **Interface** named `TripDao.kt`:

```kotlin
package com.example.pocketplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pocketplanner.data.local.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity)

    // Flow automatically updates the UI when the database changes!
    @Query("SELECT * FROM trips WHERE userId = :userId ORDER BY startDate ASC")
    fun getAllTripsForUser(userId: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: String): TripEntity?
}
```

### B. Place DAO
Inside the `dao` package, create an **Interface** named `PlaceDao.kt`:

```kotlin
package com.example.pocketplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pocketplanner.data.local.entity.PlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: PlaceEntity)

    @Query("SELECT * FROM places WHERE tripId = :tripId AND dayNumber = :dayNumber ORDER BY name ASC")
    fun getPlacesForDay(tripId: String, dayNumber: Int): Flow<List<PlaceEntity>>
}
```

---

## 4. Create the Database File
Finally, we tie it all together. Right-click on the `data.local` package (not inside entity or dao) and create an **Abstract Class** named `AppDatabase.kt`.

```kotlin
package com.example.pocketplanner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.pocketplanner.data.local.dao.PlaceDao
import com.example.pocketplanner.data.local.dao.TripDao
import com.example.pocketplanner.data.local.entity.PlaceEntity
import com.example.pocketplanner.data.local.entity.TripEntity

// If we add more tables later, we just add them to the entities array and bump the version
@Database(
    entities = [TripEntity::class, PlaceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    // Room will automatically implement these for us
    abstract fun tripDao(): TripDao
    abstract fun placeDao(): PlaceDao
}
```

---
Let me know when you've added these 5 files! Since Room generates code behind the scenes, you should do a quick **Build > Make Project** (or hit the little green hammer icon at the top of Android Studio) to ensure it compiles without errors.
