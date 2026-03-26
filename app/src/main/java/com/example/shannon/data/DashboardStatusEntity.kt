package com.example.shannon.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dashboard_status")
data class DashboardStatusEntity(
    @PrimaryKey
    @ColumnInfo(name = "tool_key")
    val toolKey: String,
    @ColumnInfo(name = "text_value")
    val textValue: String,
    @ColumnInfo(name = "tone")
    val tone: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
