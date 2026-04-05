package com.example.shannon

import android.content.Context
import androidx.annotation.StringRes
import com.example.shannon.domain.model.ConnectivityTargetPreset
import com.example.shannon.domain.model.DnsAnalysisStatus
import com.example.shannon.domain.model.DnsLookupTransport
import com.example.shannon.domain.model.PingPacketState
import com.example.shannon.domain.model.PortScanIpVersion
import com.example.shannon.domain.model.PortScanTransport
import com.example.shannon.domain.model.PortStatus
import com.example.shannon.domain.model.ProtocolProbeErrorCategory
import com.example.shannon.domain.model.ProtocolProbeKind
import com.example.shannon.domain.model.ProtocolProbeStatus
import com.example.shannon.domain.model.ReportFormat
import com.example.shannon.domain.model.SniAnalysisStatus
import com.example.shannon.domain.model.SniProbeErrorCategory
import com.example.shannon.domain.model.SniVariantType
import com.example.shannon.domain.model.TlsAnalysisHeuristicStatus
import com.example.shannon.domain.model.TlsEndpointStatus
import com.example.shannon.domain.model.WebsiteAccessibilityCategory
import com.example.shannon.domain.model.WebsiteAccessibilityOutcome
import com.example.shannon.domain.model.WebsiteAccessibilityPreset
import com.example.shannon.domain.model.WebsiteAccessibilityStatus
import com.example.shannon.presentation.model.DiagnosticsDestination
import java.util.Locale

@StringRes
fun DiagnosticsDestination.titleResId(): Int = when (this) {
    DiagnosticsDestination.Home -> R.string.app_name
    DiagnosticsDestination.HomeSummary -> R.string.screen_network_status
    DiagnosticsDestination.Overview -> R.string.screen_network_overview
    DiagnosticsDestination.ConnectivityTest -> R.string.screen_connectivity_test
    DiagnosticsDestination.DnsAnalysis -> R.string.screen_dns_analysis
    DiagnosticsDestination.ProtocolAnalysis -> R.string.screen_protocol_analysis
    DiagnosticsDestination.TlsAnalysis -> R.string.screen_tls_analysis
    DiagnosticsDestination.SniMitmAnalysis -> R.string.screen_sni_mitm_analysis
    DiagnosticsDestination.PortScan -> R.string.screen_port_scan
    DiagnosticsDestination.PingDiagnostics -> R.string.screen_ping_diagnostics
    DiagnosticsDestination.TracerouteDiagnostics -> R.string.screen_traceroute_diagnostics
    DiagnosticsDestination.ReportExport -> R.string.screen_report_export
    DiagnosticsDestination.WebsiteAccessibility -> R.string.screen_website_accessibility
    DiagnosticsDestination.About -> R.string.screen_about_shannon
}

@StringRes
fun ConnectivityTargetPreset.titleResId(): Int = when (this) {
    ConnectivityTargetPreset.Standard -> R.string.connectivity_preset_standard_title
    ConnectivityTargetPreset.HighCensorship -> R.string.connectivity_preset_high_censorship_title
}

@StringRes
fun ConnectivityTargetPreset.subtitleResId(): Int = when (this) {
    ConnectivityTargetPreset.Standard -> R.string.connectivity_preset_standard_subtitle
    ConnectivityTargetPreset.HighCensorship -> R.string.connectivity_preset_high_censorship_subtitle
}

@StringRes
fun WebsiteAccessibilityPreset.titleResId(): Int = when (this) {
    WebsiteAccessibilityPreset.Quick -> R.string.website_preset_quick_title
    WebsiteAccessibilityPreset.Baseline -> R.string.website_preset_baseline_title
    WebsiteAccessibilityPreset.Extended -> R.string.website_preset_extended_title
    WebsiteAccessibilityPreset.Custom -> R.string.website_preset_custom_title
}

@StringRes
fun WebsiteAccessibilityPreset.subtitleResId(): Int = when (this) {
    WebsiteAccessibilityPreset.Quick -> R.string.website_preset_quick_subtitle
    WebsiteAccessibilityPreset.Baseline -> R.string.website_preset_baseline_subtitle
    WebsiteAccessibilityPreset.Extended -> R.string.website_preset_extended_subtitle
    WebsiteAccessibilityPreset.Custom -> R.string.website_preset_custom_subtitle
}

@StringRes
fun WebsiteAccessibilityCategory.titleResId(): Int = when (this) {
    WebsiteAccessibilityCategory.LocalControl -> R.string.website_category_local_control_title
    WebsiteAccessibilityCategory.ForeignControl -> R.string.website_category_foreign_control_title
    WebsiteAccessibilityCategory.Restricted -> R.string.website_category_restricted_title
    WebsiteAccessibilityCategory.Sensitive -> R.string.website_category_sensitive_title
    WebsiteAccessibilityCategory.Custom -> R.string.website_category_custom_title
}

