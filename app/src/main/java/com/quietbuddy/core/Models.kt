package com.quietbuddy.core

/** Buddy's five moods, ordered from calm to loud. */
enum class Mood { HAPPY, FINE, WORRIED, SAD, EARS_COVERED }

/** Whether the app is currently measuring. Anything but LISTENING shows Buddy asleep. */
enum class ListeningState { LISTENING, PAUSED, NO_PERMISSION, MIC_BUSY }

/** One processed 100 ms frame. [laeq2sDb] is the rolling 2 s energy average, calibrated. */
data class LevelSample(
    val timestampMs: Long,
    val frameDb: Float,
    val laeq2sDb: Float,
)

/** One minute of history, produced by LevelProcessor. Energy-averaged, calibrated. */
data class MinuteAggregate(
    val minuteStartMs: Long,
    val laeqDb: Float,
    val minDb: Float,
    val maxDb: Float,
    val listeningSeconds: Int,
)

enum class Preset(val worriedFromDb: Float, val sadFromDb: Float) {
    QUIET_TIME(55f, 65f),
    EVERYDAY(65f, 75f),
    PLAYTIME(72f, 82f),
}

/**
 * Band edges. Happy below worried-10, Fine up to worried, Worried up to sad,
 * Sad up to sad+10, Ears covered from sad+10.
 */
data class Thresholds(val worriedFromDb: Float, val sadFromDb: Float) {
    val fineFromDb: Float get() = worriedFromDb - 10f
    val earsFromDb: Float get() = sadFromDb + 10f

    /** Lower edge (dB) of each mood's band; HAPPY has none. */
    fun lowerEdge(mood: Mood): Float = when (mood) {
        Mood.HAPPY -> Float.NEGATIVE_INFINITY
        Mood.FINE -> fineFromDb
        Mood.WORRIED -> worriedFromDb
        Mood.SAD -> sadFromDb
        Mood.EARS_COVERED -> earsFromDb
    }

    /** Mood for a level with no hysteresis. */
    fun rawMood(db: Float): Mood = when {
        db >= earsFromDb -> Mood.EARS_COVERED
        db >= sadFromDb -> Mood.SAD
        db >= worriedFromDb -> Mood.WORRIED
        db >= fineFromDb -> Mood.FINE
        else -> Mood.HAPPY
    }

    fun shiftedBy(deltaDb: Float) = Thresholds(worriedFromDb + deltaDb, sadFromDb + deltaDb)
}

data class AppSettings(
    val roomName: String = "Living room",
    val preset: Preset? = Preset.EVERYDAY,
    val worriedFromDb: Float = Preset.EVERYDAY.worriedFromDb,
    val sadFromDb: Float = Preset.EVERYDAY.sadFromDb,
    val chimeOnSad: Boolean = false,
    val keepListening: Boolean = false,
    val keepScreenOn: Boolean = true,
    val calibrationOffsetDb: Float = DEFAULT_CALIBRATION_OFFSET_DB,
    val calibratedAtMs: Long? = null,
    val onboardingDone: Boolean = false,
    val character: Character = Character.DEFAULT,
    /** Selected style per character; a missing entry means [CharacterStyle.defaultFor]. */
    val styles: Map<Character, CharacterStyle> = emptyMap(),
    /** Accessories each character is wearing, in any combination; a missing entry means none. */
    val accessories: Map<Character, Set<Accessory>> = emptyMap(),
) {
    val baseThresholds: Thresholds get() = Thresholds(worriedFromDb, sadFromDb)

    fun styleFor(c: Character): CharacterStyle =
        styles[c]?.takeIf { it.character == c } ?: CharacterStyle.defaultFor(c)

    fun accessoriesFor(c: Character): Set<Accessory> = accessories[c].orEmpty().filterTo(mutableSetOf()) { it.character == c }

    /**
     * Puts [accessory] on [c], alongside whatever else it wears, or takes it off if already worn.
     * An accessory that belongs to another character is ignored.
     */
    fun toggleAccessory(c: Character, accessory: Accessory): AppSettings {
        if (accessory.character != c) return this
        val worn = accessoriesFor(c)
        val next = if (accessory in worn) worn - accessory else worn + accessory
        return copy(accessories = accessories + (c to next))
    }

    companion object {
        /** Offset added to dBFS to get approximate dB SPL before the user calibrates. */
        const val DEFAULT_CALIBRATION_OFFSET_DB = 90f
    }
}

/** One bar in History's hourly chart. */
data class HourBar(val hour: Int, val laeqDb: Float)

/** A day as History shows it. Computed from minute rows using the thresholds current at query time. */
data class DaySummary(
    val dateEpochDay: Long,
    val calmMinutes: Int,
    val loudestDb: Float?,
    val loudestAtMs: Long?,
    val dominantMood: Mood?,
    val challengesWon: Int,
)
