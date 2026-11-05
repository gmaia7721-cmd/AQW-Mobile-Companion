package com.aqw.companion

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    companion object {
        private const val RELEASE_API = "https://api.github.com/repos/gmaia7721-cmd/AQW-Mobile-Companion/releases/latest"
        private val fallbackClasses = listOf(
            "Abyssal Angel", "Abyssal Angel's Shadow", "Acolyte", "Alpha DOOMmega", "Alpha Pirate",
            "Arachnomancer", "Arcana Invoker", "ArchFiend", "ArchMage", "ArchPaladin", "Artifact Hunter",
            "Bard", "BattleMage", "Battle Priest", "BattleBlademage", "Blaze Binder", "Blood Ancient",
            "Blood Sorceress", "CardClasher", "Chaos Avenger", "Chaos Slayer", "Chrono Assassin",
            "Chrono Commandant", "Chrono DataKnight", "ChronoCorruptor", "Chronomancer", "Cryomancer",
            "Culinaromancer", "Darkblood StormKing", "Dark Caster", "Dark Lord", "DeathKnight",
            "DoomKnight", "Dragon of Time", "DragonLord", "Dragon Shinobi", "Elemental Dracomancer",
            "Eternal Inversionist", "Exalted Harbinger", "Frostval Barbarian", "Glacial Berserker",
            "Guardian", "Horc Evader", "Infinity Knight", "Legion DoomKnight", "Legion Revenant",
            "LightCaster", "Lord of Order", "Mage", "Master of Moglins", "MindBreaker", "Necromancer",
            "Ninja", "Paladin", "Paladin Chronomancer", "Pirate", "Ranger", "Rogue", "Scarlet Sorceress",
            "Shaman", "ShadowStalker of Time", "ShadowWeaver of Time", "SkyGuard Grenadier",
            "StoneCrusher", "SwordMaster", "TimeKiller", "Troll Spellsmith", "Vampire Lord",
            "Void Highlord", "Warrior", "Yami no Ronin"
        ).distinct().sorted()
        private val ultras = listOf(
            "Ultra Dage", "Ultra Nulgath", "Ultra Speaker", "Ultra Darkon", "Ultra Drago",
            "Ultra Ezrajal", "Ultra Warden", "Ultra Engineer", "Ultra Tyndarius",
            "Champion Drakath", "Ultra Alteon", "Ultra Xiang"
        )
    }

    private lateinit var content: LinearLayout
    private var selectedClass = ""
    private var selectedUltra = ""
    private var currentSection = "Classes"
    private var wikiStatus = "Dados da Wiki ainda não sincronizados."
    private var wikiClasses: List<WikiDataRepository.PageEntry> = emptyList()
    private var wikiEnhancements: List<WikiDataRepository.PageEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("companion_settings", Context.MODE_PRIVATE)
        selectedClass = prefs.getString("selected_class", "").orEmpty()
        selectedUltra = prefs.getString("selected_ultra", "").orEmpty()
        wikiClasses = WikiDataRepository.cachedClasses(this)
        wikiEnhancements = WikiDataRepository.cachedEnhancements(this)
        wikiStatus = if (WikiDataRepository.lastUpdated(this) > 0L) "Cache local da Wiki carregado."
            else "Sem cache local. Toque em Atualizar dados da Wiki."

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 36, 28, 24) }
        root.addView(TextView(this).apply { textSize = 23f; text = "AQW Mobile Companion" })
        root.addView(TextView(this).apply {
            text = "Seu assistente de classes, combos, Forge e Ultras"
            textSize = 14f
            setPadding(0, 6, 0, 20)
        })
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Classes", "Ultras", "Forge", "Biblioteca").forEach { tab ->
            nav.addView(Button(this).apply {
                text = tab
                textSize = 11f
                setOnClickListener { showSection(tab) }
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(nav)
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 18, 0, 8) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        showSection("Classes")
        refreshWikiData(showMessage = false)
        checkForUpdates()
    }

    private fun showSection(section: String) {
        currentSection = section
        content.removeAllViews()
        when (section) {
            "Classes" -> {
                addText("CLASSES", 19f)
                addText(if (selectedClass.isBlank()) "Nenhuma classe selecionada" else "Classe selecionada: $selectedClass", 16f)
                addButton("Selecionar classe") {
                    val options = wikiClasses.map { it.name }.ifEmpty { fallbackClasses }
                    chooseItem("Escolha sua classe", options) {
                        selectedClass = it
                        saveSelection("selected_class", it)
                        showSection("Classes")
                    }
                }
                if (selectedClass.isNotBlank()) addButton("Consultar ficha da classe na AQW Wiki") { loadSelectedClassPage() }
                addText("Fonte: AQWorlds Wiki — índice de classes e páginas individuais.", 13f)
                addText(wikiStatus, 12f)
                addButton("Atualizar dados da Wiki") { refreshWikiData(showMessage = true) }
                addText("A lista é sincronizada com a Wiki e salva no aparelho para consulta offline. A página individual pode conter habilidades, efeitos e informações da classe. Combos otimizados só serão indicados quando houver uma fonte específica validada.", 14f)
                addButton("Abrir índice de classes da Wiki") { openUrl(WikiDataRepository.CLASSES_URL) }
            }
            "Ultras" -> {
                addText("ASSISTENTE DE ULTRAS", 19f)
                addText(if (selectedUltra.isBlank()) "Nenhum Ultra selecionado" else "Chefe selecionado: $selectedUltra", 16f)
                addButton("Selecionar Ultra") {
                    chooseItem("Escolha o Ultra", ultras) {
                        selectedUltra = it
                        saveSelection("selected_ultra", it)
                        showSection("Ultras")
                    }
                }
                addText("As estratégias de Ultra não fazem parte do índice de classes da AQW Wiki. Esta versão disponibiliza guias comunitários especializados para composição, funções, taunts e consumíveis, sem inventar recomendações.", 14f)
                addButton("Abrir guia de Ultras — Jix") { openUrl(WikiDataRepository.ULTRA_GUIDE_URL) }
                addButton("Abrir setups e consumíveis — AQWG") { openUrl(WikiDataRepository.ULTRA_SETUP_URL) }
                addText("Os consumíveis variam conforme classe, função e chefe. Não use a mesma combinação para todos os Ultras.", 14f)
            }
            "Forge" -> {
                addText("FORGE & ENHANCEMENTS", 19f)
                addText("Índice de enhancements sincronizado da AQW Wiki. Abra uma página para consultar os detalhes publicados.", 14f)
                if (wikiEnhancements.isEmpty()) addText("Ainda não há dados locais. Sincronize a Wiki na aba Biblioteca.", 13f)
                wikiEnhancements.filter { it.name.contains("Forge", true) }.forEach { entry ->
                    addButton(entry.name) { openUrl(entry.url) }
                }
                addText("Outras categorias", 16f)
                wikiEnhancements.filterNot { it.name.contains("Forge", true) }.take(35).forEach { entry ->
                    addButton(entry.name) { openUrl(entry.url) }
                }
                addText("A melhor combinação de arma, elmo e capa depende da classe e do contexto (solo, farm, grupo ou Ultra). Os dados dos itens vêm da Wiki; os melhores setups precisam de validação por classe e situação.", 14f)
                addButton("Abrir índice completo de Enhancements") { openUrl(WikiDataRepository.ENHANCEMENTS_URL) }
            }
            else -> {
                addText("BIBLIOTECA", 19f)
                addText("Sincronização de dados públicos da AQW Wiki. Classes e enhancements ficam salvos localmente após uma atualização bem-sucedida.", 14f)
                addText(wikiStatus, 13f)
                addText("Classes encontradas: \${wikiClasses.size}", 14f)
                addText("Páginas de enhancements encontradas: \${wikiEnhancements.size}", 14f)
                addButton("Atualizar dados da Wiki agora") { refreshWikiData(showMessage = true) }
                addButton("Abrir AQW Wiki") { openUrl("https://aqwwiki.wikidot.com/") }
                addButton("Abrir guia de poções e alquimia") { openUrl("https://aqw.ewk.hu/potion/") }
                addText("Última sincronização: \${formatTimestamp(WikiDataRepository.lastUpdated(this))}", 12f)
            }
        }
    }

    private fun refreshWikiData(showMessage: Boolean) {
        wikiStatus = "Consultando AQW Wiki…"
        if (showMessage) showSection(currentSection)
        Thread {
            try {
                val result = WikiDataRepository.refresh(this)
                runOnUiThread {
                    wikiClasses = result.classes
                    wikiEnhancements = result.enhancements
                    wikiStatus = "Wiki atualizada: \${wikiClasses.size} classes e \${wikiEnhancements.size} páginas de enhancements."
                    showSection(currentSection)
                    if (showMessage) AlertDialog.Builder(this)
                        .setTitle("Sincronização concluída")
                        .setMessage(wikiStatus + "\nOs dados também foram salvos no aparelho para consulta offline.")
                        .setPositiveButton("OK", null).show()
                }
            } catch (error: Exception) {
                runOnUiThread {
                    wikiStatus = "Falha ao atualizar. Cache anterior preservado. \${error.localizedMessage ?: ""}"
                    showSection(currentSection)
                    if (showMessage) AlertDialog.Builder(this).setTitle("Não foi possível atualizar a Wiki")
                        .setMessage(wikiStatus).setPositiveButton("OK", null).show()
                }
            }
        }.start()
    }

    private fun loadSelectedClassPage() {
        val entry = WikiDataRepository.findClass(this, selectedClass)
        if (entry == null) {
            AlertDialog.Builder(this).setTitle(selectedClass)
                .setMessage("Não encontrei uma página individual correspondente no cache. Atualize os dados e tente novamente.")
                .setPositiveButton("OK", null).show()
            return
        }
        val loading = AlertDialog.Builder(this).setTitle("Consultando AQW Wiki")
            .setMessage("Carregando a ficha de \${entry.name}…").setNegativeButton("Fechar", null).show()
        Thread {
            try {
                val pageText = WikiDataRepository.fetchPageText(entry.url)
                runOnUiThread {
                    loading.dismiss()
                    AlertDialog.Builder(this).setTitle(entry.name).setMessage(pageText)
                        .setPositiveButton("Fechar", null)
                        .setNeutralButton("Abrir Wiki") { _, _ -> openUrl(entry.url) }.show()
                }
            } catch (error: Exception) {
                runOnUiThread {
                    loading.dismiss()
                    AlertDialog.Builder(this).setTitle("Erro ao consultar a classe")
                        .setMessage(error.localizedMessage ?: "Não foi possível carregar a página.")
                        .setPositiveButton("OK", null)
                        .setNeutralButton("Abrir Wiki") { _, _ -> openUrl(entry.url) }.show()
                }
            }
        }.start()
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun chooseItem(title: String, items: List<String>, onSelected: (String) -> Unit) {
        val search = EditText(this).apply { hint = "Pesquisar..." }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun showMatches(query: String) {
            list.removeAllViews()
            items.filter { it.contains(query, true) }.take(100).forEach { item ->
                list.addView(Button(this).apply { text = item; setOnClickListener { onSelected(item) } })
            }
        }
        showMatches("")
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { showMatches(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 8)
            addView(search)
            addView(ScrollView(this@MainActivity).apply { addView(list) }, LinearLayout.LayoutParams(-1, 650))
        }
        AlertDialog.Builder(this).setTitle(title).setView(wrapper).setNegativeButton("Fechar", null).show()
    }

    private fun saveSelection(key: String, value: String) {
        getSharedPreferences("companion_settings", Context.MODE_PRIVATE).edit().putString(key, value).apply()
    }

    private fun addText(value: String, size: Float) {
        content.addView(TextView(this).apply { text = value; textSize = size; setPadding(0, 8, 0, 12) })
    }

    private fun addButton(value: String, action: () -> Unit) {
        content.addView(Button(this).apply { text = value; setOnClickListener { action() } })
    }

    private fun formatTimestamp(timestamp: Long): String =
        if (timestamp <= 0L) "Nunca" else SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

    private fun checkForUpdates() {
        Thread {
            try {
                val connection = (URL(RELEASE_API).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 7000
                    readTimeout = 7000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "AQW-Mobile-Companion")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                val release = JSONObject(body)
                val remote = release.optString("tag_name").removePrefix("v")
                val installed = packageManager.getPackageInfo(packageName, 0).versionName ?: "0.0"
                if (remote.isNotBlank() && isNewerVersion(remote, installed)) {
                    runOnUiThread {
                        AlertDialog.Builder(this).setTitle("Nova atualização disponível")
                            .setMessage("Versão instalada: $installed\nNova versão: $remote\nDeseja abrir a página para baixar?")
                            .setNegativeButton("Depois", null)
                            .setPositiveButton("Baixar") { _, _ -> openUrl(release.optString("html_url")) }.show()
                    }
                }
            } catch (_: Exception) { }
        }.start()
    }

    private fun isNewerVersion(remote: String, installed: String): Boolean {
        val a = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val b = installed.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
