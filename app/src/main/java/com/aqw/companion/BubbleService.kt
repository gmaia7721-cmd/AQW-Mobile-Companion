package com.aqw.companion

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*

/** Lightweight game overlay; does not capture the screen or interact with the game. */
class BubbleService: Service() {
    companion object { const val ACTION_STOP = "com.aqw.companion.STOP_BUBBLE" }
    private lateinit var wm: WindowManager
    private var root: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var expanded=false
    private var tab="Ultras"
    private var selected="Ultra Dage"
    private var chosen="Chaos Avenger"
    private var detail="Equipe"
    private var selectedForge="Dauntless"
    private var selectedPotion="Fate Tonic"
    private var team=mutableListOf<String>()
    private var willTaunt=true
    private var tauntOrder=1
    private var craftStep=0
    private var classMode="Farm"
    private var classSearch=""
    private var showClassPicker=false
    private var xDown=0f
    private var yDown=0f
    private var initialX=0
    private var initialY=0
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private val gold = 0xFFDEAF52.toInt()
    private val panel = 0xFF20252E.toInt()
    private val muted = 0xFFADB5C2.toInt()
    private fun shape(color:Int,r:Int=12)=GradientDrawable().apply { setColor(color);cornerRadius=dp(r).toFloat() }
    private fun card(title:String,subtitle:String?=null):LinearLayout = LinearLayout(this).apply {
        orientation=LinearLayout.VERTICAL
        background=shape(panel,12)
        setPadding(dp(12),dp(9),dp(12),dp(9))
        addView(label(title,14f).apply {
            setTextColor(gold)
            setTypeface(null,android.graphics.Typeface.BOLD)
            setPadding(0,0,0,0)
        })
        if(subtitle!=null)addView(label(subtitle,11f).apply {
            setTextColor(muted)
            setPadding(0,dp(3),0,0)
        })
    }
    override fun onCreate() {
        super.onCreate()
        if(!Settings.canDrawOverlays(this)){stopSelf();return}
        wm=getSystemService(WINDOW_SERVICE) as WindowManager
        val channel="aqw_bubble"
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(NotificationChannel(channel,"AQW Companion flutuante",NotificationManager.IMPORTANCE_LOW))
        val stopIntent=Intent(this,BubbleService::class.java).setAction(ACTION_STOP)
        val stopPending=PendingIntent.getService(this,58,stopIntent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=Notification.Builder(this,channel).setContentTitle("AQW Companion")
            .setContentText("Bolinha ativa • toque em Encerrar para remover")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .addAction(Notification.Action.Builder(null,"Encerrar",stopPending).build())
            .setOngoing(true).build()
        if(Build.VERSION.SDK_INT>=34) startForeground(57,notification,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(57,notification)
        val prefs=getSharedPreferences("companion_settings",MODE_PRIVATE)
        selected=prefs.getString("selected_ultra","Ultra Dage") ?: "Ultra Dage"
        chosen=prefs.getString("selected_class","Chaos Avenger") ?: "Chaos Avenger"
        willTaunt=prefs.getBoolean("will_taunt",true)
        tauntOrder=prefs.getInt("taunt_order",1)
        val defaultTeam=UltraGuide.bosses.firstOrNull{it.name==selected}?.team ?: emptyList()
        team=(0..3).map { prefs.getString("team_$it",defaultTeam.getOrNull(it) ?: "Lord of Order") ?: "Lord of Order" }.toMutableList()
        params=WindowManager.LayoutParams(dp(58),dp(58),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT).apply {
            gravity=Gravity.TOP or Gravity.START
            x=prefs.getInt("bubble_x",resources.displayMetrics.widthPixels-dp(70))
            y=prefs.getInt("bubble_y",dp(160))
        }
        root=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL}
        wm.addView(root,params)
        render()
    }
    private fun label(s:String,size:Float=13f)=TextView(this).apply {
        text=s;textSize=size;setTextColor(0xFFF1F5FF.toInt());setPadding(dp(7),dp(6),dp(7),dp(6))
    }
    private fun button(s:String,action:()->Unit):TextView=label(s).apply {
        background=shape(panel,9)
        setOnClickListener {action()}
    }
    private fun render(){
        val box=root?:return
        box.removeAllViews()
        box.setPadding(dp(7),dp(7),dp(7),dp(7))
        box.background=GradientDrawable().apply { setColor(0xF80D1118.toInt());cornerRadius=dp(16).toFloat();setStroke(dp(1),0xFF66502D.toInt()) }
        if(!expanded){
            val bubble=label("⚔",25f).apply {
                gravity=Gravity.CENTER
                background=shape(0xFF574321.toInt(),40)
                setTextColor(gold)
                setPadding(0,0,0,0)
                setOnTouchListener(object:View.OnTouchListener {
                    private var moved=false
                    override fun onTouch(v:View,event:android.view.MotionEvent):Boolean {
                        val p=params?:return false
                        when(event.actionMasked){
                            MotionEvent.ACTION_DOWN->{xDown=event.rawX;yDown=event.rawY;initialX=p.x;initialY=p.y;moved=false;return true}
                            MotionEvent.ACTION_MOVE->{
                                if(kotlin.math.abs(event.rawX-xDown)>dp(7)||kotlin.math.abs(event.rawY-yDown)>dp(7))moved=true
                                if(moved){p.x=(initialX+(event.rawX-xDown)).toInt().coerceAtLeast(0);p.y=(initialY+(event.rawY-yDown)).toInt().coerceAtLeast(0);wm.updateViewLayout(box,p)}
                                return true
                            }
                            MotionEvent.ACTION_UP->{
                                getSharedPreferences("companion_settings",MODE_PRIVATE).edit().putInt("bubble_x",p.x).putInt("bubble_y",p.y).apply()
                                if(!moved){expanded=true;resize();render()}
                                return true
                            }
                        }
                        return false
                    }
                })
            }
            box.addView(bubble,LinearLayout.LayoutParams(dp(48),dp(48)))
            return
        }
        val header=LinearLayout(this).apply {orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        header.addView(label("⚔  AQW COMPANION",14f).apply { setTextColor(gold);setTypeface(null,android.graphics.Typeface.BOLD) },LinearLayout.LayoutParams(0,dp(38),1f))
        header.addView(button("—"){expanded=false;resize();render()}.apply {contentDescription="Minimizar bolinha"})
        header.addView(button("×"){stopSelf()}.apply {contentDescription="Encerrar bolinha"})
        box.addView(header)
        val tabs=LinearLayout(this).apply {orientation=LinearLayout.HORIZONTAL}
        listOf("Ultras","Classes","Farm","Forge","Poções").forEach { name->
            val tabButton=button(name){tab=name;showClassPicker=false;resize();render()}.apply {
                gravity=Gravity.CENTER
                textSize=11f
                setPadding(dp(2),dp(7),dp(2),dp(7))
                if(tab==name){background=shape(0xFF574321.toInt(),9);setTextColor(0xFFF7D48A.toInt())}else setTextColor(muted)
            }
            tabs.addView(tabButton,LinearLayout.LayoutParams(0,dp(36),1f).apply {marginEnd=dp(3)})
        }
        box.addView(tabs)
        val scroll=ScrollView(this).apply {isFillViewport=false;isVerticalScrollBarEnabled=false}
        val body=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(dp(3),dp(8),dp(3),dp(6))}
        scroll.addView(body)
        box.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        fun line(s:String){body.addView(label(s,12f))}
        fun heading(s:String){body.addView(label(s,13f).apply{setTextColor(gold);setTypeface(null,android.graphics.Typeface.BOLD)})}
        fun item(s:String,action:()->Unit){
            body.addView(button(s,action).apply{setPadding(dp(12),dp(11),dp(12),dp(11));textSize=12f},
                LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(5)})
        }
        fun copy(join:String){
            (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                .setPrimaryClip(android.content.ClipData.newPlainText("AQW",join))
        }
        fun chooser(title:String,names:List<String>,picked:(String)->Unit){
            val dialog=AlertDialog.Builder(this@BubbleService)
                .setTitle(title).setItems(names.toTypedArray()){_,i->picked(names[i]);render()}
                .setNegativeButton("Fechar",null).create()
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            dialog.show()
        }
        when(tab){
            "Ultras"->{
                item("⚔  $selected   ▾"){
                    chooser("Selecionar Ultra",UltraGuide.bosses.map{it.name}){name->
                        selected=name
                        chosen=UltraGuide.bosses.first{it.name==name}.team.first()
                        detail="Equipe"
                        team=bossTeam(name)
                        getSharedPreferences("companion_settings",MODE_PRIVATE).edit()
                            .putString("selected_ultra",selected).putString("selected_class",chosen).apply()
                    }
                }
                val boss=UltraGuide.bosses.firstOrNull{it.name==selected}?:UltraGuide.bosses.first()
                val subTabs=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
                listOf("Equipe","Build","Boss","Taunt").forEach { name->
                    subTabs.addView(button(name){detail=name;render()}.apply{
                        gravity=Gravity.CENTER;textSize=12f;setPadding(dp(2),dp(9),dp(2),dp(9))
                        if(detail==name){background=shape(0xFF574321.toInt(),9);setTextColor(0xFFF7D48A.toInt())}
                    },LinearLayout.LayoutParams(0,dp(38),1f).apply{marginEnd=dp(3)})
                }
                body.addView(subTabs)
                when(detail){
                    "Equipe"->{
                        heading("Composição recomendada")
                        heading("Monte sua composição (toque para trocar)")
                        team.forEachIndexed{i,c->
                            item("${i+1}. $c   ▾"){
                                val names=(UltraGuide.bosses.flatMap{it.team}+team).distinct().sorted()
                                chooser("Jogador ${i+1}: classe",names){newClass->
                                    team[i]=newClass
                                    getSharedPreferences("companion_settings",MODE_PRIVATE).edit().putString("team_$i",newClass).apply()
                                }
                            }
                        }
                        heading("Sua classe")
                        item("$chosen   ▾"){chooser("Minha classe",team.distinct()){chosen=it}}
                        heading("Sugestão original")
                        boss.team.forEachIndexed{i,c->
                            item("${i+1}. $c\n${boss.roles[i]}"){
                                chosen=c;detail="Build"
                                getSharedPreferences("companion_settings",MODE_PRIVATE).edit().putString("selected_class",c).apply()
                                render()
                            }
                        }
                    }
                    "Build"->{
                        item("$chosen   ▾"){chooser("Classe da equipe",boss.team){chosen=it}}
                        val build=UltraGuide.build(chosen)
                        if(build==null)line("Build individual ainda não verificada.")
                        else{
                            heading("Enhancements")
                            line("Arma: ${build.weapon}")
                            line("Classe: ${build.classEnh}")
                            line("Elmo: ${build.helm}")
                            line("Capa: ${build.cape}")
                            heading("Consumíveis")
                            line("Tônico: ${build.tonic}")
                            line("Elixir: ${build.elixir}")
                            line("Poção: ${build.potion}")
                        }
                    }
                    "Taunt" -> {
                        item("Sua classe: $chosen   ▾") {
                            chooser("Qual classe você está usando?",team.distinct()){chosen=it
                                getSharedPreferences("companion_settings",MODE_PRIVATE).edit()
                                    .putString("selected_class",chosen).apply()
                            }
                        }
                        item("Você vai dar taunt? ${if(willTaunt) "SIM" else "NÃO"}"){
                            willTaunt=!willTaunt
                            getSharedPreferences("companion_settings",MODE_PRIVATE).edit().putBoolean("will_taunt",willTaunt).apply()
                            render()
                        }
                        if(willTaunt)item("Sua vez: ${if(tauntOrder==1) "1º taunt" else "2º taunt"}   ▾"){
                            tauntOrder=if(tauntOrder==1)2 else 1
                            getSharedPreferences("companion_settings",MODE_PRIVATE).edit().putInt("taunt_order",tauntOrder).apply()
                            render()
                        }
                        val guide=UltraGuide.tauntGuide(selected)
                        val classAdvice=UltraGuide.classTaunt(selected,chosen)
                        heading("🎯 $chosen — SUA FUNÇÃO")
                        if(!willTaunt){
                            line("Você NÃO é responsável pelo taunt nesta composição.")
                            line("Mantenha buffs, cura ou DPS e não use Scroll of Enrage sem combinar.")
                        } else {
                            line("Você é o ${tauntOrder}º taunter na rotação combinada.")
                            heading("QUANDO DAR TAUNT?")
                        line(classAdvice.whenToTaunt)
                        heading("O QUE FAZER")
                        line(classAdvice.action)
                        heading("SINAL NA TELA")
                        line(classAdvice.signal)
                        heading("IMPORTANTE")
                        line(classAdvice.warning)
                        if(!classAdvice.confirmed)line("⚠ Rotação exata não confirmada para esta classe. Não inventamos segundos.")
                        }
                        heading("MECÂNICA GERAL")
                        line(guide.timing)
                    }
                    else->{
                        heading("Mecânicas do chefe")
                        boss.mechanics.forEachIndexed{i,m->line("${i+1}. $m")}
                        heading("Dica")
                        line(boss.tip)
                    }
                }
                item("Copiar /join ${boss.join}"){copy("/join ${boss.join}")}
            }
            "Classes"->{
                val allClasses=(WikiDataRepository.cachedClasses(this).map{it.name}
                    +ClassBuildRepository.cached(this).map{it.className}
                    +UltraGuide.bosses.flatMap{it.team}).distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
                item("⚔  $chosen   ▾"){showClassPicker=!showClassPicker;resize();render()}
                if(showClassPicker){
                    line("Catálogo: ${allClasses.size} classes • toque para selecionar")
                    val results=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
                    fun showMatches(query:String){
                        results.removeAllViews()
                        allClasses.filter{it.contains(query,true)}.take(40).forEach{c->
                            results.addView(button(c){chosen=c;showClassPicker=false;resize();render()})
                        }
                    }
                    val input=EditText(this).apply{
                        hint="Pesquisar classe..."
                        setSingleLine(true)
                        textSize=13f
                        setTextColor(Color.WHITE)
                        setHintTextColor(0xFFB0BCD0.toInt())
                        setText(classSearch)
                        setSelectAllOnFocus(false)
                        addTextChangedListener(object:android.text.TextWatcher{
                            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){}
                            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){
                                classSearch=s.toString()
                                showMatches(classSearch)
                            }
                            override fun afterTextChanged(s:android.text.Editable?){}
                        })
                    }
                    body.addView(input)
                    showMatches(classSearch)
                    body.addView(results)
                    if(allClasses.size>40)line("Mostrando até 40 resultados. Digite para filtrar.")
                } else {
                    val modes=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
                    listOf("Farm","Boss","Ultra").forEach{mode->
                        modes.addView(button(mode){classMode=mode;render()}.apply{
                            gravity=Gravity.CENTER
                            if(classMode==mode){background=shape(0xFF574321.toInt(),9);setTextColor(0xFFF7D48A.toInt())}
                        },LinearLayout.LayoutParams(0,dp(36),1f))
                    }
                    body.addView(modes)
                    val build=if(classMode=="Ultra")UltraGuide.build(chosen) else null
                    if(build!=null){
                        heading("Build Ultra de referência")
                        line("Arma: ${build.weapon}")
                        line("Classe: ${build.classEnh}")
                        line("Elmo: ${build.helm}")
                        line("Capa: ${build.cape}")
                        heading("Consumíveis")
                        line("Tônico: ${build.tonic}")
                        line("Elixir: ${build.elixir}")
                        line("Poção: ${build.potion}")
                    }else{
                        val general=ClassBuildRepository.find(this,chosen)
                        if(general!=null){
                            val advice=ClassBuildRepository.advice(general,classMode)
                            heading("Sugestão de ${advice.title} • não validada individualmente")
                            line("Arma: ${advice.weapon}")
                            line("Classe: ${advice.classEnhancement}")
                            line("Elmo: ${advice.helm}")
                            line("Capa: ${advice.cape}")
                            line(advice.note)
                            heading("Combo e consumíveis")
                            line("Ainda sem recomendação específica validada.")
                        }else {
                            heading("Classe disponível no catálogo")
                            line("Build, combo e consumíveis ainda não verificados.")
                        }
                    }
                }
                if(WikiDataRepository.cachedClasses(this).isEmpty())
                    line("Sincronize a AQW Wiki na aba Dados do aplicativo para ampliar o catálogo.")
            }
            "Farm" -> {
                heading("AUTO FARM / AUTO BOSS")
                line("Tudo aqui na bolinha, sem sair do AQW.")
                line("Experimental: não reconhece morte, cooldowns ou inimigos. Não usar em Ultras.")
                val combatPrefs = getSharedPreferences(CombatTapService.PREFS, MODE_PRIVATE)
                val profiles = linkedMapOf(
                    "Legion Revenant" to "4,3,2,5",
                    "ArchMage" to "3,4,5,2",
                    "Arcana Invoker" to "2,3,4,5",
                    "Void Highlord" to "4,3,2,5",
                    "Chaos Avenger" to "2,3,4,5",
                    "Yami no Ronin" to "2,3,4,5"
                )
                val farmClass = combatPrefs.getString("farm_class", "Legion Revenant") ?: "Legion Revenant"
                item("Classe: $farmClass   ▾") {
                    chooser("Classe do Farm", profiles.keys.toList()) { selectedClass ->
                        CombatTapService.instance?.stopLoop()
                        combatPrefs.edit().putString("farm_class", selectedClass)
                            .putString("sequence", profiles[selectedClass]).apply()
                    }
                }
                line("Combo: " + (combatPrefs.getString("sequence", profiles[farmClass]) ?: "2,3,4,5"))
                val service = CombatTapService.instance
                if (service == null) {
                    line("Ative a acessibilidade do AQW Companion para controlar os toques.")
                } else {
                    line(if (service.isLoopRunning()) "Status: ATIVO" else "Status: PARADO")
                    item("▶ INICIAR FARM") {
                        service.startLoop()
                        expanded = false
                        resize()
                        render()
                    }
                    item("■ PARAR FARM") { service.stopLoop(); render() }
                }
                val x1 = combatPrefs.getInt("x1", 40)
                val y = combatPrefs.getInt("y", 94)
                heading("Ajuste dos botões inferiores")
                line("Skill 1: $x1% horizontal | Barra: $y% vertical")
                item("◀ Skills para esquerda") {
                    val editor = combatPrefs.edit()
                    (1..5).forEach { n ->
                        editor.putInt("x$n", (combatPrefs.getInt("x$n", 36+n*4)-1).coerceIn(1,99))
                    }
                    editor.apply();render()
                }
                item("Skills para direita ▶") {
                    val editor = combatPrefs.edit()
                    (1..5).forEach { n ->
                        editor.putInt("x$n", (combatPrefs.getInt("x$n", 36+n*4)+1).coerceIn(1,99))
                    }
                    editor.apply();render()
                }
                item("Barra mais acima ▲") {
                    combatPrefs.edit().putInt("y", (y-1).coerceIn(1,99)).apply();render()
                }
                item("Barra mais abaixo ▼") {
                    combatPrefs.edit().putInt("y", (y+1).coerceIn(1,99)).apply();render()
                }
                item("Restaurar posições conforme print") {
                    val editor = combatPrefs.edit().putInt("y",94)
                    (1..5).forEach { n -> editor.putInt("x$n",36+n*4) }
                    editor.apply();render()
                }
                line("Os ajustes usam a barra de skills inferior como referência. Se estiver clicando no lugar errado, pare o farm antes de ajustar.")
            }
            "Forge"->{
                item("Forge: $selectedForge   ▾"){
                    chooser("Escolher Forge",UltraGuide.forges.map{it.name}){selectedForge=it}
                }
                UltraGuide.forges.firstOrNull{it.name==selectedForge}?.let{
                    heading("${it.name} • ${it.category}")
                    line(it.description)
                }
            }
            else->{
                item("Consumível: $selectedPotion   ▾"){
                    chooser("Escolher consumível",UltraGuide.consumables.map{it.name}){selectedPotion=it;craftStep=0}
                }
                UltraGuide.consumables.firstOrNull{it.name==selectedPotion}?.let{
                    heading(it.name)
                    line(it.note)
                    val craft=UltraGuide.craftGuide(it.name)
                    heading("MATERIAIS NECESSÁRIOS")
                    if(craft.materials.isEmpty())line("Materiais ainda não verificados.")
                    else craft.materials.forEach{m->line("• $m")}
                    heading("COMO FAZER — PASSO A PASSO")
                    val index=craftStep.coerceIn(0,craft.steps.lastIndex)
                    line("Passo ${index+1} de ${craft.steps.size}")
                    line(craft.steps[index])
                    item("◀ Anterior"){craftStep=(craftStep-1).coerceAtLeast(0);render()}
                    item("Próximo ▶"){craftStep=(craftStep+1).coerceAtMost(craft.steps.lastIndex);render()}
                    if(!craft.verified)line("⚠ Guia não verificado; não gaste materiais sem conferir.")
                    line("Fonte: ${craft.source}")
                    item("Copiar /join ${it.join}"){copy("/join ${it.join}")}
                }
            }
        }
        body.addView(label("GUIA DE REFERÊNCIA  •  AQW",10f).apply { setTextColor(muted);gravity=Gravity.CENTER })
    }
    private fun bossTeam(name:String)=UltraGuide.bosses.firstOrNull{it.name==name}?.team?.toMutableList() ?: mutableListOf()
    private fun resize(){
        val p=params?:return
        p.flags=if(expanded && tab=="Classes" && showClassPicker) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        p.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        p.width=if(expanded) dp(320).coerceAtMost(resources.displayMetrics.widthPixels-dp(24)) else dp(58)
        p.height=if(expanded) (resources.displayMetrics.heightPixels*0.43).toInt().coerceAtLeast(dp(270)) else dp(58)
        p.x=p.x.coerceIn(0,(resources.displayMetrics.widthPixels-p.width).coerceAtLeast(0))
        root?.let{wm.updateViewLayout(it,p)}
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action==ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }
    override fun onDestroy(){root?.let{wm.removeView(it)};root=null;super.onDestroy()}
    override fun onBind(intent:Intent?):IBinder?=null
}