@StringRes
fun WebsiteAccessibilityCategory.subtitleResId(): Int = when (this) {
    WebsiteAccessibilityCategory.LocalControl -> R.string.website_category_local_control_subtitle
    WebsiteAccessibilityCategory.ForeignControl -> R.string.website_category_foreign_control_subtitle
    WebsiteAccessibilityCategory.Restricted -> R.string.website_category_restricted_subtitle
    WebsiteAccessibilityCategory.Sensitive -> R.string.website_category_sensitive_subtitle
    WebsiteAccessibilityCategory.Custom -> R.string.website_category_custom_subtitle
}

@StringRes
fun WebsiteAccessibilityOutcome.titleResId(): Int = when (this) {
    WebsiteAccessibilityOutcome.Available -> R.string.website_result_available
    WebsiteAccessibilityOutcome.Unstable -> R.string.website_result_unstable
    WebsiteAccessibilityOutcome.Limited -> R.string.website_result_limited
}

@StringRes
fun DnsLookupTransport.titleResId(): Int = when (this) {
    DnsLookupTransport.System -> R.string.dns_transport_system
    DnsLookupTransport.Udp -> R.string.dns_transport_udp
    DnsLookupTransport.TcpFallback -> R.string.dns_transport_tcp_fallback
}

@StringRes
fun DnsAnalysisStatus.titleResId(): Int = when (this) {
    DnsAnalysisStatus.Ok -> R.string.dns_status_ok
    DnsAnalysisStatus.CdnVariation -> R.string.dns_status_variation
    DnsAnalysisStatus.Suspicious -> R.string.status_suspicious
    DnsAnalysisStatus.Blocked -> R.string.status_blocked
}

@StringRes
fun ReportFormat.titleResId(): Int = when (this) {
    ReportFormat.Json -> R.string.report_format_json
    ReportFormat.Markdown -> R.string.report_format_markdown
    ReportFormat.Text -> R.string.report_format_text
}

@StringRes
fun PingPacketState.titleResId(): Int = when (this) {
    PingPacketState.Pending -> R.string.ping_packet_pending
    PingPacketState.Reply -> R.string.ping_packet_reply
    PingPacketState.Lost -> R.string.ping_packet_lost
    PingPacketState.Error -> R.string.ping_packet_error
}

@StringRes
fun PortScanIpVersion.titleResId(): Int = when (this) {
    PortScanIpVersion.IPv4 -> R.string.ip_version_ipv4
    PortScanIpVersion.IPv6 -> R.string.ip_version_ipv6
}

@StringRes
fun PortScanTransport.titleResId(): Int = when (this) {
    PortScanTransport.Tcp -> R.string.transport_tcp
    PortScanTransport.Udp -> R.string.transport_udp
}

@StringRes
fun PortStatus.titleResId(): Int = when (this) {
    PortStatus.OPEN -> R.string.port_status_open
    PortStatus.CLOSED -> R.string.port_status_closed
    PortStatus.FILTERED -> R.string.port_status_filtered
    PortStatus.TIMEOUT -> R.string.port_status_timeout
    PortStatus.ERROR -> R.string.port_status_error
}

@StringRes
fun ProtocolProbeKind.titleResId(): Int = when (this) {
    ProtocolProbeKind.Http11 -> R.string.protocol_http11
    ProtocolProbeKind.Http2 -> R.string.protocol_http2
    ProtocolProbeKind.Http3 -> R.string.protocol_http3
    ProtocolProbeKind.WebSocket -> R.string.protocol_websocket
}

@StringRes
fun ProtocolProbeStatus.titleResId(): Int = when (this) {
    ProtocolProbeStatus.Supported -> R.string.protocol_status_supported
    ProtocolProbeStatus.Failed -> R.string.protocol_status_failed
    ProtocolProbeStatus.Blocked -> R.string.protocol_status_blocked
    ProtocolProbeStatus.Fallback -> R.string.protocol_status_fallback
    ProtocolProbeStatus.Inconclusive -> R.string.status_inconclusive
}

@StringRes
fun ProtocolProbeErrorCategory.titleResId(): Int = when (this) {
    ProtocolProbeErrorCategory.DnsFailure -> R.string.protocol_error_dns_failure
    ProtocolProbeErrorCategory.TcpFailure -> R.string.protocol_error_tcp_failure
    ProtocolProbeErrorCategory.TlsFailure -> R.string.protocol_error_tls_failure
    ProtocolProbeErrorCategory.AlpnFailure -> R.string.protocol_error_alpn_failure
    ProtocolProbeErrorCategory.QuicFailure -> R.string.protocol_error_quic_failure
    ProtocolProbeErrorCategory.HttpFailure -> R.string.protocol_error_http_failure
    ProtocolProbeErrorCategory.WebSocketUpgradeFailure -> R.string.protocol_error_websocket_upgrade_failure
    ProtocolProbeErrorCategory.Timeout -> R.string.port_status_timeout
    ProtocolProbeErrorCategory.Unknown -> R.string.generic_unknown
}

