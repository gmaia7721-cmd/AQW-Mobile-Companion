package com.aqw.companion

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Downloads the public AQW Wiki monster index and caches it locally.
 * The existing built-in catalogue remains available if the Wiki is offline.
 */
object MonsterCatalogUpdater {
    private const val PREFS = "monster_catalog_cache"
    private const val KEY_NAMES = "wiki_monster_names"
    private const val KEY_UPDATED = "last_updated"
    private const val API = "https://adventurequestworlds.fandom.com/api.php"
    private val executor = Executors.newSingleThreadExecutor()
    private val updating = AtomicBoolean(false)

    fun loadCached(context: Context) {
        val cached = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_NAMES, mutableSetOf())
            ?.toList()
            .orEmpty()
        if (cached.isNotEmpty()) MonsterCatalog.replaceRemoteNames(cached)
    }

    fun refresh(context: Context, callback: ((Int, Boolean) -> Unit)? = null) {
        if (!updating.compareAndSet(false, true)) return
        val app = context.applicationContext
        executor.execute {
            var updated = false
            try {
                val downloaded = linkedSetOf<String>()
                downloaded.addAll(fetchCategory("Monsters"))
                downloaded.addAll(fetchCategory("Boss_Monsters"))
                if (downloaded.size >= 100) {
                    val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    prefs.edit()
                        .putStringSet(KEY_NAMES, downloaded)
                        .putLong(KEY_UPDATED, System.currentTimeMillis())
                        .apply()
                    MonsterCatalog.replaceRemoteNames(downloaded)
                    updated = true
                }
            } catch (_: Exception) {
                // Keep the cached and built-in catalogues when the Wiki is unreachable.
            } finally {
                updating.set(false)
                callback?.invoke(MonsterCatalog.currentCount(), updated)
            }
        }
    }

    private fun fetchCategory(category: String): Set<String> {
        val names = linkedSetOf<String>()
        var continuation: String? = null
        var pages = 0

        do {
            val query = buildString {
                append("?action=query&list=categorymembers")
                append("&cmtitle=").append(URLEncoder.encode("Category:$category", "UTF-8"))
                append("&cmtype=page&cmnamespace=0&cmlimit=500&format=json&formatversion=2")
                if (!continuation.isNullOrBlank()) {
                    append("&cmcontinue=").append(URLEncoder.encode(continuation, "UTF-8"))
                }
            }
            val connection = (URL(API + query).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 18000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "AQW-Mobile-Companion/0.7 (Android)")
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IllegalStateException("Wiki HTTP ${connection.responseCode}")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(body)
                val members = root.optJSONObject("query")?.optJSONArray("categorymembers")
                    ?: throw IllegalStateException("Resposta do Wiki sem lista de monstros")
                for (i in 0 until members.length()) {
                    val title = members.optJSONObject(i)?.optString("title").orEmpty().trim()
                    if (title.isNotBlank()) names.add(title)
                }
                continuation = root.optJSONObject("continue")?.optString("cmcontinue")
                    ?.takeIf { it.isNotBlank() && it != "null" }
            } finally {
                connection.disconnect()
            }
            pages++
        } while (!continuation.isNullOrBlank() && pages < 10)

        return names
    }
}
