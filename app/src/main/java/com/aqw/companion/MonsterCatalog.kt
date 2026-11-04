package com.aqw.companion

import java.text.Normalizer
import java.util.Locale

/**
 * Offline name index for visible AQW monster labels.
 * Names are based on the AQWorlds Wiki's "Monsters (All)" index and common
 * in-game names. This is an initial offline catalogue, not a guarantee that
 * every seasonal/new/renamed creature is included.
 */
object MonsterCatalog {
    private val names = listOf(
        "Abaddon", "Abandoned Dolly", "Abominator", "Abomineator", "Abyssal Bat",
        "Abyssal Guard", "Abyssal Mage", "Abyssal Underbeast", "Acornent", "Actual Sloth Dragon",
        "Akriloth", "Aku", "Albino Bat", "Alien Dino", "Alien Juggernaut", "Amethyst Golem",
        "Ancient Dread Dragon", "Ancient Golem", "Ancient Horror", "Ancient Maggot",
        "Ancient Mole", "Ancient Prowler", "Ancient Terror", "Anglerfish", "Angry Zombie",
        "Anubis Deathguard", "Aqueevil", "Aracara", "Archwraith", "Arctic Direwolf",
        "Ashax", "Aster", "Baby Trigoras", "Bad Apple", "Bamboo Treeant", "Bandit",
        "Banished Banshee", "Barrier Bot", "Barrier Sentry", "Beach Werewolf",
        "Beast Maker", "Beast of Pirate's Bay", "Belrot the Fiend", "Big Bad Boar",
        "Big Jack Sprat", "Black Dragon", "Black Knight", "Black Light Elemental",
        "Blastbones", "Blighted Deer", "Blighted Dragon", "Blighted Lion", "Blighted Stalker",
        "Blighted Wolf", "Blightfang", "Blood Dracolich", "Blood Dragon", "Blood Minion",
        "Bone Berserker", "Bone Terror", "Bone Wyvern", "Bonecrusher", "Bonemuncher",
        "Boss Zardman", "Box Guardian", "Braken Head", "Braken Tentacle", "Bronze Draconian",
        "Brutalcorn", "Burning Witch", "Cactus Creeper", "Castle Guard", "Castle Spider",
        "Castle Wraith", "Cat Clothed Shark", "Cave Creeper", "Cave Lizard", "Cave Yeti",
        "Cerberus", "Chainsaw Sneevil", "Chaonslaught Caster", "Chaonslaught Cavalry",
        "Chaonslaught Warrior", "Chaorrupted Armor", "Chaorrupted Bear", "Chaorrupted Knight",
        "Chaorrupted Wolf", "Chaos Angler", "Chaos Beast", "Chaos Dracolich", "Chaos Dragon",
        "Chaos Drow", "Chaos Fluffy", "Chaos Ghost", "Chaos Giant", "Chaos Goat",
        "Chaos Gorillaphant", "Chaos Harpy", "Chaos Lycan", "Chaos Manticore",
        "Chaos Rat", "Chaos Rhino Beetle", "Chaos Spider", "Chaos Tigriff", "Chaos Turtle",
        "Chaos Vampire Knight", "Chicken", "Chickencow", "Chimera", "Chinchilizard",
        "Cloaked Fiend", "Cloud Shark", "Coal Ghoul", "Corn Minion", "Corn Stalker",
        "Crusader", "Cryptkeeper Lich", "Cutie Makai", "Cyclops Raider", "Cyclops Warlord",
        "Dage the Dark Lord", "Dakka the Dire Dragon", "Dark Assassin", "Dark Caster",
        "Dark Draconian", "Dark Dracozombie", "Dark Elemental", "Dark Fairy", "Dark Djinn",
        "Darkovia Werewolf", "Death", "DeathKnight", "Deathgazer", "Deathless",
        "Deep Dweller", "Demon", "Demon Warrior", "Dire Wolf", "Doomknight",
        "Dreadspider", "Dwakel Blaster", "Dwakel Caster", "Dwakel Soldier", "Dwakel Warrior",
        "Earth Elemental", "Ebil Sneezer", "Ebon Dungeon Guard", "Ebonslate Bruiser",
        "Ebonslate Guard", "Ebonslate Imp", "Ebonslate Knight", "Ectomancer", "Ectoplasm",
        "Evil Elemental", "Famine", "Fang Flame Warrior", "Farmer Zombie", "Fart Elemental",
        "Fear Feeder", "Fear Gorta", "Fear Imp", "Fear Monger", "Fear Muncher",
        "Fire Elemental", "Fishbones", "Fishman Soldier", "Fishwing", "Flamethrower Dwakel",
        "Frogdrake", "Frogzard", "General Porkon", "Ghoul", "Golden Draconian",
        "Grand Inquisitor", "Green Rat", "Grizzly Bear", "Groglurk", "Grumble",
        "Hail Elemental", "Hapless Skeleton", "Harpy", "Harvest Golem", "Harvest Sneevil",
        "Hatchling", "Haunted Closet", "Haunted Hamster", "Ice Bear", "Ice Demon",
        "Ice Elemental", "Ice Giant", "Ice Golem", "Igneous Lava Crab", "Inquisitor Captain",
        "Inquisitor Guard", "Jack Frost", "Jellyfish", "Kalestri Hound", "Kathool",
        "Kathool Kultist", "King Alteon's Knight", "Kittarian Mouse Eater",
        "Kittarian Mouse Flayer", "Kuro", "Ladon", "Large Toxic Beast", "Leatherwing",
        "Lich", "Magma Blazebeard", "Magma Pirate", "Magman", "Marsh Lurker",
        "Marsh Tree", "Mithril Man", "Mosquito", "Naga Baas", "Nature Elemental",
        "Necro Minion", "Necropolis Soldier", "Nightbane", "Orc Noob", "Orc Trainer",
        "Orc Warrior", "Phantom Jaws", "Piggy Drake", "Pine Grizzly", "Pine Troll",
        "Purple Draconian", "Purple Slime", "Rattlebones", "Red Ant", "Red Dragon",
        "Red Trobble", "River Fishman", "Rock Elemental", "Scarecrow", "Scarabian",
        "Skeletal Fire Mage", "Skeletal Ice Mage", "Skeletal Soldier", "Skeletal Viking",
        "Skeletal Warrior", "Slime", "Snail", "Sneevil", "Soulseeker", "Speyeder",
        "Spider", "Thrax IronHide", "Treeant", "Trobble", "Undead Berserker",
        "Undead Giant", "Undead Pirate", "Undead Voltaire", "Vampire Knight",
        "Venom Draconian", "Water Draconian", "Water Elemental", "Wereboar", "Werepyre",
        "Werewolf", "Wicked Witch", "Wisteria", "Wyvern", "Xan", "Xanta Claus",
        "Xyfrag", "Yargol Magebane", "Zardman Fisher", "Zardman Grunt", "Zardman Hunter",
        "Zognax", "Zom-Bay", "Zombeh", "Zombie", "Zombie Dragon", "Zombie Warrior",
        "Ultra Dage", "Ultra Nulgath", "Ultra Speaker", "Ultra Drago", "Ultra Engineer",
        "Ultra Ezrajal", "Ultra Gramiel", "Ultra Warden", "Champion Drakath",
        "Kathool Atchoo", "Malgor", "Drakath", "Nulgath", "Dage the Evil",
        "Queen of Monsters", "Sepulchure", "Vordred", "Akriloth", "Tara",
        "Big Bad Boar", "Ogug Stoneaxe", "Brentan", "Flibbitiestgibbet"
    ).distinct()