@StringRes
fun SniVariantType.titleResId(): Int = when (this) {
    SniVariantType.NormalSni -> R.string.sni_variant_normal
    SniVariantType.AlternativeSni -> R.string.sni_variant_alternative
    SniVariantType.NoSni -> R.string.sni_variant_none
    SniVariantType.RandomSni -> R.string.sni_variant_random
}

@StringRes
fun SniAnalysisStatus.titleResId(): Int = when (this) {
    SniAnalysisStatus.Normal -> R.string.status_normal
    SniAnalysisStatus.SniFilteringSuspected -> R.string.sni_status_filtering
    SniAnalysisStatus.TlsInterceptionSuspected -> R.string.sni_status_tls_interception
    SniAnalysisStatus.MitmSuspected -> R.string.sni_status_mitm
    SniAnalysisStatus.Inconclusive -> R.string.status_inconclusive
}

@StringRes
fun SniProbeErrorCategory.titleResId(): Int = when (this) {
    SniProbeErrorCategory.DnsFailure -> R.string.protocol_error_dns_failure
    SniProbeErrorCategory.TcpFailure -> R.string.protocol_error_tcp_failure
    SniProbeErrorCategory.TlsFailure -> R.string.protocol_error_tls_failure
    SniProbeErrorCategory.RstDetected -> R.string.sni_error_rst_detected
    SniProbeErrorCategory.Timeout -> R.string.port_status_timeout
    SniProbeErrorCategory.Unknown -> R.string.generic_unknown
}

@StringRes
fun TlsEndpointStatus.titleResId(): Int = when (this) {
    TlsEndpointStatus.Normal -> R.string.status_normal
    TlsEndpointStatus.Failed -> R.string.protocol_status_failed
    TlsEndpointStatus.Inconclusive -> R.string.status_inconclusive
}

@StringRes
fun TlsAnalysisHeuristicStatus.titleResId(): Int = when (this) {
    TlsAnalysisHeuristicStatus.NoTlsAnomalies -> R.string.tls_status_no_anomalies
    TlsAnalysisHeuristicStatus.TlsInterceptionSuspected -> R.string.tls_status_interception
    TlsAnalysisHeuristicStatus.UnusualCertificateChain -> R.string.tls_status_unusual_chain
    TlsAnalysisHeuristicStatus.TlsDowngradeSuspected -> R.string.tls_status_downgrade
    TlsAnalysisHeuristicStatus.Inconclusive -> R.string.status_inconclusive
}

@StringRes
fun WebsiteAccessibilityStatus.titleResId(): Int = when (this) {
    WebsiteAccessibilityStatus.Ok -> R.string.status_ok
    WebsiteAccessibilityStatus.DnsBlocked -> R.string.website_status_dns_blocked
    WebsiteAccessibilityStatus.TcpBlocked -> R.string.website_status_tcp_blocked
    WebsiteAccessibilityStatus.TlsError -> R.string.website_status_tls_error
    WebsiteAccessibilityStatus.HttpError -> R.string.website_status_http_error
}

@StringRes
fun sectionTitleResId(section: String): Int? = when (section) {
    "Overview" -> R.string.report_section_overview
    "Connectivity" -> R.string.report_section_connectivity
    "DNS" -> R.string.report_section_dns
    "Protocols" -> R.string.report_section_protocols
    "TLS" -> R.string.report_section_tls
    "SNI / MITM" -> R.string.report_section_sni_mitm
    "Ping" -> R.string.report_section_ping
    "Traceroute" -> R.string.report_section_traceroute
    "Websites" -> R.string.report_section_websites
    else -> null
}

fun Context.portScanSummaryText(
    openCount: Int,
    closedCount: Int,
    warningCount: Int,
    errorCount: Int,
): String {
    val parts = buildList {
        if (openCount > 0) add(getString(R.string.port_scan_summary_open, openCount))
        if (closedCount > 0) add(getString(R.string.port_scan_summary_closed, closedCount))
        if (warningCount > 0) add(getString(R.string.port_scan_summary_timeout, warningCount))
        if (errorCount > 0) add(getString(R.string.port_scan_summary_error, errorCount))
    }
    return parts.joinToString(" / ")
}

fun Context.millisecondsText(value: Number): String {
    val numeric = value.toDouble()
    val display = if (numeric % 1.0 == 0.0) {
        numeric.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", numeric)
    }
    return getString(R.string.milliseconds_value, display)
}
