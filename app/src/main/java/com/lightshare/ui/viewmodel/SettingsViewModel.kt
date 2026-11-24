package com.lightshare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightshare.data.model.AppSettings
import com.lightshare.data.model.HistoryEntry
import com.lightshare.data.repository.HistoryRepository
import com.lightshare.data.repository.SettingsRepository
import com.lightshare.qr.generator.QrEcLevel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val historyRepo: HistoryRepository
) : ViewModel() {
    val settings: StateFlow<AppSettings> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    val history: StateFlow<List<HistoryEntry>> =
        historyRepo.entries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFps(v: Int) = viewModelScope.launch { settingsRepo.setFramesPerSecond(v) }
    fun setBlockSize(v: Int) = viewModelScope.launch { settingsRepo.setBlockSize(v) }
    fun setEcLevel(v: QrEcLevel) = viewModelScope.launch { settingsRepo.setEcLevel(v) }
    fun clearHistory() = viewModelScope.launch { historyRepo.clear() }
}
