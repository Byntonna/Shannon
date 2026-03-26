package com.example.shannon.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DashboardStatusEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ShannonDatabase : RoomDatabase() {
    abstract fun dashboardStatusDao(): DashboardStatusDao

    companion object {
        @Volatile
        private var instance: ShannonDatabase? = null

        fun getInstance(context: Context): ShannonDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShannonDatabase::class.java,
                    "shannon.db",
                ).build().also { instance = it }
            }
        }
    }
}
