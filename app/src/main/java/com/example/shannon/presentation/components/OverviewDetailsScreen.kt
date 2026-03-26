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
import com.example.shannon.R
import com.example.shannon.domain.model.NetworkOverview

@Composable
fun OverviewDetailsScreen(
    overview: NetworkOverview?,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = onRefresh) {
            Text(context.getString(R.string.action_refresh_network))
        }
        overview?.let {
            OverviewCard(overview = it)
        } ?: PlaceholderCard(text = context.getString(R.string.overview_loading))
    }
}

@Composable
private fun OverviewCard(overview: NetworkOverview) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun copyValue(label: String, value: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(context, context.getString(R.string.overview_label_copied, label), Toast.LENGTH_SHORT).show()
    }

    DiagnosticsSectionSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Text(
            text = context.getString(R.string.screen_network_overview),
            style = MaterialTheme.typography.titleMedium,
        )
        OverviewRow(context.getString(R.string.overview_type), overview.networkType, onCopy = ::copyValue)
        OverviewRow(context.getString(R.string.overview_ssid), overview.ssid, onCopy = ::copyValue)
        OverviewRow(context.getString(R.string.overview_bssid), overview.bssid, onCopy = ::copyValue)
        OverviewRow(context.getString(R.string.overview_signal_strength), overview.signalStrength, onCopy = ::copyValue)
        OverviewRow(context.getString(R.string.overview_internet), overview.internetReachable.toStatus(context), onCopy = ::copyValue)
        OverviewRow(context.getString(R.string.overview_validated), overview.validated.toStatus(context), onCopy = ::copyValue)
        OverviewRow(context.getString(R.string.overview_metered), overview.metered.toStatus(context, invertMeaning = true), onCopy = ::copyValue)
        OverviewRow(context.getString(R.string.overview_downstream), overview.downstreamMbps, onCopy = ::copyValue)
        OverviewRow(context.getString(R.string.overview_upstream), overview.upstreamMbps, onCopy = ::copyValue)
        OverviewRow(context.getString(R.string.overview_private_ip), overview.privateIpAddress, onCopy = ::copyValue)
        OverviewRow(context.getString(R.string.overview_gateway), overview.gatewayAddress, onCopy = ::copyValue)
        OverviewRow(context.getString(R.string.overview_ipv6), overview.ipv6Address, onCopy = ::copyValue)
        OverviewRow(context.getString(R.string.overview_dns), overview.dnsServers.joinToString(), onCopy = ::copyValue)
        OverviewRow(context.getString(R.string.overview_carrier), overview.carrierName, onCopy = ::copyValue)
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

private fun Boolean.toStatus(context: Context, invertMeaning: Boolean = false): String {
    return if (invertMeaning) {
        if (this) context.getString(R.string.answer_no) else context.getString(R.string.answer_yes)
    } else {
        if (this) context.getString(R.string.answer_yes) else context.getString(R.string.answer_no)
    }
}

private fun String.isCopyableOverviewValue(): Boolean {
    return lowercase() !in setOf("n/a", "yes", "no", "unavailable", "н/д", "да", "нет")
}
