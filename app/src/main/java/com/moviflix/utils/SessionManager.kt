package com.moviflix.utils

import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.UUID

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "moviflix_prefs")

class SessionManager(private val context: Context) {

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_USER_TYPE = stringPreferencesKey("user_type")
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_TIMEZONE = stringPreferencesKey("timezone")
        private val KEY_MCC = stringPreferencesKey("mcc")
        private val KEY_USER_ID = longPreferencesKey("user_id")
        private val KEY_NICK_NAME = stringPreferencesKey("nick_name")
        private val KEY_HEAD_IMG = stringPreferencesKey("head_img")
        private val KEY_EMAIL = stringPreferencesKey("email")
        private val KEY_IS_VIP = booleanPreferencesKey("is_vip")
        private val KEY_DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        private val KEY_SUBTITLE_LANG = stringPreferencesKey("subtitle_lang")
        private val KEY_AUDIO_LANG = stringPreferencesKey("audio_lang")
    }

    // Sync reads for interceptor use (called on background threads only)
    private fun prefs() = runBlocking { context.dataStore.data.first() }

    var token: String?
        get() = prefs()[KEY_TOKEN]
        set(value) = runBlocking {
            context.dataStore.edit { it[KEY_TOKEN] = value ?: "" }
        }

    var userType: String?
        get() = prefs()[KEY_USER_TYPE]
        set(value) = runBlocking {
            context.dataStore.edit { it[KEY_USER_TYPE] = value ?: "" }
        }

    val deviceId: String
        get() {
            val stored = prefs()[KEY_DEVICE_ID]
            if (!stored.isNullOrEmpty()) return stored
            // Generate a stable device ID
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            val id = if (!androidId.isNullOrEmpty() && androidId != "9774d56d682e549c")
                androidId
            else
                UUID.randomUUID().toString().replace("-", "").take(16)
            runBlocking { context.dataStore.edit { it[KEY_DEVICE_ID] = id } }
            return id
        }

    var language: String
        get() = prefs()[KEY_LANGUAGE] ?: "ar"
        set(value) = runBlocking {
            context.dataStore.edit { it[KEY_LANGUAGE] = value }
        }

    var timezone: String
        get() = prefs()[KEY_TIMEZONE] ?: "GMT+02:00"
        set(value) = runBlocking {
            context.dataStore.edit { it[KEY_TIMEZONE] = value }
        }

    var mcc: String
        get() = prefs()[KEY_MCC] ?: "602"
        set(value) = runBlocking {
            context.dataStore.edit { it[KEY_MCC] = value }
        }

    var userId: Long
        get() = prefs()[KEY_USER_ID] ?: 0L
        set(value) = runBlocking {
            context.dataStore.edit { it[KEY_USER_ID] = value }
        }

    var nickName: String
        get() = prefs()[KEY_NICK_NAME] ?: ""
        set(value) = runBlocking {
            context.dataStore.edit { it[KEY_NICK_NAME] = value }
        }

    var headImg: String
        get() = prefs()[KEY_HEAD_IMG] ?: ""
        set(value) = runBlocking {
            context.dataStore.edit { it[KEY_HEAD_IMG] = value }
        }

    var email: String
        get() = prefs()[KEY_EMAIL] ?: ""
        set(value) = runBlocking {
            context.dataStore.edit { it[KEY_EMAIL] = value }
        }

    var isVip: Boolean
        get() = prefs()[KEY_IS_VIP] ?: false
        set(value) = runBlocking {
            context.dataStore.edit { it[KEY_IS_VIP] = value }
        }

    var defaultQuality: String
        get() = prefs()[KEY_DEFAULT_QUALITY] ?: "GROOT_HD"
        set(value) = runBlocking {
            context.dataStore.edit { it[KEY_DEFAULT_QUALITY] = value }
        }

    var subtitleLanguage: String
        get() = prefs()[KEY_SUBTITLE_LANG] ?: "ar"
        set(value) = runBlocking {
            context.dataStore.edit { it[KEY_SUBTITLE_LANG] = value }
        }

    var audioLanguage: String
        get() = prefs()[KEY_AUDIO_LANG] ?: "ar"
        set(value) = runBlocking {
            context.dataStore.edit { it[KEY_AUDIO_LANG] = value }
        }

    val isLoggedIn: Boolean get() = !token.isNullOrEmpty()

    fun logout() = runBlocking {
        context.dataStore.edit {
            it.remove(KEY_TOKEN)
            it.remove(KEY_USER_TYPE)
            it.remove(KEY_USER_ID)
            it.remove(KEY_NICK_NAME)
            it.remove(KEY_HEAD_IMG)
            it.remove(KEY_EMAIL)
            it.remove(KEY_IS_VIP)
        }
    }
}
