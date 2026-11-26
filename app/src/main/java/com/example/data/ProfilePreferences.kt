package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mench_profile")

data class UserProfile(
    val playerName: String = "یاسر",
    val frameId: Int = 0,
    val preferredTeamIndex: Int = 0,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val autoMoveSinglePawn: Boolean = false
)

class ProfilePreferences(private val context: Context) {

    private object Keys {
        val PLAYER_NAME = stringPreferencesKey("player_name")
        val FRAME_ID = intPreferencesKey("frame_id")
        val PREFERRED_TEAM = intPreferencesKey("preferred_team")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val AUTO_MOVE_SINGLE = booleanPreferencesKey("auto_move_single")
    }

    val profileFlow: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            playerName = prefs[Keys.PLAYER_NAME]?.takeIf { it.isNotBlank() } ?: "یاسر",
            frameId = (prefs[Keys.FRAME_ID] ?: 0).coerceIn(0, 3),
            preferredTeamIndex = (prefs[Keys.PREFERRED_TEAM] ?: 0).coerceIn(0, 3),
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            autoMoveSinglePawn = prefs[Keys.AUTO_MOVE_SINGLE] ?: false
        )
    }

    suspend fun updateProfile(
        name: String,
        frameId: Int,
        preferredTeamIndex: Int
    ) {
        val cleanName = name.trim().take(14).ifBlank { "بازیکن" }
        context.dataStore.edit { prefs ->
            prefs[Keys.PLAYER_NAME] = cleanName
            prefs[Keys.FRAME_ID] = frameId.coerceIn(0, 3)
            prefs[Keys.PREFERRED_TEAM] = preferredTeamIndex.coerceIn(0, 3)
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setAutoMoveSinglePawn(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_MOVE_SINGLE] = enabled }
    }
}
