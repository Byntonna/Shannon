package com.example.shannon.domain.model

enum class WebsiteAccessibilityStatus {
    Ok,
    DnsBlocked,
    TcpBlocked,
    TlsError,
    HttpError,
}

data class WebsiteAccessibilityTarget(
    val name: String,
    val url: String,
)

enum class WebsiteAccessibilityPreset(
    val targets: List<WebsiteAccessibilityTarget>,
) {
    Popular(
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
        targets = listOf(
            WebsiteAccessibilityTarget("Google", "https://www.google.com/generate_204"),
            WebsiteAccessibilityTarget("Wikipedia", "https://www.wikipedia.org/"),
            WebsiteAccessibilityTarget("GitHub", "https://github.com/"),
            WebsiteAccessibilityTarget("Telegram", "https://web.telegram.org/"),
        ),
    ),
    Media(
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
