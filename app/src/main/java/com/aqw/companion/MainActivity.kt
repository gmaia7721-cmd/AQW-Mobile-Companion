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

class MainActivity : Activity() {
    companion object {
        private const val RELEASE_API = "https://api.github.com/repos/gmaia7721-cmd/AQW-Mobile-Companion/releases/latest"
        private val classes = listOf(
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
            "Shaman", "ShadowStalker of Time", "ShadowWeaver of Time", "Shaman", "SkyGuard Grenadier",
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
    private lateinit var heading: TextView
    private lateinit var details: TextView
    private var selectedClass = ""
    private var selectedUltra = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("companion_settings", Context.MODE_PRIVATE)
        selectedClass = prefs.getString("selected_class", "").orEmpty()
        selectedUltra = prefs.getString("selected_ultra", "").orEmpty()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 24)
        }
        heading = TextView(this).apply { textSize = 23f; text = "AQW Mobile Companion" }
        root.addView(heading)
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
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 18, 0, 8)
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        showSection("Classes")
        checkForUpdates()
    }

    private fun showSection(section: String) {
        content.removeAllViews()
        when (section) {
            "Classes" -> {
                addText("CLASSES", 19f)
                addText(if (selectedClass.isBlank()) "Nenhuma classe selecionada" else "Classe selecionada: $selectedClass", 16f)
                addButton("Selecionar classe") { chooseItem("Escolha sua classe", classes) { selectedClass = it; saveSelection("selected_class", it); showSection("Classes") } }
                addText("Combos e configurações", 16f)
                addText("A ficha de cada classe será organizada com sequência de skills, enhancement de classe, Forge de arma, elmo e capa, além de variações para solo, farm e grupo. As recomendações específicas serão adicionadas com validação por classe; não vou inventar combos genéricos.", 14f)
            }
            "Ultras" -> {
                addText("ASSISTENTE DE ULTRAS", 19f)
                addText(if (selectedUltra.isBlank()) "Nenhum Ultra selecionado" else "Chefe selecionado: $selectedUltra", 16f)
                addButton("Selecionar Ultra") { chooseItem("Escolha o Ultra", ultras) { selectedUltra = it; saveSelection("selected_ultra", it); showSection("Ultras") } }
                addText("A ficha de cada chefe terá composição recomendada, função de cada classe, sequência de habilidades, taunts e consumíveis por função. As recomendações devem ser específicas para a luta.", 14f)
                addText("Referência inicial de consumíveis: Battle ou Malevolence Elixir, Fate ou Sage Tonic e Felicitous Philtre / Potent Honour, variando conforme classe e chefe. Não use a mesma combinação para todos os Ultras.", 14f)
            }
            "Forge" -> {
                addText("FORGE & ENHANCEMENTS", 19f)
                addText("Arma: Valiance, Dauntless, Elysium, Arcana's Concerto, Ravenous, Praxis, Awe e outras.", 15f)
                addText("Elmo: Anima, Vim, Pneuma, Examen e opções específicas.", 15f)
                addText("Capa: Vainglory, Penitence, Lament, Absolution e outras.", 15f)
                addText("A melhor combinação será indicada por classe e contexto (solo, farm, grupo ou Ultra), sem tratar uma Forge como universal.", 14f)
            }
            else -> {
                addText("BIBLIOTECA", 19f)
                addText("Consulta de enhancements tradicionais, Forge, poções, elixires, tonics, scrolls e habilidades. Cada item terá efeito, uso recomendado e alternativas.", 14f)
            }
        }
    }

    private fun chooseItem(title: String, items: List<String>, onSelected: (String) -> Unit) {
        val search = EditText(this).apply { hint = "Pesquisar..." }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun showMatches(query: String) {
            list.removeAllViews()
            items.filter { it.contains(query, true) }.take(80).forEach { item ->
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
        content.addView(TextView(this).apply {
            text = value
            textSize = size
            setPadding(0, 8, 0, 12)
        })
    }

    private fun addButton(value: String, action: () -> Unit) {
        content.addView(Button(this).apply { text = value; setOnClickListener { action() } })
    }

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
                        AlertDialog.Builder(this)
                            .setTitle("Nova atualização disponível")
                            .setMessage("Versão instalada: $installed\nNova versão: $remote\nDeseja abrir a página para baixar?")
                            .setNegativeButton("Depois", null)
                            .setPositiveButton("Baixar") { _, _ ->
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.optString("html_url"))))
                            }.show()
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
