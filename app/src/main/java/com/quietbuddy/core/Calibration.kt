package com.quietbuddy.core

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Bounds for microphone calibration. Anything outside them is a typo or a broken value, and an
 * offset of Infinity or NaN would otherwise end up stored and crash every screen that shows a level.
 */
object Calibration {
    /** Lowest sound meter reading we accept; quieter than a silent bedroom. */
    const val MIN_REFERENCE_DB = 20f

    /** Highest sound meter reading we accept; well past anything a room of children should reach. */
    const val MAX_REFERENCE_DB = 130f

    /** How far a calibrated offset may sit from [AppSettings.DEFAULT_CALIBRATION_OFFSET_DB]. */
    const val MAX_OFFSET_DEVIATION_DB = 60f

    /**
     * The reading typed on the calibration screen, or null if it is not a number in
     * [MIN_REFERENCE_DB]..[MAX_REFERENCE_DB]. Takes a decimal comma too, as es/eu keyboards offer it.
     */
    fun parseReferenceDb(text: String): Float? {
        val value = text.trim().replace(',', '.').toFloatOrNull() ?: return null
        return value.takeIf { it.isFinite() && it in MIN_REFERENCE_DB..MAX_REFERENCE_DB }
    }

    fun isSaneOffset(offsetDb: Float): Boolean =
        offsetDb.isFinite() && abs(offsetDb - AppSettings.DEFAULT_CALIBRATION_OFFSET_DB) <= MAX_OFFSET_DEVIATION_DB

    /** The offset that makes [readingDb] show as [referenceDb], or null if it would not be sane. */
    fun correctedOffset(currentOffsetDb: Float, referenceDb: Float, readingDb: Float): Float? =
        (currentOffsetDb + (referenceDb - readingDb)).takeIf { isSaneOffset(it) }

    /** A stored offset, or the default if it is missing or broken. */
    fun sanitizeStoredOffset(storedDb: Float?): Float =
        storedDb?.takeIf { isSaneOffset(it) } ?: AppSettings.DEFAULT_CALIBRATION_OFFSET_DB
}

/** [roundToInt] for display: null instead of a crash when the value is NaN or infinite. */
fun Float.roundToIntOrNull(): Int? = if (isFinite()) roundToInt() else null
