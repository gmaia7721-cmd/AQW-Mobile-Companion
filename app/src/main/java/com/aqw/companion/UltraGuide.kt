package com.aqw.companion

/** Offline encounter reference. Recommendations are provisional, not live-verified meta. */
object UltraGuide {
    data class Boss(val name:String,val join:String,val team:List<String>,val roles:List<String>,val mechanics:List<String>,val tip:String)


    data class ClassTaunt(val whenToTaunt:String,val signal:String,val action:String,val warning:String,val confirmed:Boolean)
    fun classTaunt(boss:String,clazz:String):ClassTaunt {
        val c=clazz.lowercase()
        val loo=c.contains("lord of order")
        val lr=c.contains("legion revenant")
        val ap=c.contains("archpaladin")
        val cav=c.contains("chaos avenger")
        val isTaunter=loo||lr||ap||cav||c.contains("void highlord")
        if(boss=="Champion Drakath") {
            return if(loo) ClassTaunt(
                "Prepare o Scroll of Enrage ANTES de 18M, 16M, 14M, 12M, 8M, 6M e 4M de HP.",
                "Olhe o HP do Drakath. O gatilho é a vida, não o relógio.",
                "Se você for o taunter designado, use Scroll of Enrage imediatamente antes do marco.",
                "Confirme com a equipe quem segura o taunt; o tempo varia com o DPS.",true)
            else ClassTaunt("Normalmente você não é o taunter padrão nesta composição.",
                "Observe quem ficou responsável pelos marcos de HP.",
                "Mantenha sua função de dano, buffs ou cura. Só taunte se a equipe atribuir essa função a você.",
                "Não use Scroll of Enrage por conta própria.",false)
        }
        if(boss=="Ultra Dage") {
            return if(cav) ClassTaunt("Mantenha o taunt do Chaos Avenger ativo durante a luta.",
                "Observe o ciclo de ataques e as mecânicas Zone/Decay.",
                "Use a habilidade 3 no ciclo combinado e renove antes de o efeito acabar.",
                "O instante exato depende da rotação e da velocidade da equipe.",true)
            else ClassTaunt("Taunt não atribuído automaticamente à sua classe nesta composição.",
                "Siga o jogador que está segurando a ameaça.",
                "Execute sua função normal; só use Scroll of Enrage se sua equipe pedir.",
                "Não confunda Decay com taunt.",false)
        }
        if(boss=="Ultra Nulgath") {
            return if(ap||lr) ClassTaunt("Alterne com o OUTRO taunter; nunca os dois ao mesmo tempo.",
                "Use o aviso 'Behold the power of the Abyss!' e a duração de Frailty como referência.",
                "Definam antes quem é o primeiro e o segundo. O segundo responde ao primeiro na rotação.",
                "Sem saber sua posição (primeiro/segundo), não existe segundo exato seguro.",true)
            else ClassTaunt("Você não está marcado como taunter principal nesta composição.",
                "Observe a alternância dos dois taunters.",
                "Concentre-se na função da sua classe, salvo outra estratégia combinada.",
                "Não interrompa a rotação de taunts.",false)
        }
        if(boss=="Ultra Speaker") {
            return if(loo||lr||ap) ClassTaunt("Taunte somente na sua vez da rotação Truth/Listen.",
                "Avisos 'I will make you see the truth' e 'You shall listen'.",
                "Combine sua posição na rotação de 3 ou 4 taunters ANTES de começar.",
                "Sem a estratégia e a posição definidas, não é seguro indicar segundos fixos.",false)
            else ClassTaunt("Depende da estratégia de 3 ou 4 taunters.",
                "Observe Truth, Listen e as zonas.",
                "Confirme se sua classe participa da rotação antes de usar Scroll of Enrage.",
                "Uma indicação genérica pode causar wipe.",false)
        }
        if(boss=="Ultra Darkon") {
            return if(isTaunter) ClassTaunt("Se for um dos dois taunters, alterne a cada Elegy atribuída.",
                "Observe a animação de boca (Elegy) e a fase atual.",
                "Taunte apenas na sua vez, para evitar acumular Elegies na mesma pessoa.",
                "A composição cadastrada pode usar outra estratégia; confirme sua função.",false)
            else ClassTaunt("Não há taunt atribuído a esta classe por padrão.",
                "Observe a mecânica Elegy.",
                "Mantenha sua função de suporte/dano; não entre na rotação sem combinar.",
                "A estratégia depende da composição.",false)
        }
        if(boss=="Ultra Drago" && (lr||ap)) {
            return ClassTaunt("Mantenha o taunt no alvo designado pela equipe.",
                "Verifique se você ficou responsável pelo Bow ou pelo Axe.",
                "Renove o taunt antes de expirar, sempre no seu alvo.",
                "O intervalo exato depende da estratégia; confirme o alvo.",false)
        }
        return ClassTaunt("Não há momento exato de taunt validado para $clazz neste Ultra.",
            "Consulte a mecânica do chefe e a rotação combinada.",
            "Não use Scroll of Enrage automaticamente. Confirme se sua classe é responsável pelo taunt.",
            "Dados insuficientes para recomendar segundos ou habilidade específica.",false)
    }
    data class TauntGuide(val timing:String,val trigger:String,val roles:String,val caution:String,val verified:Boolean)
    /** Encounter cues are safer than a wall-clock timer because DPS changes phase timing. */
    fun tauntGuide(bossName:String):TauntGuide = when(bossName) {
        "Ultra Dage" -> TauntGuide("Loop de Enrage + habilidade 3 do Chaos Avenger; renove antes de expirar.","Use a habilidade 3 no ataque automático após Noxious Decay ou Zone. Na abertura, mire o segundo ataque automático.","Chaos Avenger; combine com a equipe antes de começar.","Noxious Decay inverte curas convencionais. Não use taunt no escuro.",true)
        "Ultra Nulgath" -> TauntGuide("Dois jogadores alternam. Método simples: aproximadamente 6 s de diferença; não tauntar juntos.","Método eficiente: primeiro taunt ~6 s após iniciar; segundo no aviso 'Behold the power of the Abyss!'. Depois acompanhe o fim de Frailty.","Dois taunters designados (por exemplo, ArchPaladin e Legion Revenant).","Acompanhe as auras. Contratos acumulados podem eliminar o taunter.",true)
        "Champion Drakath" -> TauntGuide("Por HP, não por cronômetro: pouco antes de 18M, 16M, 14M, 12M, 8M, 6M e 4M.","Observe a barra de vida; prepare Scroll of Enrage antes de cada marco.","Um taunter combinado, frequentemente Lord of Order.","Não gaste o taunt no cooldown sem considerar o próximo marco. 10M e 2M não entram nessa lista.",true)
        "Ultra Speaker" -> TauntGuide("Use os avisos do chefe; janela de reação de cerca de 2 s após a mensagem.","'I will make you see the truth' = Truth; 'You shall listen' = Listen. Siga a rotação de zonas e taunts definida pelo grupo.","Depende da estratégia (3 ou 4 taunters). Na estratégia de 3: LR, AP e LoO dividem Truth/Listen.","Tauntar imediatamente antes de entrar na Zone pode causar morte. Não use cronômetro fixo.",true)
        "Ultra Darkon" -> TauntGuide("Alterne os taunters nas animações de boca (Elegy), conforme a fase.","Na fase 2, evite que a mesma pessoa receba duas ou três Elegies consecutivas.","Duas pessoas designadas para alternância.","Duas seguidas podem inverter críticos; três seguidas podem matar. Ajuste na transição de fase.",true)
        "Ultra Drago" -> TauntGuide("Taunt contínuo coordenado nos alvos designados; renove antes de acabar.","Um taunter controla o Bow e outro o Axe, de acordo com a composição.","Frequentemente LR no Bow e AP no Axe.","Confirme os alvos com a equipe; esta é uma orientação comunitária, não um temporizador validado.",true)
        "Ultra Warden" -> TauntGuide("Taunt de proteção na fase Enrage, conforme o ataque do chefe.","Observe a transição para Enrage e prepare a cura para golpes que deixam 1 HP.","Defina o responsável pelo taunt antes de iniciar.","Intervalo exato não verificado. Não há contagem regressiva confiável cadastrada.",false)
        "Ultra Tyndarius" -> TauntGuide("Controle a ameaça conforme a composição; sem intervalo fixo verificado.","Priorize os orbes e coordene o tanque durante a troca de alvos.","Tanque designado pela equipe.","Timing exato do taunt ainda não verificado.",false)
        "Ultra Engineer" -> TauntGuide("Sem timing de taunt verificado para esta luta.","Observe os drones e as janelas de contra-ataque.","Defina a função de cada jogador antes da luta.","Não confunda a mecânica de drones com um taunt obrigatório.",false)
        "Ultra Ezrajal" -> TauntGuide("Sem timing de taunt verificado para esta luta.","Observe Counter Attack e bloqueio de habilidades.","A equipe deve coordenar a interrupção dos ataques.","Não há taunt obrigatório confirmado neste guia.",false)
        "Ultra Alteon" -> TauntGuide("Timing de taunt ainda não verificado.","Consulte as mecânicas e a composição antes de iniciar.","Definir em equipe.","Sem dados confiáveis para exibir segundos.",false)
        "Ultra Xiang" -> TauntGuide("Timing de taunt ainda não verificado.","Consulte as mecânicas e a composição antes de iniciar.","Definir em equipe.","Sem dados confiáveis para exibir segundos.",false)
        else -> TauntGuide("Ainda não verificado.","Sem gatilho registrado.","Definir em equipe.","Não utilizar temporização presumida.",false)
    }
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

