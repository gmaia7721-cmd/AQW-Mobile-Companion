package com.aqw.companion

/** Local, sandboxed scripting prototype. Never sends commands to the game. */
class ScriptRuntime(private val bridge: GameStateBridge = DemoGameStateBridge()) {
    data class Result(val logs: List<String>, val stopped: Boolean = false)
    @Volatile private var cancelled = false
    fun stop() { cancelled = true }
    fun run(source: String): Result {
        cancelled = false
        val logs = mutableListOf<String>()
        val lines = source.lineSequence().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }.take(101).toList()
        if (lines.size > 100) return Result(listOf("Limite: 100 comandos por execução"))
        for ((index, line) in lines.withIndex()) {
            if (cancelled) return Result(logs + "Execução interrompida", true)
            val cmd = line.substringBefore(' ').lowercase()
            val arg = line.substringAfter(' ', "").trim()
            when (cmd) {
                "status" -> logs += "Mapa: ${bridge.snapshot().map}; classe: ${bridge.snapshot().playerClass}"
                "targets" -> logs += "Alvos de demonstração: ${bridge.snapshot().targets.joinToString()}"
                "skills" -> logs += "Habilidades de demonstração: ${bridge.snapshot().skills.joinToString()}"
                "echo" -> logs += arg.take(200)
                else -> logs += "Linha ${index + 1}: comando desconhecido: ${cmd.take(30)}"
            }
        }
        return Result(logs)
    }
}