    private val normalized = names.associateBy { normalize(it) }

    fun findMatches(ocrLines: List<String>): List<String> {
        val found = linkedSetOf<String>()
        for (raw in ocrLines) {
            val candidate = cleanOcrLine(raw)
            if (candidate.isBlank()) continue
            val key = normalize(candidate)
            val exact = normalized[key]
            if (exact != null) {
                found.add(exact)
                continue
            }
            // OCR sometimes confuses a single character. Keep fuzzy matching
            // conservative to avoid treating player/class labels as monsters.
            if (key.length >= 7) {
                val match = normalized.entries.firstOrNull { (known, _) ->
                    kotlin.math.abs(known.length - key.length) <= 1 &&
                        editDistanceAtMostOne(known, key)
                }?.value
                if (match != null) found.add(match)
            }
        }
        return found.toList()
    }

    private fun cleanOcrLine(value: String): String {
        return value.trim()
            .replace(Regex("\\s+(Lv\\.?|Level)\\s*\\d+.*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+\\d{1,3}\\s*/\\s*\\d{1,3}.*$"), "")
            .trim()
    }

    private fun normalize(value: String): String {
        val plain = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return plain.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]"), "")
    }

    private fun editDistanceAtMostOne(a: String, b: String): Boolean {
        if (a == b) return true
        if (kotlin.math.abs(a.length - b.length) > 1) return false
        var i = 0
        var j = 0
        var edits = 0
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) {
                i++; j++
            } else {
                edits++
                if (edits > 1) return false
                if (a.length > b.length) i++
                else if (b.length > a.length) j++
                else { i++; j++ }
            }
        }
        if (i < a.length || j < b.length) edits++
        return edits <= 1
    }
}
