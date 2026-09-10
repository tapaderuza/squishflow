package com.example.squishfocus

expect object MissionPersistence {
    fun load(): String?
    fun save(value: String)
    fun clear()
}

private fun String.toHex(): String = encodeToByteArray().joinToString("") { byte ->
    byte.toUByte().toString(16).padStart(2, '0')
}

private fun String.fromHex(): String = chunked(2)
    .map { it.toInt(16).toByte() }
    .toByteArray()
    .decodeToString()

fun FocusMission.serialize(index: Int, accepted: Boolean): String = buildString {
    append("1|").append(index).append('|').append(if (accepted) 1 else 0)
    append('|').append(title.toHex()).append('|').append(originalGoal.toHex())
    blocks.forEach { block ->
        append('|').append(block.kind.name).append(',').append(block.minutes).append(',').append(block.title.toHex())
    }
}

data class PersistedMission(val mission: FocusMission, val index: Int, val accepted: Boolean)

fun deserializeMission(value: String?): PersistedMission? = runCatching {
    val fields = value?.split('|') ?: return null
    if (fields.size < 6 || fields[0] != "1") return null
    val blocks = fields.drop(5).map { encoded ->
        val parts = encoded.split(',', limit = 3)
        MissionBlock(parts[2].fromHex(), parts[1].toInt(), BlockKind.valueOf(parts[0]))
    }
    PersistedMission(
        mission = FocusMission(fields[3].fromHex(), fields[4].fromHex(), blocks),
        index = fields[1].toInt().coerceIn(0, blocks.lastIndex.coerceAtLeast(0)),
        accepted = fields[2] == "1",
    )
}.getOrNull()
