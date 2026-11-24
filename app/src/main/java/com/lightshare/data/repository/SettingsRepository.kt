package com.lightshare.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lightshare.data.model.AppSettings
import com.lightshare.qr.generator.QrEcLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val context: Context) {
    private val fps = intPreferencesKey("fps")
    private val block = intPreferencesKey("block_size")
    private val ec = stringPreferencesKey("ec_level")

    val settings: Flow<AppSettings> = context.lightShareDataStore.data.map { p ->
        AppSettings(
            framesPerSecond = p[fps] ?: 6,
            blockSize = p[block] ?: 256,
            ecLevel = p[ec]?.let { s -> QrEcLevel.entries.firstOrNull { it.name == s } } ?: QrEcLevel.M
        )
    }

    suspend fun setFramesPerSecond(v: Int) { context.lightShareDataStore.edit { it[fps] = v } }
    suspend fun setBlockSize(v: Int) { context.lightShareDataStore.edit { it[block] = v } }
    suspend fun setEcLevel(v: QrEcLevel) { context.lightShareDataStore.edit { it[ec] = v.name } }
}
