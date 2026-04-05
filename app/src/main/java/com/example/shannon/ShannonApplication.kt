package com.example.shannon

import android.app.Application
import com.example.shannon.data.AndroidNetworkDiagnosticsRepository
import com.example.shannon.data.AndroidPortScanRepository
import com.example.shannon.data.HomeDashboardStatusStore
import com.example.shannon.data.ShannonDatabase
import com.example.shannon.domain.usecase.ExportReportUseCase
import com.example.shannon.domain.usecase.ReadNetworkOverviewUseCase
import com.example.shannon.domain.usecase.RunConnectivityTestUseCase
import com.example.shannon.domain.usecase.RunDnsAnalysisUseCase
import com.example.shannon.domain.usecase.RunPingUseCase
import com.example.shannon.domain.usecase.RunProtocolAnalysisUseCase
import com.example.shannon.domain.usecase.RunSniMitmAnalysisUseCase
import com.example.shannon.domain.usecase.RunTlsAnalysisUseCase
import com.example.shannon.domain.usecase.RunTracerouteUseCase
import com.example.shannon.domain.usecase.RunWebsiteAccessibilityTestUseCase
import com.example.shannon.domain.usecase.ScanPortUseCase

class ShannonApplication : Application() {
    lateinit var appContainer: ShannonAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = ShannonAppContainer(this)
    }
}

class ShannonAppContainer(
    application: Application,
) {
    private val appContext = application.applicationContext
    private val networkRepository = AndroidNetworkDiagnosticsRepository(appContext)
    private val portScanRepository = AndroidPortScanRepository(appContext)
    private val database = ShannonDatabase.getInstance(appContext)
    private val homeDashboardStatusStore = HomeDashboardStatusStore(
        context = appContext,
        dashboardStatusDao = database.dashboardStatusDao(),
    )

    val readNetworkOverview = ReadNetworkOverviewUseCase(networkRepository)
    val runConnectivityTest = RunConnectivityTestUseCase(networkRepository)
    val runWebsiteAccessibilityTest = RunWebsiteAccessibilityTestUseCase(networkRepository)
    val runDnsAnalysis = RunDnsAnalysisUseCase(networkRepository)
    val runProtocolAnalysis = RunProtocolAnalysisUseCase(networkRepository)
    val runTlsAnalysis = RunTlsAnalysisUseCase(networkRepository)
    val runSniMitmAnalysis = RunSniMitmAnalysisUseCase(networkRepository)
    val scanPort = ScanPortUseCase(portScanRepository)
    val runPing = RunPingUseCase(networkRepository)
    val runTraceroute = RunTracerouteUseCase(networkRepository)
    val exportDiagnosticReport = ExportReportUseCase(networkRepository)
    val dashboardStatusStore = homeDashboardStatusStore
}
