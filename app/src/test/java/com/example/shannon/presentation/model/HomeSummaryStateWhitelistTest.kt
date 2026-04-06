package com.example.shannon.presentation.model

import com.example.shannon.R
import com.example.shannon.domain.model.ConnectivityTestResult
import com.example.shannon.domain.model.WhitelistZoneCheckResult
import com.example.shannon.domain.model.WhitelistZoneTarget
import com.example.shannon.domain.model.WhitelistZoneTargetGroup
import com.example.shannon.domain.model.WhitelistZoneTargetResult
import com.example.shannon.domain.model.WhitelistZoneVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSummaryStateWhitelistTest {

    @Test
    fun `whitelist zone verdict degrades summary to warning and adds reason`() {
        val state = DiagnosticsUiState(
            whitelistZoneCheckResult = whitelistResult(WhitelistZoneVerdict.InWhitelistZone),
        )

        val summary = state.homeSummaryState()

        assertEquals(HomeSummaryTone.Warning, summary.tone)
        assertTrue(summary.reasons.any { it.titleResId == R.string.home_summary_reason_whitelist_zone_title })
        assertTrue(summary.nextSteps.contains(DiagnosticsDestination.WhitelistZoneCheck))
    }

    @Test
    fun `outside whitelist zone does not degrade otherwise positive summary`() {
        val state = DiagnosticsUiState(
            testResult = successConnectivityResult(),
            whitelistZoneCheckResult = whitelistResult(WhitelistZoneVerdict.OutsideWhitelistZone),
        )

        val summary = state.homeSummaryState()

        assertEquals(HomeSummaryTone.Positive, summary.tone)
    }

    @Test
    fun `inconclusive whitelist result contributes neutral reason`() {
        val state = DiagnosticsUiState(
            whitelistZoneCheckResult = whitelistResult(WhitelistZoneVerdict.Inconclusive),
        )

        val summary = state.homeSummaryState()

        assertEquals(HomeSummaryTone.Neutral, summary.tone)
        assertTrue(summary.reasons.any { it.titleResId == R.string.home_summary_reason_whitelist_inconclusive_title })
    }

    private fun whitelistResult(
        verdict: WhitelistZoneVerdict,
    ): WhitelistZoneCheckResult {
        return WhitelistZoneCheckResult(
            verdict = verdict,
            results = listOf(
                WhitelistZoneTargetResult(
                    target = WhitelistZoneTarget("Yandex", "https://ya.ru", WhitelistZoneTargetGroup.LocalControl),
                    diagnostics = successConnectivityResult(),
                    accessible = true,
                )
            ),
            checkedAt = "10:00:00",
        )
    }

    private fun successConnectivityResult(): ConnectivityTestResult {
        return ConnectivityTestResult(
            steps = listOf(
                com.example.shannon.domain.model.ConnectivityStepResult("DNS", true, "ok"),
                com.example.shannon.domain.model.ConnectivityStepResult("TCP", true, "ok"),
                com.example.shannon.domain.model.ConnectivityStepResult("TLS", true, "ok"),
                com.example.shannon.domain.model.ConnectivityStepResult("HTTP", true, "ok"),
            ),
            checkedAt = "10:00:00",
            endpointLabel = "Test",
            endpointUrl = "https://example.com",
            fallbackUsed = false,
        )
    }
}

