package com.cms.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.cms.app.data.models.UserModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cms_session")

class SessionManager(private val context: Context) {

    private val TOKEN_KEY    = stringPreferencesKey(Constants.PREF_TOKEN)
    private val USERNAME_KEY = stringPreferencesKey(Constants.PREF_USERNAME)
    private val ROLE_KEY     = stringPreferencesKey(Constants.PREF_ROLE)
    private val USER_ID_KEY  = longPreferencesKey(Constants.PREF_USER_ID)

    val userFlow: Flow<UserModel?> = context.dataStore.data.map { prefs ->
        val token = prefs[TOKEN_KEY] ?: return@map null
        UserModel(
            id = prefs[USER_ID_KEY],
            username = prefs[USERNAME_KEY] ?: "",
            role = prefs[ROLE_KEY] ?: "USER"
        )
    }
    val usernameFlow: Flow<String?> = context.dataStore.data.map { it[USERNAME_KEY] }
    val roleFlow: Flow<String?> = context.dataStore.data.map { it[ROLE_KEY] }

    suspend fun saveSession(token: String, user: UserModel) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY]    = token
            prefs[USERNAME_KEY] = user.username
            prefs[ROLE_KEY]     = user.role
            user.id?.let { prefs[USER_ID_KEY] = it }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun getToken(): String? =
        context.dataStore.data.first()[TOKEN_KEY]

    suspend fun isLoggedIn(): Boolean =
        context.dataStore.data.first()[TOKEN_KEY] != null

    suspend fun getStoredUser(): UserModel? {
        val prefs = context.dataStore.data.first()
        val token = prefs[TOKEN_KEY] ?: return null
        return UserModel(
            id       = prefs[USER_ID_KEY],
            username = prefs[USERNAME_KEY] ?: "",
            role     = prefs[ROLE_KEY] ?: "USER"
        )
    }
}
