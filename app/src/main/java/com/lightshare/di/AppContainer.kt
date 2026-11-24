package com.lightshare.di

import android.content.Context
import com.lightshare.data.repository.FileRepository
import com.lightshare.data.repository.HistoryRepository
import com.lightshare.data.repository.SettingsRepository
import com.lightshare.data.storage.ReceiveStorage

/** Manual DI: three screens' worth of dependencies don't justify Hilt's kapt/ksp build cost and version coupling. */
class AppContainer(context: Context) {
    private val app = context.applicationContext
    val settingsRepository = SettingsRepository(app)
    val historyRepository = HistoryRepository(app)
    val fileRepository = FileRepository(app)
    val receiveStorage = ReceiveStorage(app)
}
