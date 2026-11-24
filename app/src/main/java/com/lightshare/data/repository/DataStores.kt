package com.lightshare.data.repository

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.lightShareDataStore by preferencesDataStore(name = "lightshare")
