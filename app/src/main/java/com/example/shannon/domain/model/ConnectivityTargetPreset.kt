package com.example.shannon.domain.model

data class ConnectivityTarget(
    val label: String,
    val url: String,
)

enum class ConnectivityTargetPreset(
    val endpoints: List<ConnectivityTarget>,
) {
    Standard(
        endpoints = listOf(
            ConnectivityTarget(
                label = "Cloudflare Trace",
                url = "https://www.cloudflare.com/cdn-cgi/trace",
            ),
            ConnectivityTarget(
                label = "Google Connectivity Check",
                url = "https://connectivitycheck.gstatic.com/generate_204",
            ),
        ),
    ),
    HighCensorship(
        endpoints = listOf(
            ConnectivityTarget(
                label = "Firefox Detect Portal",
                url = "https://detectportal.firefox.com/success.txt",
            ),
            ConnectivityTarget(
                label = "Microsoft Connect Test",
                url = "https://www.msftconnecttest.com/connecttest.txt",
            ),
        ),
    ),
}
