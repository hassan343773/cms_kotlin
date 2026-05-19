package com.cms.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.complaintExtrasDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "complaint_extras"
)

class ComplaintExtrasRepository(private val context: Context) {

    private val gson = Gson()
    private val keyExtras = stringPreferencesKey("complaint_extras_json")

    val extrasFlow: Flow<Map<Long, ComplaintExtras>> =
        context.complaintExtrasDataStore.data.map { prefs ->
            val raw = prefs[keyExtras] ?: return@map emptyMap()
            val type = object : TypeToken<Map<String, ComplaintExtras>>() {}.type
            val parsed: Map<String, ComplaintExtras>? = gson.fromJson(raw, type)
            parsed?.mapKeys { it.key.toLongOrNull() ?: 0L }?.filterKeys { it != 0L }
                ?: emptyMap()
        }

    suspend fun put(complaintId: Long, extras: ComplaintExtras) {
        context.complaintExtrasDataStore.edit { prefs ->
            val current = readMap(prefs[keyExtras])
            val updated = current.toMutableMap().apply { put(complaintId, extras) }
            prefs[keyExtras] = gson.toJson(updated.mapKeys { it.key.toString() })
        }
    }

    suspend fun update(complaintId: Long, transform: (ComplaintExtras) -> ComplaintExtras) {
        context.complaintExtrasDataStore.edit { prefs ->
            val current = readMap(prefs[keyExtras])
            val prev = current[complaintId] ?: ComplaintExtras()
            val next = transform(prev)
            val updated = current.toMutableMap().apply { put(complaintId, next) }
            prefs[keyExtras] = gson.toJson(updated.mapKeys { it.key.toString() })
        }
    }

    private fun readMap(raw: String?): Map<Long, ComplaintExtras> {
        if (raw.isNullOrBlank()) return emptyMap()
        val type = object : TypeToken<Map<String, ComplaintExtras>>() {}.type
        val parsed: Map<String, ComplaintExtras>? = gson.fromJson(raw, type)
        return parsed?.mapNotNull { (k, v) ->
            k.toLongOrNull()?.let { it to v }
        }?.toMap() ?: emptyMap()
    }
}
