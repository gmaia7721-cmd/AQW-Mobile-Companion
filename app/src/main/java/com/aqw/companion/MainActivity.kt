package com.aqw.companion

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
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
    private lateinit var nav: LinearLayout
    private val bg = Color.rgb(11, 16, 29)
    private val panel = Color.rgb(24, 33, 51)
    private val accent = Color.rgb(114, 150, 255)
    private val muted = Color.rgb(166, 181, 207)
    private val white = Color.rgb(239, 244, 255)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int = 16) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
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

        window.statusBarColor = bg
        window.navigationBarColor = bg
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(12))
            setBackgroundColor(bg)
        }
        root.addView(TextView(this).apply {
            text = "AQW  /  COMPANION"
            textSize = 12f
            letterSpacing = 0.18f
            setTextColor(accent)
            typeface = Typeface.DEFAULT_BOLD
        })
        root.addView(TextView(this).apply {
            textSize = 26f
            text = "Seu guia de batalha"
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(6), 0, dp(3))
        })
        root.addView(TextView(this).apply {
            text = "Classes • Forge • Ultras • Consumíveis"
            textSize = 13f
            setTextColor(muted)
            setPadding(0, 0, 0, dp(20))
        })
        nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded(panel, 18)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        root.addView(nav)
        val scroll = ScrollView(this).apply { isFillViewport = false; clipToPadding = false }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(18), 0, dp(20))
        }
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
        nav.removeAllViews()
        listOf("Classes", "Ultras", "Forge", "Poções", "Biblioteca").forEach { tab ->
            nav.addView(TextView(this).apply {
                text = when(tab) { "Biblioteca" -> "Dados"; else -> tab }
                gravity = android.view.Gravity.CENTER
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (tab == section) bg else muted)
                background = rounded(if (tab == section) accent else panel, 13)
                setPadding(dp(3), dp(13), dp(3), dp(13))
                setOnClickListener { showSection(tab) }
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        if (section == "Ultras" || section == "Forge" || section == "Classes" || section == "Poções") {
            addButton("⚔ Ativar bolinha flutuante") { startBubble() }
            addButton("Parar bolinha") { stopService(Intent(this, BubbleService::class.java)) }
        }
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
                val guide = UltraGuide.bosses.firstOrNull { it.name == selectedUltra }
                if (guide != null) {
                    addText("LOCAL: /join ${guide.join}", 16f)
                    addButton("Copiar /join ${guide.join}") { copyJoin("/join ${guide.join}") }
                    addText("EQUIPE • toque na classe para ver a build", 17f)
                    guide.team.forEachIndexed { index, className ->
                        addButton("${index+1}. $className — ${guide.roles[index]}") {
                            selectedClass = className
                            saveSelection("selected_class", className)
                            showSection("Ultras")
                        }
                    }
                    val chosen = selectedClass.takeIf { it in guide.team } ?: guide.team.first()
                    addText("BUILD: $chosen", 18f)
                    UltraGuide.build(chosen)?.let { build ->
                        addText("Weapon: ${build.weapon}\nClass: ${build.classEnh}\nHelm: ${build.helm}\nCape: ${build.cape}", 14f)
                        addText("Tônico: ${build.tonic}\nElixir: ${build.elixir}\nPoção/Scroll: ${build.potion}", 14f)
                    } ?: addText("Build individual ainda não verificada.", 14f)
                    addText("MECÂNICAS ESPECÍFICAS", 18f)
                    guide.mechanics.forEachIndexed { index, mechanic -> addText("${index+1}. $mechanic", 14f) }
                    addText("DICA: ${guide.tip}", 14f)
                    addText("Referência offline; não é meta confirmado em tempo real. Valide mudanças de balanceamento.", 12f)
                } else if (selectedUltra.isNotBlank()) {
                    addText("Guia deste Ultra ainda não validado; não exibiremos informações de outro chefe.", 14f)
                } else addText("Selecione um Ultra para consultar o guia.", 14f)
            }
            "Forge" -> {
                addText("FORGE NATIVO • consulta offline", 19f)
                UltraGuide.forges.forEach { forge ->
                    addText("${forge.name} • ${forge.category}", 17f)
                    addText(forge.description, 14f)
                }
                addText("Efeitos resumidos; requisitos e quests detalhados ainda serão ampliados.", 12f)
            }
            "Poções" -> {
                addText("POÇÕES E CONSUMÍVEIS", 19f)
                UltraGuide.consumables.forEach { potion ->
                    addText(potion.name, 17f)
                    addText(potion.note, 14f)
                    addButton("Copiar /join ${potion.join}") { copyJoin("/join ${potion.join}") }
                }
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

    private fun copyJoin(value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("AQW join", value))
    }

    private fun startBubble() {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this).setTitle("Permissão para bolinha")
                .setMessage("Autorize Exibir sobre outros aplicativos para usar o Companion durante o AQW.")
                .setPositiveButton("Autorizar") { _, _ ->
                    startActivity(Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")))
                }.setNegativeButton("Cancelar", null).show()
            return
        }
        try {
            androidxPlaceholder()
        } catch (error: Exception) {
            AlertDialog.Builder(this).setTitle("Não foi possível abrir a bolinha")
                .setMessage(error.localizedMessage ?: "Verifique as permissões do Android.")
                .setPositiveButton("OK",null).show()
        }
    }

    private fun androidxPlaceholder() {
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(Intent(this, BubbleService::class.java))
        else startService(Intent(this, BubbleService::class.java))
    }

    private fun saveSelection(key: String, value: String) {
        getSharedPreferences("companion_settings", Context.MODE_PRIVATE).edit().putString(key, value).apply()
    }

    private fun addText(value: String, size: Float) {
        val heading = size >= 17f || value.startsWith("━━") || value.startsWith("SETUP:")
        val detail = value.contains("\\n") || value.startsWith("Arma:") || value.startsWith("Tônico:") ||
            value.startsWith("Função:") || value.startsWith("MECÂNICAS:")
        content.addView(TextView(this).apply {
            text = value
            textSize = size
            setTextColor(if (heading) white else muted)
            if (heading) typeface = Typeface.DEFAULT_BOLD
            setLineSpacing(dp(3).toFloat(), 1f)
            setPadding(dp(if (detail) 14 else 6), dp(if (detail) 14 else 8), dp(if (detail) 14 else 6), dp(if (detail) 14 else 10))
            if (detail) background = rounded(panel, 14)
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(if (detail) 9 else 2) })
    }

    private fun addButton(value: String, action: () -> Unit) {
        val selected = value.endsWith(" ✓")
        content.addView(TextView(this).apply {
            text = if (selected) "✓  " + value.removeSuffix(" ✓") else value + "   ›"
            textSize = 14f
            gravity = android.view.Gravity.CENTER_VERTICAL
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (selected) bg else white)
            background = rounded(if (selected) accent else panel, 14)
            setPadding(dp(16), dp(15), dp(14), dp(15))
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
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
