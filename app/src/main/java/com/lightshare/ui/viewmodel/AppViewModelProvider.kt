package com.lightshare.ui.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lightshare.LightShareApp

private fun CreationExtras.app(): LightShareApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LightShareApp

object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val c = app().container
            SendViewModel(c.fileRepository, c.settingsRepository, c.historyRepository)
        }
        initializer {
            val c = app().container
            ReceiveViewModel(c.receiveStorage, c.historyRepository)
        }
        initializer {
            val c = app().container
            SettingsViewModel(c.settingsRepository, c.historyRepository)
        }
    }
}
