package com.example.shannon.domain.model

enum class WhitelistZoneTargetGroup {
    LocalControl,
    ForeignControl,
    BlockedReference,
}

enum class WhitelistZoneVerdict {
    InWhitelistZone,
    OutsideWhitelistZone,
    NoWhitelistDetectedButReferenceBlocked,
    Inconclusive,
}

data class WhitelistZoneTarget(
    val name: String,
    val url: String,
    val group: WhitelistZoneTargetGroup,
)

data class WhitelistZoneTargetResult(
    val target: WhitelistZoneTarget,
    val diagnostics: ConnectivityTestResult,
    val accessible: Boolean,
)

data class WhitelistZoneCheckResult(
    val verdict: WhitelistZoneVerdict,
    val results: List<WhitelistZoneTargetResult>,
    val checkedAt: String,
)

val defaultWhitelistZoneTargets = listOf(
    WhitelistZoneTarget("Yandex", "https://ya.ru", WhitelistZoneTargetGroup.LocalControl),
    WhitelistZoneTarget("Mail.ru", "https://mail.ru", WhitelistZoneTargetGroup.LocalControl),
    WhitelistZoneTarget("VK ID", "https://id.vk.ru", WhitelistZoneTargetGroup.LocalControl),
    WhitelistZoneTarget("Rutube", "https://rutube.ru", WhitelistZoneTargetGroup.LocalControl),
    WhitelistZoneTarget("Ozon", "https://ozon.ru", WhitelistZoneTargetGroup.LocalControl),
    WhitelistZoneTarget(
        "Firefox captive portal",
        "https://detectportal.firefox.com/canonical.html",
        WhitelistZoneTargetGroup.ForeignControl,
    ),
    WhitelistZoneTarget(
        "Google",
        "https://google.com",
        WhitelistZoneTargetGroup.ForeignControl,
    ),
    WhitelistZoneTarget(
        "Microsoft",
        "https://www.microsoft.com",
        WhitelistZoneTargetGroup.ForeignControl,
    ),
    WhitelistZoneTarget(
        "Android connectivity check",
        "https://www.google.com/generate_204",
        WhitelistZoneTargetGroup.ForeignControl,
    ),
    WhitelistZoneTarget("Tor Project", "https://www.torproject.org", WhitelistZoneTargetGroup.BlockedReference),
    WhitelistZoneTarget("OONI Explorer", "https://explorer.ooni.org", WhitelistZoneTargetGroup.BlockedReference),
)

fun evaluateWhitelistZoneVerdict(
    results: List<WhitelistZoneTargetResult>,
): WhitelistZoneVerdict {
    val localTargets = results.filter { it.target.group == WhitelistZoneTargetGroup.LocalControl }
    val foreignTargets = results.filter { it.target.group == WhitelistZoneTargetGroup.ForeignControl }
    val blockedTargets = results.filter { it.target.group == WhitelistZoneTargetGroup.BlockedReference }

    val allLocalAccessible = localTargets.isNotEmpty() && localTargets.all { it.accessible }
    val foreignAccessibleCount = foreignTargets.count { it.accessible }
    val foreignBlockedCount = foreignTargets.count { !it.accessible }
    val blockedAccessibleCount = blockedTargets.count { it.accessible }
    val blockedUnavailableCount = blockedTargets.count { !it.accessible }

    return when {
        allLocalAccessible &&
            foreignBlockedCount >= 2 &&
            blockedUnavailableCount >= 1 -> WhitelistZoneVerdict.InWhitelistZone
        allLocalAccessible &&
            foreignAccessibleCount >= 2 &&
            blockedUnavailableCount >= 1 -> WhitelistZoneVerdict.NoWhitelistDetectedButReferenceBlocked
        allLocalAccessible &&
            foreignAccessibleCount >= 2 &&
            blockedAccessibleCount == blockedTargets.size &&
            blockedTargets.isNotEmpty() -> WhitelistZoneVerdict.OutsideWhitelistZone
        else -> WhitelistZoneVerdict.Inconclusive
    }
}
