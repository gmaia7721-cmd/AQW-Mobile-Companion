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
    private lateinit var wm: WindowManager
    private var root: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var expanded=false
    private var tab="Ultras"
    private var selected="Ultra Dage"
    private var chosen="Chaos Avenger"
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
        val notification=Notification.Builder(this,channel).setContentTitle("AQW Companion").setContentText("Toque na bolinha para consultar os guias").setSmallIcon(android.R.drawable.ic_menu_info_details).setOngoing(true).build()
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
        header.addView(label("AQW COMPANION",16f),LinearLayout.LayoutParams(0,dp(43),1f))
        header.addView(button("—"){expanded=false;resize();render()})
        box.addView(header)
        val tabs=LinearLayout(this).apply {orientation=LinearLayout.HORIZONTAL}
        listOf("Ultras","Classes","Forge","Poções").forEach { name->
            tabs.addView(button(name){tab=name;render()},LinearLayout.LayoutParams(0,dp(43),1f))
        }
        box.addView(tabs)
        val scroll=ScrollView(this)
        val body=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL}
        scroll.addView(body)
        box.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        fun line(s:String){body.addView(label(s))}
        fun item(s:String,action:()->Unit){body.addView(button(s,action),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(5)})}
        fun copy(join:String){
            (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("AQW",join))
        }
        when(tab){
            "Ultras"->{
                item("Chefe: $selected  ▾"){
                    val names=UltraGuide.bosses.map{it.name}.toTypedArray()
                    AlertDialog.Builder(this@BubbleService).setTitle("Selecionar Ultra").setItems(names){_,which->
                        selected=names[which];chosen=UltraGuide.bosses[which].team.first()
                        getSharedPreferences("companion_settings",MODE_PRIVATE).edit().putString("selected_ultra",selected).apply()
                        render()
                    }.setNegativeButton("Fechar",null).create().apply{window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);show() }
                }
                val b=UltraGuide.bosses.firstOrNull{it.name==selected}?:UltraGuide.bosses[0]
                item("Copiar /join ${b.join}"){copy("/join ${b.join}")}
                line("EQUIPE • toque na classe")
                b.team.forEachIndexed{i,c->item("${i+1}. $c — ${b.roles[i]}"){chosen=c;render()}}
                line("BUILD: $chosen")
                val build=UltraGuide.build(chosen)
                if(build!=null){
                    line("Weapon: ${build.weapon}\nClass: ${build.classEnh}\nHelm: ${build.helm}\nCape: ${build.cape}")
                    line("Tônico: ${build.tonic}\nElixir: ${build.elixir}\nPoção: ${build.potion}")
                }else line("Build individual ainda não verificada.")
                line("MECÂNICAS")
                b.mechanics.forEachIndexed{i,m->line("${i+1}. $m")}
                line("DICA: ${b.tip}")
                line("Estratégia de referência, sujeita a atualizações do jogo.")
            }
            "Classes"->{
                line("CLASSES • builds nativas de referência")
                UltraGuide.bosses.flatMap{it.team}.distinct().sorted().forEach{c->
                    item(c){chosen=c;render()}
                }
                line("SELECIONADA: $chosen")
                UltraGuide.build(chosen)?.let{line("Weapon: ${it.weapon}\nClass: ${it.classEnh}\nHelm: ${it.helm}\nCape: ${it.cape}\nTônico: ${it.tonic}\nElixir: ${it.elixir}\nPoção: ${it.potion}")}
            }
            "Forge"->UltraGuide.forges.forEach{f->line("${f.name} • ${f.category}\n${f.description}")}
            else->UltraGuide.consumables.forEach{p->item("${p.name} • /join ${p.join}\n${p.note}"){copy("/join ${p.join}")}}
        }
    }
    private fun resize(){
        val p=params?:return
        p.width=if(expanded) dp(330).coerceAtMost(resources.displayMetrics.widthPixels-dp(16)) else dp(58)
        p.height=if(expanded) (resources.displayMetrics.heightPixels*0.66).toInt() else dp(58)
        p.x=p.x.coerceIn(0,(resources.displayMetrics.widthPixels-p.width).coerceAtLeast(0))
        root?.let{wm.updateViewLayout(it,p)}
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int)=START_STICKY
    override fun onDestroy(){root?.let{wm.removeView(it)};root=null;super.onDestroy()}
    override fun onBind(intent:Intent?):IBinder?=null
}
