package com.example.shannon.data

import android.content.Context
import com.example.shannon.titleResId
import com.example.shannon.domain.model.HomeDashboardStatus
import com.example.shannon.domain.model.HomeDashboardStatusKey
import com.example.shannon.domain.model.HomeDashboardStatusTone
import com.example.shannon.domain.model.PortStatus

class HomeDashboardStatusStore(
    private val context: Context,
    private val dashboardStatusDao: DashboardStatusDao,
) {
    suspend fun loadStatuses(): Map<HomeDashboardStatusKey, HomeDashboardStatus> {
        return dashboardStatusDao.getAll().mapNotNull { entity ->
            val key = runCatching {
                HomeDashboardStatusKey.valueOf(entity.toolKey)
            }.getOrNull() ?: return@mapNotNull null
            val tone = runCatching {
                HomeDashboardStatusTone.valueOf(entity.tone)
            }.getOrDefault(HomeDashboardStatusTone.Neutral)
            val text = when (key) {
                HomeDashboardStatusKey.PortScan -> {
                    PortStatus.entries.firstOrNull { it.name == entity.textValue }
                        ?.let { context.getString(it.titleResId()) }
                        ?: entity.textValue
                }
                else -> entity.textValue
            }
            key to HomeDashboardStatus(
                key = key,
                text = text,
                tone = tone,
            )
        }.toMap()
    }

    suspend fun saveStatus(status: HomeDashboardStatus) {
        dashboardStatusDao.upsert(
            DashboardStatusEntity(
                toolKey = status.key.name,
                textValue = status.text,
                tone = status.tone.name,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }
}
