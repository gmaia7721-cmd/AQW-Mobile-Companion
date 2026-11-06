package com.aqw.companion
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class CombatActivity : Activity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val p=getSharedPreferences(CombatTapService.PREFS,MODE_PRIVATE)
  val root=LinearLayout(this).apply { orientation=1;setPadding(24,24,24,24) }
  fun label(t:String){root.addView(TextView(this).apply{text=t;textSize=16f;setPadding(0,12,0,4)})}
  label("Auto Farm / Auto Boss (experimental)")
  label("Não detecta HP, morte ou cooldowns. Não usar em Ultras. Pode contrariar as regras do jogo.")
  val profiles=linkedMapOf("Farm - Legion Revenant" to "4,3,2,5","Farm - ArchMage" to "3,4,5,2","Farm - Arcana Invoker" to "2,3,4,5","Boss - Void Highlord" to "4,3,2,5","Boss - Chaos Avenger" to "2,3,4,5","Boss - Yami no Ronin" to "2,3,4,5")
  val names=profiles.keys.toList()
  val spinner=Spinner(this)
  spinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,names)
  root.addView(spinner)
  fun field(title:String,default:String):EditText {
   label(title)
   return EditText(this).also{it.setSingleLine(true);it.setText(default);root.addView(it)}
  }
  val seq=field("Sequência de skills (1 a 5)",p.getString("sequence","2,3,4,5") ?: "2,3,4,5")
  val delay=field("Intervalo entre cliques em milissegundos (mín. 350)",p.getLong("interval",1200).toString())
  val y=field("Altura das skills (% da tela)",p.getInt("y",85).toString())
  val xs=(1..5).map{n->field("Posição horizontal da skill "+n+" (% da tela)",p.getInt("x"+n,12+n*14).toString())}
  spinner.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener {
   override fun onNothingSelected(parent:android.widget.AdapterView<*>?){}
   override fun onItemSelected(parent:android.widget.AdapterView<*>?,view:android.view.View?,position:Int,id:Long){seq.setText(profiles[names[position]])}
  }
  root.addView(Button(this).apply {
   text="Salvar configuração"
   setOnClickListener{
    val steps=seq.text.toString().split(',',' ','-','>').mapNotNull{it.trim().toIntOrNull()}
    if(steps.isEmpty()||steps.any{it !in 1..5}){Toast.makeText(this@CombatActivity,"Use apenas skills 1 a 5",Toast.LENGTH_LONG).show();return@setOnClickListener}
    val editor=p.edit().putString("sequence",steps.joinToString(","))
     .putLong("interval",(delay.text.toString().toLongOrNull()?:1200L).coerceIn(350L,10000L))
     .putInt("y",(y.text.toString().toIntOrNull()?:85).coerceIn(1,99))
    xs.forEachIndexed{index,edit->editor.putInt("x"+(index+1),(edit.text.toString().toIntOrNull()?:50).coerceIn(1,99))}
    editor.apply()
    Toast.makeText(this@CombatActivity,"Salvo. Controle os cliques pela bolinha.",Toast.LENGTH_LONG).show()
   }
  })
  root.addView(Button(this).apply{text="Ativar acessibilidade";setOnClickListener{startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}})
  root.addView(Button(this).apply{text="PARAR cliques";setOnClickListener{CombatTapService.instance?.stopLoop()}})
  setContentView(ScrollView(this).apply{addView(root)})
 }
}
