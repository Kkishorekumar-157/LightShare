package com.lightshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.lightshare.ui.navigation.LightShareNavHost
import com.lightshare.ui.theme.LightShareTheme
import com.lightshare.ui.viewmodel.AppViewModelProvider
import com.lightshare.ui.viewmodel.ReceiveViewModel
import com.lightshare.ui.viewmodel.SendViewModel
import com.lightshare.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    // Activity-scoped so an in-progress transfer survives rotation/recreation.
    private val sendVm: SendViewModel by viewModels { AppViewModelProvider.Factory }
    private val receiveVm: ReceiveViewModel by viewModels { AppViewModelProvider.Factory }
    private val settingsVm: SettingsViewModel by viewModels { AppViewModelProvider.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LightShareTheme { LightShareNavHost(sendVm, receiveVm, settingsVm) } }
    }
}
