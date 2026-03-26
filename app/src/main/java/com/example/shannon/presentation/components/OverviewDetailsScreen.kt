package com.example.shannon.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.shannon.domain.model.NetworkOverview

@Composable
fun OverviewDetailsScreen(
    overview: NetworkOverview?,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = onRefresh) {
            Text("Refresh network")
        }
        overview?.let {
            OverviewCard(overview = it)
        } ?: PlaceholderCard(text = "Loading network overview...")
    }
}

@Composable
private fun OverviewCard(overview: NetworkOverview) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun copyValue(label: String, value: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
    }

    DiagnosticsSectionSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Text(
            text = "Network overview",
            style = MaterialTheme.typography.titleMedium,
        )
        OverviewRow("Type", overview.networkType, onCopy = ::copyValue)
        OverviewRow("SSID", overview.ssid, onCopy = ::copyValue)
        OverviewRow("BSSID", overview.bssid, onCopy = ::copyValue)
        OverviewRow("Signal strength", overview.signalStrength, onCopy = ::copyValue)
        OverviewRow("Internet", overview.internetReachable.toStatus(), onCopy = ::copyValue)
        OverviewRow("Validated", overview.validated.toStatus(), onCopy = ::copyValue)
        OverviewRow("Metered", overview.metered.toStatus(invertMeaning = true), onCopy = ::copyValue)
        OverviewRow("Downstream", overview.downstreamMbps, onCopy = ::copyValue)
        OverviewRow("Upstream", overview.upstreamMbps, onCopy = ::copyValue)
        OverviewRow("Private IP", overview.privateIpAddress, onCopy = ::copyValue)
        OverviewRow("Gateway", overview.gatewayAddress, onCopy = ::copyValue)
        OverviewRow("IPv6", overview.ipv6Address, onCopy = ::copyValue)
        OverviewRow("DNS", overview.dnsServers.joinToString(), onCopy = ::copyValue)
        OverviewRow("Carrier", overview.carrierName, onCopy = ::copyValue)
    }
}

@Composable
private fun OverviewRow(
    label: String,
    value: String,
    onCopy: (String, String) -> Unit,
) {
    val copyEnabled = value.isCopyableOverviewValue()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = copyEnabled) { onCopy(label, value) },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun Boolean.toStatus(invertMeaning: Boolean = false): String {
    return if (invertMeaning) {
        if (this) "No" else "Yes"
    } else {
        if (this) "Yes" else "No"
    }
}

private fun String.isCopyableOverviewValue(): Boolean {
    return lowercase() !in setOf("n/a", "yes", "no", "unavailable")
}
