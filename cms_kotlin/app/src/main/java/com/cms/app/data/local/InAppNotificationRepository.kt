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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.inAppNotificationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "in_app_notifications"
)

data class InAppNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val complaintId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

class InAppNotificationRepository(private val context: Context) {

    private val gson = Gson()
    private val keyInbox = stringPreferencesKey("inbox_by_username_json")
    private val keySeen = stringPreferencesKey("seen_complaint_ids_by_username_json")

    val inboxFlow: Flow<Map<String, List<InAppNotification>>> =
        context.inAppNotificationDataStore.data.map { prefs ->
            readInbox(prefs[keyInbox])
        }

    suspend fun addForUsername(username: String, notification: InAppNotification) {
        val key = username.trim().lowercase()
        if (key.isEmpty()) return
        context.inAppNotificationDataStore.edit { prefs ->
            val inbox = readInbox(prefs[keyInbox]).toMutableMap()
            val list = inbox[key].orEmpty() + notification
            inbox[key] = list.takeLast(200)
            prefs[keyInbox] = gson.toJson(inbox)
        }
    }

    suspend fun markRead(username: String, notificationId: String) {
        val key = username.trim().lowercase()
        if (key.isEmpty()) return
        context.inAppNotificationDataStore.edit { prefs ->
            val inbox = readInbox(prefs[keyInbox]).toMutableMap()
            val list = inbox[key].orEmpty().map { n ->
                if (n.id == notificationId) n.copy(read = true) else n
            }
            inbox[key] = list
            prefs[keyInbox] = gson.toJson(inbox)
        }
    }

    suspend fun markAllRead(username: String) {
        val key = username.trim().lowercase()
        if (key.isEmpty()) return
        context.inAppNotificationDataStore.edit { prefs ->
            val inbox = readInbox(prefs[keyInbox]).toMutableMap()
            inbox[key] = inbox[key].orEmpty().map { it.copy(read = true) }
            prefs[keyInbox] = gson.toJson(inbox)
        }
    }

    suspend fun getSeenComplaintIds(username: String): Set<Long> {
        val key = username.trim().lowercase()
        if (key.isEmpty()) return emptySet()
        val raw = context.inAppNotificationDataStore.data.first()[keySeen]
        val map = readSeenMap(raw)
        return map[key].orEmpty()
    }

    suspend fun replaceSeenComplaintIds(username: String, ids: Set<Long>) {
        val key = username.trim().lowercase()
        if (key.isEmpty()) return
        context.inAppNotificationDataStore.edit { prefs ->
            val map = readSeenMap(prefs[keySeen]).toMutableMap()
            map[key] = ids
            prefs[keySeen] = gson.toJson(map.mapValues { (_, v) -> v.toList() })
        }
    }

    suspend fun appendSeenComplaintIds(username: String, ids: Collection<Long>) {
        if (ids.isEmpty()) return
        val key = username.trim().lowercase()
        if (key.isEmpty()) return
        context.inAppNotificationDataStore.edit { prefs ->
            val map = readSeenMap(prefs[keySeen]).toMutableMap()
            val merged = map[key].orEmpty() + ids.toSet()
            map[key] = merged
            prefs[keySeen] = gson.toJson(map.mapValues { (_, v) -> v.toList() })
        }
    }

    private fun readInbox(raw: String?): Map<String, List<InAppNotification>> {
        if (raw.isNullOrBlank()) return emptyMap()
        val type = object : TypeToken<Map<String, List<InAppNotification>>>() {}.type
        return gson.fromJson<Map<String, List<InAppNotification>>>(raw, type)
            ?.mapKeys { it.key.lowercase() }
            ?: emptyMap()
    }

    private fun readSeenMap(raw: String?): Map<String, Set<Long>> {
        if (raw.isNullOrBlank()) return emptyMap()
        val type = object : TypeToken<Map<String, List<Long>>>() {}.type
        val parsed: Map<String, List<Long>>? = gson.fromJson(raw, type)
        return parsed?.mapValues { it.value.toSet() }?.mapKeys { it.key.lowercase() } ?: emptyMap()
    }
}
