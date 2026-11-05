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
    private var xDown=0f
    private var yDown=0f
    private var initialX=0
    private var initialY=0
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun shape(color:Int,r:Int=15)=GradientDrawable().apply { setColor(color);cornerRadius=dp(r).toFloat() }
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
        text=s;textSize=size;setTextColor(Color.WHITE);setPadding(dp(8),dp(7),dp(8),dp(7))
    }
    private fun button(s:String,action:()->Unit):TextView=label(s).apply {
        background=shape(0xFF2A3E65.toInt(),10)
        setOnClickListener {action()}
    }
    private fun render(){
        val box=root?:return
        box.removeAllViews()
        box.setPadding(dp(5),dp(5),dp(5),dp(5))
        box.background=shape(0xF0121C30.toInt(),17)
        if(!expanded){
            val bubble=label("⚔",25f).apply {
                gravity=Gravity.CENTER
                background=shape(0xFF4566C8.toInt(),40)
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
        header.addView(label("AQW • GUIA",15f),LinearLayout.LayoutParams(0,dp(40),1f))
        header.addView(button("×"){stopSelf()}.apply {contentDescription="Encerrar bolinha"})
        header.addView(button("—"){expanded=false;resize();render()}.apply {contentDescription="Minimizar bolinha"})
        box.addView(header)
        val tabs=LinearLayout(this).apply {orientation=LinearLayout.HORIZONTAL}
        listOf("Ultras","Classes","Forge","Poções").forEach { name->
            val tabButton=button(name){tab=name;render()}.apply {
                gravity=Gravity.CENTER
                textSize=11f
                setPadding(dp(2),dp(7),dp(2),dp(7))
                if(tab==name)background=shape(0xFF4566C8.toInt(),10)
            }
            tabs.addView(tabButton,LinearLayout.LayoutParams(0,dp(36),1f).apply {marginEnd=dp(3)})
        }
        box.addView(tabs)
        val scroll=ScrollView(this).apply {isFillViewport=false;isVerticalScrollBarEnabled=false}
        val body=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(dp(3),dp(8),dp(3),dp(6))}
        scroll.addView(body)
        box.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        fun line(s:String){body.addView(label(s,12f))}
        fun heading(s:String){body.addView(label(s,14f).apply{setTypeface(null,android.graphics.Typeface.BOLD)})}
        fun item(s:String,action:()->Unit){
            body.addView(button(s,action).apply{setPadding(dp(10),dp(10),dp(10),dp(10));textSize=12f},
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
                        getSharedPreferences("companion_settings",MODE_PRIVATE).edit()
                            .putString("selected_ultra",selected).putString("selected_class",chosen).apply()
                    }
                }
                val boss=UltraGuide.bosses.firstOrNull{it.name==selected}?:UltraGuide.bosses.first()
                val subTabs=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
                listOf("Equipe","Build","Boss").forEach { name->
                    subTabs.addView(button(name){detail=name;render()}.apply{
                        gravity=Gravity.CENTER;textSize=12f;setPadding(dp(2),dp(9),dp(2),dp(9))
                        if(detail==name)background=shape(0xFF4566C8.toInt(),10)
                    },LinearLayout.LayoutParams(0,dp(38),1f).apply{marginEnd=dp(3)})
                }
                body.addView(subTabs)
                when(detail){
                    "Equipe"->{
                        heading("Composição recomendada")
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
                item("Classe: $chosen   ▾"){
                    chooser("Selecionar classe",UltraGuide.bosses.flatMap{it.team}.distinct().sorted()){chosen=it}
                }
                UltraGuide.build(chosen)?.let {
                    heading("Build de referência")
                    line("Arma: ${it.weapon}")
                    line("Classe: ${it.classEnh}")
                    line("Elmo: ${it.helm}")
                    line("Capa: ${it.cape}")
                    heading("Consumíveis")
                    line("Tônico: ${it.tonic}")
                    line("Elixir: ${it.elixir}")
                    line("Poção: ${it.potion}")
                }?:line("Build ainda não verificada.")
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
                    chooser("Escolher consumível",UltraGuide.consumables.map{it.name}){selectedPotion=it}
                }
                UltraGuide.consumables.firstOrNull{it.name==selectedPotion}?.let{
                    heading(it.name)
                    line(it.note)
                    item("Copiar /join ${it.join}"){copy("/join ${it.join}")}
                }
            }
        }
        line("Guia offline de referência • sujeito a atualizações")
    }
    private fun resize(){
        val p=params?:return
        p.width=if(expanded) dp(300).coerceAtMost(resources.displayMetrics.widthPixels-dp(24)) else dp(58)
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
