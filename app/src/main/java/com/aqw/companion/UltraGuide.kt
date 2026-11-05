package com.aqw.companion

/** Offline encounter reference. Recommendations are provisional, not live-verified meta. */
object UltraGuide {
    data class Boss(val name:String,val join:String,val team:List<String>,val roles:List<String>,val mechanics:List<String>,val tip:String)
    val bosses = listOf(
        Boss("Ultra Dage","ultradage",listOf("Chaos Avenger","Legion DoomKnight","Lord of Order","ArchPaladin"),listOf("Tank / taunt","Decay / DPS","Buffs","Debuffs / proteção"),listOf("Noxious Decay pune curas convencionais; prefira Health Vamp/lifesteal.","Entre na plataforma iluminada durante Zone Strike.","Mantenha Decay para impedir a regeneração do chefe."),"Combine taunt e Decay antes de iniciar."),
        Boss("Ultra Nulgath","ultranulgath",listOf("Void Highlord","ArchPaladin","Lord of Order","Legion Revenant"),listOf("DPS / taunt","Proteção / taunt","Buffs","DPS / suporte"),listOf("Alterne taunts para distribuir os debuffs de Nulgath.","Controle a Overfiend Blade antes do golpe fatal.","A espada fortalece o chefe ao morrer; evite mortes desnecessárias."),"Defina os dois taunts antes da luta."),
        Boss("Ultra Speaker","ultraspeaker",listOf("Legion Revenant","ArchPaladin","Lord of Order","Quantum Chronomancer"),listOf("Decay / taunt","Proteção / taunt","Buffs / taunt","DPS"),listOf("Truth e Listen exigem taunts sincronizados.","Posicione o jogador designado na zona vermelha e os outros fora.","Use Decay e respeite a sequência combinada de mecânicas."),"Não improvise a rotação de taunts."),
        Boss("Champion Drakath","championdrakath",listOf("Void Highlord","StoneCrusher","Lord of Order","ArchPaladin"),listOf("DPS","Buffs","Buffs / taunt","Proteção / cura"),listOf("Antecipe o taunt nos marcos de HP do chefe.","Os ataques aumentam de intensidade nas fases finais.","Use bônus de dano contra Chaos quando possível."),"Coordene Scroll of Enrage."),
        Boss("Ultra Tyndarius","ultratyndarius",listOf("Chaos Avenger","Legion Revenant","Lord of Order","ArchPaladin"),listOf("Tank","DPS","Buffs","Proteção"),listOf("Elimine os orbes antes de focar o chefe.","Evite manter os dois orbes ativos simultaneamente.","O tank deve controlar aggro enquanto o grupo troca de alvo."),"Orbes primeiro, chefe depois."),
        Boss("Ultra Engineer","ultraengineer",listOf("Legion Revenant","Lord of Order","ArchPaladin","StoneCrusher"),listOf("DPS","Buffs","Proteção","Haste / suporte"),listOf("Priorize Defense Drone e depois Attack Drone.","O Defense Drone pode punir ataques durante seu contra-ataque.","Ataque o Engineer quando os drones estiverem controlados."),"Observe a janela de contra-ataque."),
        Boss("Ultra Warden","ultrawarden",listOf("Legion Revenant","Lord of Order","ArchPaladin","StoneCrusher"),listOf("DPS / taunt","Buffs","Cura","HoT / suporte"),listOf("O chefe pode reduzir a vida dos jogadores a 1 HP.","Stuns tornam importante manter buffs e recuperação.","Na fase de Enrage, use taunt para proteger o grupo."),"Prepare cura para os golpes que deixam 1 HP."),
        Boss("Ultra Ezrajal","ultraezrajal",listOf("Legion Revenant","Lord of Order","ArchPaladin","StoneCrusher"),listOf("DPS","Buffs","Proteção","Haste"),listOf("Ezrajal bloqueia periodicamente uma habilidade.","Durante Counter Attack o dano pode ser refletido.","Interrompa os ataques na janela de reflexão."),"Não ataque durante Counter Attack."),
        Boss("Ultra Darkon","ultradarkon",listOf("Legion Revenant","Lord of Order","StoneCrusher","LightCaster"),listOf("DPS / suporte","Buffs / cura","Suporte / cura","DPS / cura"),listOf("Primeira fase: redução de acerto e possíveis stuns.","Segunda fase: redução de haste e riscos ao repetir taunts em Elegy.","Terceira fase: dano maior e ataques especiais com DoT."),"Coordene cura e taunts por fase.")
    )
    data class Build(val weapon:String,val classEnh:String,val helm:String,val cape:String,val tonic:String,val elixir:String,val potion:String)
    private val builds=mapOf(
        "Chaos Avenger" to Build("Dauntless / Health Vamp","Lucky","Anima","Penitence / Vainglory","Fate Tonic","Potent Destruction Elixir","Scroll of Enrage"),
        "Legion DoomKnight" to Build("Dauntless / Health Vamp","Lucky","Anima","Vainglory / Penitence","Fate Tonic","Potent Destruction Elixir","Potent Honor Potion"),
        "Void Highlord" to Build("Ravenous / Dauntless","Lucky","Anima","Vainglory / Penitence","Fate Tonic","Battle Elixir","Potent Honor Potion"),
        "Legion Revenant" to Build("Arcana's Concerto / Valiance","Wizard","Pneuma","Vainglory / Penitence","Sage Tonic","Malevolence Elixir","Potent Honor Potion"),
        "ArchPaladin" to Build("Health Vamp / Valiance","Lucky","Forge padrão","Penitence","Fate Tonic","Battle Elixir","Potent Honor Potion"),
        "Lord of Order" to Build("Health Vamp / Awe Blast","Lucky","Forge padrão","Penitence","Body Tonic","Revitalise Elixir","Scroll of Enrage"),
        "StoneCrusher" to Build("Valiance / Awe Blast","Wizard","Forge padrão","Absolution / Penitence","Fate Tonic","Malevolence Elixir","Potent Honor Potion"),
        "LightCaster" to Build("Valiance / Praxis","Wizard","Pneuma","Lament / Penitence","Sage Tonic","Malevolence Elixir","Potent Honor Potion"),
        "Quantum Chronomancer" to Build("Valiance / Ravenous","Lucky","Anima","Vainglory","Fate Tonic","Battle Elixir","Felicitous Philtre")
    )
    fun build(name:String)=builds[name]
    data class Forge(val name:String,val category:String,val description:String)
    val forges=listOf(
        Forge("Dauntless","Weapon","Dano agressivo; pode aplicar Decay ao usuário. Avalie sobrevivência."),
        Forge("Valiance","Weapon","Opção versátil de atributos e dano."),
        Forge("Ravenous","Weapon","Boa opção de dano contra chefes em classes compatíveis."),
        Forge("Arcana's Concerto","Weapon","Suporte com debuffs para dano da equipe."),
        Forge("Elysium","Weapon","Opção de dano e sustain para classes mágicas compatíveis."),
        Forge("Health Vamp","Awe","Recuperação por ataques, útil em certas mecânicas."),
        Forge("Awe Blast","Awe","Debuff de defesa do alvo para ajudar a equipe."),
        Forge("Anima","Helm","Alternativa ofensiva física; confira a sobrevivência."),
        Forge("Pneuma","Helm","Alternativa ofensiva mágica."),
        Forge("Vim","Helm","Opção para classes físicas e de haste."),
        Forge("Vainglory","Cape","Alternativa ofensiva; pode comprometer sobrevivência."),
        Forge("Penitence","Cape","Alternativa defensiva."),
        Forge("Lament","Cape","Alternativa ofensiva para builds compatíveis."),
        Forge("Absolution","Cape","Alternativa voltada à cura.")
    )
    data class Consumable(val name:String,val join:String,val note:String)
    val consumables=listOf(
        Consumable("Fate Tonic","alchemyacademy","Tônico ofensivo; confira requisitos da Alchemy."),
        Consumable("Body Tonic","alchemyacademy","Tônico de resistência; confira requisitos da Alchemy."),
        Consumable("Sage Tonic","alchemyacademy","Tônico para builds mágicas."),
        Consumable("Potent Destruction Elixir","alchemyacademy","Elixir ofensivo, obtido por Alchemy ou loja aplicável."),
        Consumable("Battle Elixir","alchemyacademy","Elixir ofensivo físico."),
        Consumable("Malevolence Elixir","alchemyacademy","Elixir ofensivo mágico."),
        Consumable("Revitalise Elixir","alchemyacademy","Elixir de sustain."),
        Consumable("Potent Honor Potion","alchemyacademy","Poção de dano para grupo; confira requisitos."),
        Consumable("Felicitous Philtre","alchemyacademy","Consumível ofensivo."),
        Consumable("Scroll of Enrage","dragonrune","SpellCrafting; taunt, não é uma poção comum.")
    )
}
