package com.example.shannon.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DashboardStatusDao {
    @Query("SELECT * FROM dashboard_status ORDER BY updated_at DESC")
    suspend fun getAll(): List<DashboardStatusEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DashboardStatusEntity)
}
