package com.example.shannon.domain.model

enum class WebsiteAccessibilityStatus(val title: String) {
    Ok("OK"),
    DnsBlocked("DNS blocked"),
    TcpBlocked("TCP blocked"),
    TlsError("TLS error"),
    HttpError("HTTP error"),
}

data class WebsiteAccessibilityTarget(
    val name: String,
    val url: String,
)

enum class WebsiteAccessibilityPreset(
    val title: String,
    val subtitle: String,
    val targets: List<WebsiteAccessibilityTarget>,
) {
    Popular(
        title = "Popular",
        subtitle = "All major services",
        targets = listOf(
            WebsiteAccessibilityTarget("Google", "https://www.google.com/generate_204"),
            WebsiteAccessibilityTarget("YouTube", "https://www.youtube.com/"),
            WebsiteAccessibilityTarget("Wikipedia", "https://www.wikipedia.org/"),
            WebsiteAccessibilityTarget("GitHub", "https://github.com/"),
            WebsiteAccessibilityTarget("Telegram", "https://web.telegram.org/"),
            WebsiteAccessibilityTarget("Twitter / X", "https://x.com/"),
        ),
    ),
    Core(
        title = "Core",
        subtitle = "Foundational web services",
        targets = listOf(
            WebsiteAccessibilityTarget("Google", "https://www.google.com/generate_204"),
            WebsiteAccessibilityTarget("Wikipedia", "https://www.wikipedia.org/"),
            WebsiteAccessibilityTarget("GitHub", "https://github.com/"),
            WebsiteAccessibilityTarget("Telegram", "https://web.telegram.org/"),
        ),
    ),
    Media(
        title = "Media",
        subtitle = "Video and social platforms",
        targets = listOf(
            WebsiteAccessibilityTarget("YouTube", "https://www.youtube.com/"),
            WebsiteAccessibilityTarget("Telegram", "https://web.telegram.org/"),
            WebsiteAccessibilityTarget("Twitter / X", "https://x.com/"),
        ),
    ),
}

data class WebsiteAccessibilityResult(
    val serviceName: String,
    val targetUrl: String,
    val status: WebsiteAccessibilityStatus,
    val diagnostics: ConnectivityTestResult,
)
