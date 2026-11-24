package com.lightshare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lightshare.ui.screens.*
import com.lightshare.ui.viewmodel.*

@Composable
fun LightShareNavHost(send: SendViewModel, receive: ReceiveViewModel, settings: SettingsViewModel) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen { nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } } }
        }
        composable(Routes.HOME) {
            val history by settings.history.collectAsStateWithLifecycle()
            HomeScreen(history,
                onSend = { nav.navigate(Routes.SEND) }, onReceive = { nav.navigate(Routes.RECEIVE) },
                onHistory = { nav.navigate(Routes.HISTORY) }, onSettings = { nav.navigate(Routes.SETTINGS) })
        }
        composable(Routes.SEND) {
            val ui by send.ui.collectAsStateWithLifecycle()
            SendScreen(ui, send::onFilePicked, { nav.navigate(Routes.FILE_INFO) }, { nav.popBackStack() })
        }
        composable(Routes.FILE_INFO) {
            val ui by send.ui.collectAsStateWithLifecycle()
            val s by settings.settings.collectAsStateWithLifecycle()
            FileInfoScreen(ui, s, onStart = { send.startTransmission(); nav.navigate(Routes.TRANSMIT) }, onBack = { nav.popBackStack() })
        }
        composable(Routes.TRANSMIT) {
            val ui by send.ui.collectAsStateWithLifecycle()
            TransmissionScreen(ui, send::togglePause, onCancel = {
                send.stop(); nav.popBackStack(Routes.HOME, inclusive = false)
            })
        }
        composable(Routes.RECEIVE) {
            ReceiveScreen(onStart = { receive.startReceiving(); nav.navigate(Routes.SCAN) }, onBack = { nav.popBackStack() })
        }
        composable(Routes.SCAN) {
            val ui by receive.ui.collectAsStateWithLifecycle()
            LaunchedEffect(ui.phase) {
                if (ui.phase == ReceivePhase.COMPLETE) nav.navigate(Routes.COMPLETE) { popUpTo(Routes.HOME) }
            }
            ScannerScreen(ui, receive::onQrText, receive::setTorch, receive::setTorchAvailable,
                onDetails = { nav.navigate(Routes.PROGRESS) },
                onCancel = { receive.cancel(); nav.popBackStack(Routes.HOME, inclusive = false) },
                onRetry = { receive.startReceiving() })
        }
        composable(Routes.PROGRESS) {
            val ui by receive.ui.collectAsStateWithLifecycle()
            TransferProgressScreen(ui) { nav.popBackStack() }
        }
        composable(Routes.COMPLETE) {
            val ui by receive.ui.collectAsStateWithLifecycle()
            CompletionScreen(ui, onSave = receive::saveTo, onDone = {
                receive.cancel(); nav.popBackStack(Routes.HOME, inclusive = false)
            })
        }
        composable(Routes.HISTORY) {
            val history by settings.history.collectAsStateWithLifecycle()
            HistoryScreen(history, { nav.popBackStack() }, { settings.clearHistory() })
        }
        composable(Routes.SETTINGS) {
            val s by settings.settings.collectAsStateWithLifecycle()
            SettingsScreen(s, { settings.setFps(it) }, { settings.setBlockSize(it) }, { settings.setEcLevel(it) }, { nav.popBackStack() })
        }
    }
}
