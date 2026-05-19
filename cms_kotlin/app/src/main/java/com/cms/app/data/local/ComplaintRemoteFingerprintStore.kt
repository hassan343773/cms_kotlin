package com.cms.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

private val Context.complaintRemoteFpDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "complaint_remote_fingerprints"
)

/**
 * Last-known server snapshot per complaint id (this device only).
 * Used to detect changes made on **other devices** after the next API fetch / poll.
 */
class ComplaintRemoteFingerprintStore(private val context: Context) {

    private val gson = Gson()
    private val keyFp = stringPreferencesKey("fingerprints_by_complaint_id_json")

    suspend fun get(complaintId: Long): String? {
        val map = readAll()
        return map[complaintId.toString()]
    }

    suspend fun put(complaintId: Long, fingerprint: String) {
        context.complaintRemoteFpDataStore.edit { prefs ->
            val m = readAll(prefs[keyFp]).toMutableMap()
            m[complaintId.toString()] = fingerprint
            prefs[keyFp] = gson.toJson(m)
        }
    }

    suspend fun clear() {
        context.complaintRemoteFpDataStore.edit { it.remove(keyFp) }
    }

    private suspend fun readAll(): Map<String, String> {
        val raw = context.complaintRemoteFpDataStore.data.first()[keyFp]
        return readAll(raw)
    }

    private fun readAll(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson<Map<String, String>>(raw, type) ?: emptyMap()
    }
}
