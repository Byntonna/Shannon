package com.example.shannon.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WhitelistZoneVerdictTest {

    @Test
    fun `returns in whitelist zone when local targets work and external references fail`() {
        val results = listOf(
            result("Yandex", WhitelistZoneTargetGroup.LocalControl, true),
            result("2GIS", WhitelistZoneTargetGroup.LocalControl, true),
            result("VK ID", WhitelistZoneTargetGroup.LocalControl, true),
            result("Sber Online", WhitelistZoneTargetGroup.LocalControl, true),
            result("Firefox", WhitelistZoneTargetGroup.ForeignControl, false),
            result("NCSI", WhitelistZoneTargetGroup.ForeignControl, false),
            result("Android", WhitelistZoneTargetGroup.ForeignControl, true),
            result("Tor", WhitelistZoneTargetGroup.BlockedReference, false),
            result("OONI", WhitelistZoneTargetGroup.BlockedReference, true),
        )

        assertEquals(
            WhitelistZoneVerdict.InWhitelistZone,
            evaluateWhitelistZoneVerdict(results),
        )
    }

    @Test
    fun `returns outside whitelist zone when foreign and blocked references stay reachable`() {
        val results = listOf(
            result("Yandex", WhitelistZoneTargetGroup.LocalControl, true),
            result("2GIS", WhitelistZoneTargetGroup.LocalControl, true),
            result("VK ID", WhitelistZoneTargetGroup.LocalControl, true),
            result("Sber Online", WhitelistZoneTargetGroup.LocalControl, true),
            result("Firefox", WhitelistZoneTargetGroup.ForeignControl, true),
            result("NCSI", WhitelistZoneTargetGroup.ForeignControl, true),
            result("Android", WhitelistZoneTargetGroup.ForeignControl, false),
            result("Tor", WhitelistZoneTargetGroup.BlockedReference, true),
            result("OONI", WhitelistZoneTargetGroup.BlockedReference, true),
        )

        assertEquals(
            WhitelistZoneVerdict.OutsideWhitelistZone,
            evaluateWhitelistZoneVerdict(results),
        )
    }

    @Test
    fun `returns no whitelist detected but reference blocked when foreign targets work and blocked references fail`() {
        val results = listOf(
            result("Yandex", WhitelistZoneTargetGroup.LocalControl, true),
            result("2GIS", WhitelistZoneTargetGroup.LocalControl, true),
            result("VK ID", WhitelistZoneTargetGroup.LocalControl, true),
            result("Sber Online", WhitelistZoneTargetGroup.LocalControl, true),
            result("Firefox", WhitelistZoneTargetGroup.ForeignControl, true),
            result("NCSI", WhitelistZoneTargetGroup.ForeignControl, true),
            result("Android", WhitelistZoneTargetGroup.ForeignControl, true),
            result("Tor", WhitelistZoneTargetGroup.BlockedReference, false),
            result("OONI", WhitelistZoneTargetGroup.BlockedReference, true),
        )

        assertEquals(
            WhitelistZoneVerdict.NoWhitelistDetectedButReferenceBlocked,
            evaluateWhitelistZoneVerdict(results),
        )
    }

    @Test
    fun `returns inconclusive for mixed partial pattern`() {
        val results = listOf(
            result("Yandex", WhitelistZoneTargetGroup.LocalControl, true),
            result("2GIS", WhitelistZoneTargetGroup.LocalControl, true),
            result("VK ID", WhitelistZoneTargetGroup.LocalControl, true),
            result("Sber Online", WhitelistZoneTargetGroup.LocalControl, true),
            result("Firefox", WhitelistZoneTargetGroup.ForeignControl, false),
            result("NCSI", WhitelistZoneTargetGroup.ForeignControl, true),
            result("Tor", WhitelistZoneTargetGroup.BlockedReference, false),
            result("OONI", WhitelistZoneTargetGroup.BlockedReference, false),
        )

        assertEquals(
            WhitelistZoneVerdict.Inconclusive,
            evaluateWhitelistZoneVerdict(results),
        )
    }

    @Test
    fun `returns inconclusive when a local control fails`() {
        val results = listOf(
            result("Yandex", WhitelistZoneTargetGroup.LocalControl, false),
            result("2GIS", WhitelistZoneTargetGroup.LocalControl, true),
            result("VK ID", WhitelistZoneTargetGroup.LocalControl, true),
            result("Sber Online", WhitelistZoneTargetGroup.LocalControl, true),
            result("Firefox", WhitelistZoneTargetGroup.ForeignControl, false),
            result("NCSI", WhitelistZoneTargetGroup.ForeignControl, false),
            result("Android", WhitelistZoneTargetGroup.ForeignControl, false),
            result("Tor", WhitelistZoneTargetGroup.BlockedReference, false),
            result("OONI", WhitelistZoneTargetGroup.BlockedReference, false),
        )

        assertEquals(
            WhitelistZoneVerdict.Inconclusive,
            evaluateWhitelistZoneVerdict(results),
        )
    }

    private fun result(
        name: String,
        group: WhitelistZoneTargetGroup,
        accessible: Boolean,
    ): WhitelistZoneTargetResult {
        return WhitelistZoneTargetResult(
            target = WhitelistZoneTarget(name, "https://$name.test", group),
            diagnostics = ConnectivityTestResult(
                steps = emptyList(),
                checkedAt = "00:00:00",
                endpointLabel = name,
                endpointUrl = "https://$name.test",
                fallbackUsed = false,
            ),
            accessible = accessible,
        )
    }
}