    data class CraftGuide(val materials:List<String>,val steps:List<String>,val verified:Boolean,val source:String)
    fun craftGuide(name:String):CraftGuide = when(name) {
        "Fate Tonic" -> CraftGuide(
            listOf("Opção de troca: Basic Sage Tonic x3 OU Sage Tonic x2", "Gold x1.500", "Alternativa de loja: Gold Voucher 500k x4 (Rank 8 Alchemy)"),
            listOf("Copie /join alchemyacademy e entre no mapa.",
                "Para a troca, procure Alina e abra Exchanging Mixtures (Gebo).",
                "Selecione Fate Tonic, confira os materiais da opção escolhida e confirme a troca.",
                "Alternativa: abra Gebo Shop, selecione Fate Tonic e confirme a compra se tiver Rank 8 Alchemy.",
                "Se preferir Alchemy, as combinações da wiki são tentativas e NÃO garantem o resultado: Arashtite Ore + outro reagente, Dried Slime + outro, ou Trollola Nectar + outro."),
            true,"https://aqwwiki.wikidot.com/fate-tonic")
        "Sage Tonic" -> CraftGuide(
            listOf("Opção de troca: Soul Potion x2 + Gold x1.500", "Alternativa de loja: Gold Voucher 500k x2 (Rank 8 Alchemy)"),
            listOf("Copie /join alchemyacademy e entre no mapa.",
                "Procure Alina e abra Exchanging Mixtures (Gebo) para a troca por Soul Potions.",
                "Selecione Sage Tonic, confira Soul Potion x2 e 1.500 Gold e confirme.",
                "Alternativa: abra Gebo Shop, selecione Sage Tonic e use 2 Gold Vouchers 500k (Rank 8 Alchemy).",
                "Misturas experimentais da wiki: Arashtite Ore + Doomatter, Dragon Scale (1) ou Necrot. Resultado não garantido."),
            true,"https://aqwwiki.wikidot.com/sage-tonic")
        "Scroll of Enrage" -> CraftGuide(
            listOf("Zealous Ink x1 (Spell Ink: Mystic Parchment x2, ou AC Spell Inks: Arcane Quill x1)", "Rank 5 SpellCrafting"),
            listOf("Copie /join spellcraft e entre no mapa.",
                "Fale com Neveya e abra Spell Ink ou AC Spell Inks.",
                "Obtenha Zealous Ink: troque 2 Mystic Parchments ou 1 Arcane Quill, conforme a loja.",
                "Abra a fabricação SpellCrafting com Zealous Ink.",
                "Selecione a combinação do Scroll of Enrage no minijogo e conclua. A combinação visual exata não está validada aqui.",
                "Confira 40 Scrolls of Enrage no inventário após fabricar."),
            true,"https://aqwwiki.wikidot.com/scroll-of-enrage")
        else -> CraftGuide(emptyList(),listOf("Receita e sequência de cliques ainda não verificadas para este consumível.",
            "Consulte a AQW Wiki antes de gastar materiais. Não serão exibidos ingredientes presumidos."),false,
            "https://aqwwiki.wikidot.com/")
    }
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
