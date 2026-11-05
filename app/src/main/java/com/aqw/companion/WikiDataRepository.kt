package com.aqw.companion

import android.content.Context
import org.json.JSONArray
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL

/** Downloads the public AQWorlds Wiki indexes and caches them locally. */
object WikiDataRepository {
    private const val PREFS = "aqw_wiki_cache"
    private const val KEY_CLASSES = "classes"
    private const val KEY_ENHANCEMENTS = "enhancements"
    private const val KEY_UPDATED = "updated_at"

    const val CLASSES_URL = "https://aqwwiki.wikidot.com/classes"
    const val ENHANCEMENTS_URL = "https://aqwwiki.wikidot.com/enhancements"
    const val ULTRA_GUIDE_URL = "https://jix-aqw.github.io/guias/ultra-bosses/index.html"
    const val ULTRA_SETUP_URL = "https://sites.google.com/view/aqwg-net/ultra-bosses"

    data class PageEntry(val name: String, val url: String)
    data class SyncResult(val classes: List<PageEntry>, val enhancements: List<PageEntry>, val updatedAt: Long)

    fun cachedClasses(context: Context): List<PageEntry> = readEntries(context, KEY_CLASSES)
    fun cachedEnhancements(context: Context): List<PageEntry> = readEntries(context, KEY_ENHANCEMENTS)
    fun lastUpdated(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_UPDATED, 0L)

    @Synchronized
    fun refresh(context: Context): SyncResult {
        val classes = scrapeIndex(CLASSES_URL, "classes")
        val enhancements = scrapeIndex(ENHANCEMENTS_URL, "enhancements")
        require(classes.size >= 20) { "A Wiki retornou poucas classes (\${classes.size})." }
        require(enhancements.size >= 10) { "A Wiki retornou poucos enhancements (\${enhancements.size})." }

        val now = System.currentTimeMillis()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_CLASSES, encode(classes))
            .putString(KEY_ENHANCEMENTS, encode(enhancements))
            .putLong(KEY_UPDATED, now)
            .apply()
        return SyncResult(classes, enhancements, now)
    }

    fun findClass(context: Context, name: String): PageEntry? {
        val target = normalize(name)
        val entries = cachedClasses(context)
        return entries.firstOrNull { normalize(it.name) == target }
            ?: entries.firstOrNull { normalize(it.name).contains(target) || target.contains(normalize(it.name)) }
    }

    fun fetchPageText(pageUrl: String): String {
        val document = Jsoup.parse(download(pageUrl), pageUrl)
        val main = document.selectFirst("#page-content") ?: document.body()
        return main.text().replace(Regex("\\s+"), " ").trim().take(9000)
    }

    private fun scrapeIndex(startUrl: String, kind: String): List<PageEntry> {
        val result = linkedMapOf<String, PageEntry>()
        var current = startUrl
        val visited = hashSetOf<String>()
        repeat(5) {
            if (!visited.add(current)) return@repeat
            val document = Jsoup.parse(download(current), current)
            val main = document.selectFirst("#page-content") ?: document.body()
            for (anchor in main.select("a[href]")) {
                val label = anchor.text().trim()
                val absolute = anchor.absUrl("href")
                if (label.length !in 2..90 || absolute.isBlank()) continue
                val parsed = runCatching { URL(absolute) }.getOrNull() ?: continue
                if (parsed.host != "aqwwiki.wikidot.com") continue
                val path = parsed.path.trimEnd('/')
                if (path.isBlank() || path == "/$kind" || path.startsWith("/$kind/p/")) continue
                if (label.equals("Back to Top", true) || label.equals("Next", true) ||
                    label.equals("Previous", true) || label.contains("Image:")) continue

                if (kind == "enhancements") {
                    val relevant = label.contains("enhancement", true) || label.contains("awe", true) ||
                        label.contains("forge", true) || label.contains("wizard", true) ||
                        label.contains("lucky", true) || label.contains("fighter", true) ||
                        label.contains("healer", true) || label.contains("thief", true) ||
                        label.contains("hybrid", true)
                    if (!relevant) continue
                }

                val clean = label
                    .replace(Regex("\\s*\\((Class|0 AC|AC|Merge)\\)", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\s+"), " ").trim()
                if (clean.length < 2) continue
                val key = normalize(clean)
                if (key.isNotBlank()) result.putIfAbsent(key, PageEntry(clean, absolute))
            }

            val next = main.select("a[href]").firstOrNull {
                val href = it.absUrl("href")
                val label = it.text().trim()
                (label.contains("next", true) || href.contains("/$kind/p/")) &&
                    href.isNotBlank() && href !in visited && href != current
            }?.absUrl("href")
            if (!next.isNullOrBlank()) current = next
        }
        return result.values.sortedBy { it.name.lowercase() }
    }

    private fun download(address: String): String {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 15000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "AQW-Mobile-Companion/1.2 (Android; Wiki index reader)")
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
            instanceFollowRedirects = true
        }
        try {
            val code = connection.responseCode
            require(code in 200..299) { "Wiki respondeu HTTP $code" }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(entries: List<PageEntry>): String {
        val array = JSONArray()
        entries.forEach { array.put(org.json.JSONObject().put("name", it.name).put("url", it.url)) }
        return array.toString()
    }

    private fun readEntries(context: Context, key: String): List<PageEntry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.getJSONObject(index)
                val name = item.optString("name").trim()
                val url = item.optString("url").trim()
                if (name.isNotBlank() && url.startsWith("https://aqwwiki.wikidot.com/")) PageEntry(name, url) else null
            }
        }.getOrDefault(emptyList())
    }

    private fun normalize(value: String): String = value.lowercase().replace(Regex("[^a-z0-9]"), "")
}
