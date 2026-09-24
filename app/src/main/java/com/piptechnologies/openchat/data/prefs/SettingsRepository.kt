package com.piptechnologies.openchat.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.piptechnologies.openchat.core.send.MessagingApp
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** App settings kept in DataStore Preferences (§5.5). Each flow emits its default until the key is first written. */
interface SettingsRepository {
    val onboardingDone: Flow<Boolean>
    suspend fun setOnboardingDone()

    /** The app Send opens; null until one is chosen. */
    val sendApp: Flow<MessagingApp?>
    suspend fun setSendApp(app: MessagingApp)

    val recoveryPaused: Flow<Boolean>
    suspend fun setRecoveryPaused(paused: Boolean)

    val secondLinked: Flow<Boolean>
    suspend fun setSecondLinked(linked: Boolean)

    /** BCP-47 language tag, default "en". */
    val language: Flow<String>
    suspend fun setLanguage(tag: String)

    val sendCount: Flow<Int>

    /** Counts one more successful send and returns the new count. */
    suspend fun incrementSendCount(): Int

    val ratingShown: Flow<Boolean>
    suspend fun setRatingShown()
}

class DataStoreSettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) : SettingsRepository {
    /** A failed read (an IOException, file corruption included) reads as the defaults instead of failing collectors. */
    private val preferences: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    override val onboardingDone: Flow<Boolean> = flag(ONBOARDING_DONE)

    override suspend fun setOnboardingDone() {
        dataStore.edit { it[ONBOARDING_DONE] = true }
    }

    override val sendApp: Flow<MessagingApp?> = preferences.map { MessagingApp.fromName(it[SEND_APP]) }.distinctUntilChanged()

    override suspend fun setSendApp(app: MessagingApp) {
        dataStore.edit { it[SEND_APP] = app.name }
    }

    override val recoveryPaused: Flow<Boolean> = flag(RECOVERY_PAUSED)

    override suspend fun setRecoveryPaused(paused: Boolean) {
        dataStore.edit { it[RECOVERY_PAUSED] = paused }
    }

    override val secondLinked: Flow<Boolean> = flag(SECOND_LINKED)

    override suspend fun setSecondLinked(linked: Boolean) {
        dataStore.edit { it[SECOND_LINKED] = linked }
    }

    override val language: Flow<String> = preferences.map { it[LANGUAGE] ?: DEFAULT_LANGUAGE }.distinctUntilChanged()

    override suspend fun setLanguage(tag: String) {
        dataStore.edit { it[LANGUAGE] = tag }
    }

    override val sendCount: Flow<Int> = preferences.map { it[SEND_COUNT] ?: 0 }.distinctUntilChanged()

    override suspend fun incrementSendCount(): Int {
        val updated = dataStore.edit { it[SEND_COUNT] = (it[SEND_COUNT] ?: 0) + 1 }
        return updated[SEND_COUNT] ?: 0
    }

    override val ratingShown: Flow<Boolean> = flag(RATING_SHOWN)

    override suspend fun setRatingShown() {
        dataStore.edit { it[RATING_SHOWN] = true }
    }

    private fun flag(key: Preferences.Key<Boolean>): Flow<Boolean> = preferences.map { it[key] ?: false }.distinctUntilChanged()

    private companion object {
        const val DEFAULT_LANGUAGE = "en"
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val SEND_APP = stringPreferencesKey("send_app")
        val RECOVERY_PAUSED = booleanPreferencesKey("recovery_paused")
        val SECOND_LINKED = booleanPreferencesKey("second_linked")
        val LANGUAGE = stringPreferencesKey("language")
        val SEND_COUNT = intPreferencesKey("send_count")
        val RATING_SHOWN = booleanPreferencesKey("rating_shown")
    }
}
