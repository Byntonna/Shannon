package com.example.shannon.domain.model

data class NetworkOverview(
    val networkType: String,
    val internetReachable: Boolean,
    val validated: Boolean,
    val metered: Boolean,
    val downstreamMbps: String,
    val upstreamMbps: String,
    val ssid: String,
    val bssid: String,
    val signalStrength: String,
    val privateIpAddress: String,
    val gatewayAddress: String,
    val ipv6Address: String,
    val dnsServers: List<String>,
    val carrierName: String,
)
