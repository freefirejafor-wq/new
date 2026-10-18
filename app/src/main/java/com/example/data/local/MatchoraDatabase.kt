package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.*

@Database(
    entities = [
        MatchEntity::class,
        ChannelEntity::class,
        SportCategoryEntity::class,
        AdminLogEntity::class,
        SystemConfigEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class MatchoraDatabase : RoomDatabase() {
    abstract fun matchoraDao(): MatchoraDao

    companion object {
        @Volatile
        private var INSTANCE: MatchoraDatabase? = null

        fun getDatabase(context: Context): MatchoraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MatchoraDatabase::class.java,
                    "matchora_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
