package com.example.shannon.domain.model

enum class WebsiteAccessibilityCategory {
    LocalControl,
    ForeignControl,
    Restricted,
    Sensitive,
    Custom,
}

enum class WebsiteAccessibilityStatus {
    Ok,
    DnsBlocked,
    TcpBlocked,
    TlsError,
    HttpError,
}

enum class WebsiteAccessibilityOutcome {
    Available,
    Unstable,
    Limited,
}

data class WebsiteAccessibilityTarget(
    val name: String,
    val url: String,
    val category: WebsiteAccessibilityCategory = WebsiteAccessibilityCategory.Custom,
)

enum class WebsiteAccessibilityPreset(
    val targets: List<WebsiteAccessibilityTarget>,
) {
    Quick(
        targets = listOf(
            WebsiteAccessibilityTarget("Yandex", "https://ya.ru/", WebsiteAccessibilityCategory.LocalControl),
            WebsiteAccessibilityTarget("Google", "https://www.google.com/generate_204", WebsiteAccessibilityCategory.ForeignControl),
            WebsiteAccessibilityTarget("GitHub", "https://github.com/", WebsiteAccessibilityCategory.ForeignControl),
            WebsiteAccessibilityTarget("YouTube", "https://www.youtube.com/", WebsiteAccessibilityCategory.Restricted),
            WebsiteAccessibilityTarget("X", "https://x.com/", WebsiteAccessibilityCategory.Restricted),
        ),
    ),
    Baseline(
        targets = listOf(
            WebsiteAccessibilityTarget("Yandex", "https://ya.ru/", WebsiteAccessibilityCategory.LocalControl),
            WebsiteAccessibilityTarget("VK", "https://vk.com/", WebsiteAccessibilityCategory.LocalControl),
            WebsiteAccessibilityTarget("Gosuslugi", "https://www.gosuslugi.ru/", WebsiteAccessibilityCategory.LocalControl),
            WebsiteAccessibilityTarget("Google", "https://www.google.com/generate_204", WebsiteAccessibilityCategory.ForeignControl),
            WebsiteAccessibilityTarget("GitHub", "https://github.com/", WebsiteAccessibilityCategory.ForeignControl),
            WebsiteAccessibilityTarget("Wikipedia", "https://www.wikipedia.org/", WebsiteAccessibilityCategory.ForeignControl),
            WebsiteAccessibilityTarget("YouTube", "https://www.youtube.com/", WebsiteAccessibilityCategory.Restricted),
            WebsiteAccessibilityTarget("X", "https://x.com/", WebsiteAccessibilityCategory.Restricted),
        ),
    ),
    Extended(
        targets = listOf(
            WebsiteAccessibilityTarget("Yandex", "https://ya.ru/", WebsiteAccessibilityCategory.LocalControl),
            WebsiteAccessibilityTarget("VK", "https://vk.com/", WebsiteAccessibilityCategory.LocalControl),
            WebsiteAccessibilityTarget("Gosuslugi", "https://www.gosuslugi.ru/", WebsiteAccessibilityCategory.LocalControl),
            WebsiteAccessibilityTarget("Dzen", "https://dzen.ru/", WebsiteAccessibilityCategory.LocalControl),
            WebsiteAccessibilityTarget("Google", "https://www.google.com/generate_204", WebsiteAccessibilityCategory.ForeignControl),
            WebsiteAccessibilityTarget("GitHub", "https://github.com/", WebsiteAccessibilityCategory.ForeignControl),
            WebsiteAccessibilityTarget("Wikipedia", "https://www.wikipedia.org/", WebsiteAccessibilityCategory.ForeignControl),
            WebsiteAccessibilityTarget("Cloudflare", "https://www.cloudflare.com/", WebsiteAccessibilityCategory.ForeignControl),
            WebsiteAccessibilityTarget("Microsoft", "https://www.microsoft.com/", WebsiteAccessibilityCategory.ForeignControl),
            WebsiteAccessibilityTarget("YouTube", "https://www.youtube.com/", WebsiteAccessibilityCategory.Restricted),
            WebsiteAccessibilityTarget("youtu.be", "https://youtu.be/", WebsiteAccessibilityCategory.Restricted),
            WebsiteAccessibilityTarget("X", "https://x.com/", WebsiteAccessibilityCategory.Restricted),
            WebsiteAccessibilityTarget("Telegram", "https://telegram.org/", WebsiteAccessibilityCategory.Sensitive),
            WebsiteAccessibilityTarget("Web Telegram", "https://web.telegram.org/", WebsiteAccessibilityCategory.Sensitive),
        ),
    ),
    Custom(
        targets = emptyList(),
    ),
}

data class WebsiteAccessibilityResult(
    val serviceName: String,
    val targetUrl: String,
    val status: WebsiteAccessibilityStatus,
    val diagnostics: ConnectivityTestResult,
)

fun WebsiteAccessibilityStatus.toOutcome(): WebsiteAccessibilityOutcome = when (this) {
    WebsiteAccessibilityStatus.Ok -> WebsiteAccessibilityOutcome.Available
    WebsiteAccessibilityStatus.HttpError -> WebsiteAccessibilityOutcome.Unstable
    WebsiteAccessibilityStatus.DnsBlocked,
    WebsiteAccessibilityStatus.TcpBlocked,
    WebsiteAccessibilityStatus.TlsError -> WebsiteAccessibilityOutcome.Limited
}
