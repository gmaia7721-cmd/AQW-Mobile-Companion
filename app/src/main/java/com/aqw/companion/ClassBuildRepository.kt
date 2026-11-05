package com.aqw.companion

import android.content.Context
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL

/**
 * Builds an in-app class enhancement guide from the class table maintained by AQWG.
 * The app stores the parsed recommendations locally; users do not need to open the source site.
 */
object ClassBuildRepository {
    private const val SOURCE_URL = "https://sites.google.com/view/aqwg-net/enhancing"
    private const val PREFS = "class_build_cache"
    private const val KEY_DATA = "builds"
    private const val KEY_UPDATED = "updated_at"

    data class ClassBuild(
        val className: String,
        val statSet: String,
        val aweTrait: String
    )

    data class BuildAdvice(
        val title: String,
        val weapon: String,
        val classEnhancement: String,
        val helm: String,
        val cape: String,
        val note: String
    )

    fun cached(context: Context): List<ClassBuild> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DATA, null)
            ?: return emptyList()
        return runCatching {
            org.json.JSONArray(raw).let { array ->
                (0 until array.length()).map { i ->
                    val item = array.getJSONObject(i)
                    ClassBuild(item.getString("name"), item.getString("stats"), item.getString("awe"))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun refresh(context: Context): List<ClassBuild> {
        val connection = (URL(SOURCE_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 18000
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "AQW-Mobile-Companion/1.3")
        }
        val html = try {
            require(connection.responseCode in 200..299) { "Fonte de recomendações respondeu HTTP ${connection.responseCode}" }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }

        val document = Jsoup.parse(html, SOURCE_URL)
        val rows = document.select("p, li, tr, td, div")
            .map { it.ownText().replace(Regex("\\s+"), " ").trim() }
            .filter { it.length in 3..110 }
            .distinct()

        var currentSet = ""
        val parsed = linkedMapOf<String, ClassBuild>()
        val rowPattern = Regex("^(.+?)\\s+(SpC(?:/AB|/HV)?|AB(?:/HV|/MV)?|MV(?:/HV|/SpC)?|HV)\\s*$", RegexOption.IGNORE_CASE)
        val specialPattern = Regex("^(.+?)\\s+(.+)$")

        for (row in rows) {
            val marker = row.trim().trimEnd(':').lowercase()
            if (marker == "lucky" || marker == "wizard" || marker == "fighter" || marker == "healer") {
                currentSet = marker.replaceFirstChar { it.uppercase() }
                continue
            }
            val match = rowPattern.matchEntire(row)
            if (match != null && currentSet.isNotBlank()) {
                val name = match.groupValues[1].trim().trimEnd('-', '–', ':')
                val awe = match.groupValues[2].uppercase()
                if (name.length >= 2 && name.length <= 70) {
                    parsed[normalize(name)] = ClassBuild(name, currentSet, expandAwe(awe))
                }
            } else if (row.startsWith("Dragon of Time", true)) {
                parsed[normalize("Dragon of Time")] = ClassBuild("Dragon of Time", "Wizard", "Farm: Health Vamp • Solo/Boss: Awe Blast")
            }
        }

        require(parsed.size >= 30) { "A fonte retornou apenas ${parsed.size} classes reconhecidas; o cache anterior foi preservado." }
        val array = org.json.JSONArray()
        parsed.values.forEach {
            array.put(org.json.JSONObject().put("name", it.className).put("stats", it.statSet).put("awe", it.aweTrait))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_DATA, array.toString())
            .putLong(KEY_UPDATED, System.currentTimeMillis())
            .apply()
        return parsed.values.sortedBy { it.className.lowercase() }
    }

    fun updatedAt(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_UPDATED, 0L)

    fun find(context: Context, className: String): ClassBuild? {
        val target = normalize(className)
        return cached(context).firstOrNull { normalize(it.className) == target }
            ?: cached(context).firstOrNull { normalize(it.className).contains(target) || target.contains(normalize(it.className)) }
    }

    fun advice(build: ClassBuild, mode: String): BuildAdvice {
        val magic = build.statSet.equals("Wizard", true) || build.statSet.equals("Healer", true)
        val classEnhancement = build.statSet
        return when (mode) {
            "Farm" -> BuildAdvice(
                "Farm",
                if (build.aweTrait.contains("Mana Vamp", true)) "Mana Vamp" else if (build.aweTrait.contains("Health Vamp", true)) "Health Vamp" else "Spiral Carve",
                classEnhancement,
                if (magic) "Pneuma (Forge)" else "Vim (Forge)",
                "Vainglory (Forge)",
                "Foco em dano contínuo e limpeza de grupos. Use Avarice no lugar da capa somente se precisar alcançar o limite de haste."
            )
            "Boss" -> BuildAdvice(
                "Boss",
                "Valiance (Forge) • alternativa: ${build.aweTrait}",
                classEnhancement,
                if (magic) "Pneuma (Forge)" else "Vim (Forge)",
                "Vainglory (Forge) • Penitence se precisar sobreviver",
                "Configuração geral para solo/boss. Ravenous ou Dauntless podem superar Valiance em classes específicas; isso depende da rotação e da sobrevivência."
            )
            else -> BuildAdvice(
                "Ultra",
                "Arcana's Concerto (Forge) se sua função aplicar debuff • Valiance (Forge) como alternativa",
                classEnhancement,
                if (magic) "Pneuma (Forge)" else "Vim (Forge)",
                "Penitence (Forge)",
                "Base defensiva para Ultra. O setup definitivo depende do chefe e da função do grupo; não equipe Vainglory automaticamente em lutas com dano alto."
            )
        }
    }

    private fun expandAwe(code: String): String = code.split("/").joinToString(" / ") {
        when (it.uppercase()) {
            "SPC" -> "Spiral Carve"
            "AB" -> "Awe Blast"
            "MV" -> "Mana Vamp"
            "HV" -> "Health Vamp"
            else -> it
        }
    }

    private fun normalize(value: String): String = value.lowercase().replace(Regex("[^a-z0-9]"), "")
}
