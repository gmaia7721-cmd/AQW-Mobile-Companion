package com.aqw.companion

/** Explicit read-only contract. No APK patching, memory access or protocol interception. */
interface GameStateBridge {
    data class Snapshot(
        val map: String,
        val playerClass: String,
        val targets: List<String>,
        val skills: List<String>,
        val source: String
    )
    fun snapshot(): Snapshot
}

/** Sample data only; not a live AQW connection. */
class DemoGameStateBridge : GameStateBridge {
    override fun snapshot() = GameStateBridge.Snapshot(
        map = "demo-room",
        playerClass = "Legion Revenant (demo)",
        targets = listOf("Training Dummy A", "Training Dummy B"),
        skills = listOf("Skill 1", "Skill 2", "Skill 3", "Skill 4"),
        source = "SIMULATED"
    )
}
