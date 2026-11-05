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
    private var classBuildStatus = "Recomendações ainda não sincronizadas."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("companion_settings", Context.MODE_PRIVATE)
        selectedClass = prefs.getString("selected_class", "").orEmpty()
        selectedUltra = prefs.getString("selected_ultra", "").orEmpty()
        wikiClasses = WikiDataRepository.cachedClasses(this)
        wikiEnhancements = WikiDataRepository.cachedEnhancements(this)
        classBuildStatus = if (ClassBuildRepository.updatedAt(this) > 0L) "Recomendações de classes disponíveis offline." else "Toque em Atualizar recomendações para carregar os melhores setups."
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
        refreshClassBuildData(showMessage = false)
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
                if (selectedClass.isNotBlank()) {
                    val build = ClassBuildRepository.find(this, selectedClass)
                    if (build == null) {
                        addText("Ainda não há uma recomendação sincronizada para esta classe.", 14f)
                    } else {
                        addText("ENHANCEMENTS RECOMENDADOS", 18f)
                        addText("Base da classe: ${build.statSet} • Arma/Awe: ${build.aweTrait}", 14f)
                        listOf("Farm", "Boss", "Ultra").forEach { mode ->
                            val advice = ClassBuildRepository.advice(build, mode)
                            addText("━━ ${advice.title.uppercase()} ━━", 16f)
                            addText("Arma: ${advice.weapon}", 14f)
                            addText("Classe: ${advice.classEnhancement}", 14f)
                            addText("Elmo: ${advice.helm}", 14f)
                            addText("Capa: ${advice.cape}", 14f)
                            addText(advice.note, 12f)
                        }
                    }
                }
                addText(classBuildStatus, 12f)
                addButton("Atualizar recomendações de classes") { refreshClassBuildData(showMessage = true) }
                addText("As recomendações aparecem aqui dentro do aplicativo e ficam salvas para consulta offline. A configuração de Ultra é uma base geral; confirme a função exigida pelo chefe e pela composição do grupo.", 13f)
                addText("Dados gerais de classe e enhancements: AQW Wiki. Builds por classe: guia comunitário AQWG, integrado ao app.", 12f)
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
                if (selectedUltra == "Ultra Dage") {
                    addText("ULTRA DAGE • /join ultradage • Equipe de 4", 17f)
                    addText("Composição sugerida: Chaos Avenger + Verus DoomKnight + Lord of Order + ArchPaladin. Alternativa: Legion DoomKnight no lugar de VDK.", 14f)
                    addText("Selecione abaixo a classe para ver sua função, Forge e consumíveis. Todas as recomendações ficam nesta tela, inclusive offline.", 13f)
                    val dageClasses = listOf("Chaos Avenger", "Verus DoomKnight", "Lord of Order", "ArchPaladin", "Legion DoomKnight")
                    dageClasses.forEach { className ->
                        addButton(className + if (selectedClass == className) " ✓" else "") {
                            selectedClass = className
                            saveSelection("selected_class", className)
                            showSection("Ultras")
                        }
                    }
                    val chosen = selectedClass.takeIf { it in dageClasses } ?: "Chaos Avenger"
                    addText("SETUP: $chosen", 18f)
                    when (chosen) {
                        "Chaos Avenger" -> {
                            addText("Função: tank / taunt principal. Combine skill 3 com Scroll of Enrage para manter o aggro.", 14f)
                            addText("Arma: Dauntless (alternativa: Health Vamp)\nClasse: Lucky\nElmo: Anima\nCapa: Vainglory (Penitence se precisar resistir)", 14f)
                            addText("Tônico: Fate Tonic\nElixir: Potent Destruction Elixir\nPoção/scroll: Scroll of Enrage", 14f)
                            addText("Cuidado: evite a skill 4 quando causar cura periódica sob Noxious Decay.", 13f)
                        }
                        "Verus DoomKnight" -> {
                            addText("Função: dano, debuffs e aplicar Decay para impedir a cura do Dage.", 14f)
                            addText("Arma: Dauntless ou Valiance\nClasse: Lucky\nElmo: Anima\nCapa: Vainglory", 14f)
                            addText("Tônico: Fate Tonic\nElixir: Potent Destruction Elixir\nPoção: Potent Honor Potion", 14f)
                            addText("Use a skill 5 para aplicar Decay na fase em que Dage prepara a regeneração.", 13f)
                        }
                        "Lord of Order" -> {
                            addText("Função: buffs para o grupo. Não use a habilidade de cura durante Noxious Decay.", 14f)
                            addText("Arma: Health Vamp\nClasse: Lucky\nElmo: Forge padrão\nCapa: Vainglory (ou Penitence para defesa)", 14f)
                            addText("Tônico: Body Tonic\nElixir: opcional, conforme sobrevivência\nPoção: Potent Honor Potion", 14f)
                            addText("Mantenha buffs sem ativar cura direta que possa prejudicar o grupo.", 13f)
                        }
                        "ArchPaladin" -> {
                            addText("Função: defesa, debuffs e Broken Seal. Evite a skill de cura durante Decay.", 14f)
                            addText("Arma: Health Vamp\nClasse: Lucky\nElmo: Forge padrão\nCapa: Vainglory (ou Penitence)", 14f)
                            addText("Tônico: Fate Tonic\nElixir: Potent Destruction Elixir\nPoção: Potent Honor Potion", 14f)
                            addText("Concentre-se em manter os debuffs e stacks de Broken Seal.", 13f)
                        }
                        else -> {
                            addText("Função: aplicar Decay e contribuir com dano.", 14f)
                            addText("Arma: Dauntless ou Health Vamp\nClasse: Lucky\nElmo: Anima\nCapa: Vainglory", 14f)
                            addText("Tônico: Fate Tonic\nElixir: Potent Destruction Elixir\nPoção: Potent Honor Potion", 14f)
                            addText("Mantenha o Decay com a skill 5 na fase de regeneração.", 13f)
                        }
                    }
                    addText("MECÂNICAS: não use curas normais sob Noxious Decay; Health Vamp/lifesteal funciona. Mova-se para a plataforma iluminada. Cada morte cura 500 mil HP do chefe.", 14f)
                    addText("Setups recomendados pela comunidade, sujeitos a ajustes conforme a equipe, o nível e as atualizações do AQW.", 12f)
                } else if (selectedUltra.isNotBlank()) {
                    addText("Ainda não há um setup específico validado para $selectedUltra. Não vou apresentar a build de outro chefe como se fosse adequada.", 14f)
                } else {
                    addText("Selecione um Ultra para consultar a composição e os equipamentos recomendados.", 14f)
                }
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
                addText("Classes encontradas: " + wikiClasses.size, 14f)
                addText("Páginas de enhancements encontradas: " + wikiEnhancements.size, 14f)
                addButton("Atualizar dados da Wiki agora") { refreshWikiData(showMessage = true) }
                addButton("Abrir AQW Wiki") { openUrl("https://aqwwiki.wikidot.com/") }
                addButton("Abrir guia de poções e alquimia") { openUrl("https://aqw.ewk.hu/potion/") }
                addText("Última sincronização: " + formatTimestamp(WikiDataRepository.lastUpdated(this)), 12f)
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
                    wikiStatus = "Wiki atualizada: " + wikiClasses.size + " classes e " + wikiEnhancements.size + " páginas de enhancements."
                    showSection(currentSection)
                    if (showMessage) AlertDialog.Builder(this)
                        .setTitle("Sincronização concluída")
                        .setMessage(wikiStatus + "\nOs dados também foram salvos no aparelho para consulta offline.")
                        .setPositiveButton("OK", null).show()
                }
            } catch (error: Exception) {
                runOnUiThread {
                    wikiStatus = "Falha ao atualizar. Cache anterior preservado. " + (error.localizedMessage ?: "")
                    showSection(currentSection)
                    if (showMessage) AlertDialog.Builder(this).setTitle("Não foi possível atualizar a Wiki")
                        .setMessage(wikiStatus).setPositiveButton("OK", null).show()
                }
            }
        }.start()
    }


    private fun refreshClassBuildData(showMessage: Boolean) {
        classBuildStatus = "Atualizando recomendações de classes…"
        if (showMessage) showSection(currentSection)
        Thread {
            try {
                val builds = ClassBuildRepository.refresh(this)
                runOnUiThread {
                    classBuildStatus = "Recomendações atualizadas: ${builds.size} classes."
                    showSection(currentSection)
                    if (showMessage) AlertDialog.Builder(this)
                        .setTitle("Recomendações atualizadas")
                        .setMessage(classBuildStatus + "\\nOs setups estão disponíveis dentro da aba Classes e também offline.")
                        .setPositiveButton("OK", null).show()
                }
            } catch (error: Exception) {
                runOnUiThread {
                    classBuildStatus = "Falha ao atualizar recomendações. Cache anterior preservado. " + (error.localizedMessage ?: "")
                    showSection(currentSection)
                    if (showMessage) AlertDialog.Builder(this).setTitle("Não foi possível atualizar")
                        .setMessage(classBuildStatus).setPositiveButton("OK", null).show()
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
            .setMessage("Carregando a ficha de " + entry.name + "…").setNegativeButton("Fechar", null).show()
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
