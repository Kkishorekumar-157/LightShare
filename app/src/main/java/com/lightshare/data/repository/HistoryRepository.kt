package com.lightshare.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lightshare.data.model.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Transfer history, stored as a small JSON list in DataStore (max 50 entries). Room was unnecessary for this. */
class HistoryRepository(private val context: Context) {
    private val key = stringPreferencesKey("history_json")
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(HistoryEntry.serializer())

    val entries: Flow<List<HistoryEntry>> = context.lightShareDataStore.data.map { decode(it) }

    suspend fun add(entry: HistoryEntry) {
        context.lightShareDataStore.edit { p ->
            val list = (listOf(entry) + decode(p)).take(50)
            p[key] = json.encodeToString(serializer, list)
        }
    }

    suspend fun clear() { context.lightShareDataStore.edit { it.remove(key) } }

    private fun decode(p: Preferences): List<HistoryEntry> =
        p[key]?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() } ?: emptyList()
}
