package com.aqw.companion

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Standalone sandbox editor; no access to the live game. */
class ScriptLabActivity : Activity() {
    private val runtime = ScriptRuntime()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.rgb(15, 20, 29))
        }
        val title = TextView(this).apply {
            text = "SCRIPT LAB • SIMULADOR"
            textSize = 20f
            setTextColor(Color.WHITE)
        }
        val editor = EditText(this).apply {
            setText("status\ntargets\nskills\necho Teste do Companion X")
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setBackgroundColor(Color.rgb(35, 42, 54))
            minLines = 7
            gravity = android.view.Gravity.TOP
        }
        val output = TextView(this).apply {
            text = "Nenhum script executado. Apenas dados simulados."
            setTextColor(Color.WHITE)
            textSize = 14f
        }
        val execute = Button(this).apply {
            text = "Executar no simulador"
            setOnClickListener {
                val result = runtime.run(editor.text.toString())
                output.text = result.logs.joinToString("\n").ifEmpty { "Script vazio" }
            }
        }
        val stop = Button(this).apply {
            text = "Parar"
            setOnClickListener { runtime.stop(); output.text = "Parada solicitada" }
        }
        root.addView(title)
        root.addView(editor)
        root.addView(execute)
        root.addView(stop)
        root.addView(output)
        setContentView(ScrollView(this).apply { addView(root) })
    }
}
