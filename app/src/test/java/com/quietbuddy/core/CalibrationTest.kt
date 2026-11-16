package com.quietbuddy.core

import com.quietbuddy.core.AppSettings.Companion.DEFAULT_CALIBRATION_OFFSET_DB
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalibrationTest {

    @Test
    fun `accepts plausible readings with a dot or a comma`() {
        assertEquals(45f, Calibration.parseReferenceDb("45")!!, 0f)
        assertEquals(45.5f, Calibration.parseReferenceDb("45.5")!!, 0f)
        assertEquals(45.5f, Calibration.parseReferenceDb("45,5")!!, 0f)
        assertEquals(62f, Calibration.parseReferenceDb(" 62 ")!!, 0f)
    }

    @Test
    fun `accepts the ends of the range`() {
        assertEquals(Calibration.MIN_REFERENCE_DB, Calibration.parseReferenceDb("20")!!, 0f)
        assertEquals(Calibration.MAX_REFERENCE_DB, Calibration.parseReferenceDb("130")!!, 0f)
    }

    @Test
    fun `rejects non-numbers, non-finite values and readings out of range`() {
        for (text in listOf("", " ", "abc", "4 5", "45,5,1", "NaN", "Infinity", "-Infinity", "9".repeat(45), "19.9", "130.1", "-50", "0")) {
            assertNull("\"$text\"", Calibration.parseReferenceDb(text))
        }
    }

    @Test
    fun `corrected offset moves by the difference between reference and reading`() {
        assertEquals(95f, Calibration.correctedOffset(90f, referenceDb = 65f, readingDb = 60f)!!, 1e-4f)
        assertEquals(80f, Calibration.correctedOffset(90f, referenceDb = 50f, readingDb = 60f)!!, 1e-4f)
    }

    @Test
    fun `corrected offset is null when it would leave the sane range or is not finite`() {
        assertNull(Calibration.correctedOffset(140f, referenceDb = 130f, readingDb = 20f))
        assertNull(Calibration.correctedOffset(90f, referenceDb = 60f, readingDb = Float.NaN))
        assertNull(Calibration.correctedOffset(Float.POSITIVE_INFINITY, referenceDb = 60f, readingDb = Float.POSITIVE_INFINITY))
    }

    @Test
    fun `stored offset falls back to the default when missing or broken`() {
        assertEquals(DEFAULT_CALIBRATION_OFFSET_DB, Calibration.sanitizeStoredOffset(null), 0f)
        assertEquals(DEFAULT_CALIBRATION_OFFSET_DB, Calibration.sanitizeStoredOffset(Float.NaN), 0f)
        assertEquals(DEFAULT_CALIBRATION_OFFSET_DB, Calibration.sanitizeStoredOffset(Float.POSITIVE_INFINITY), 0f)
        assertEquals(DEFAULT_CALIBRATION_OFFSET_DB, Calibration.sanitizeStoredOffset(Float.NEGATIVE_INFINITY), 0f)
        assertEquals(DEFAULT_CALIBRATION_OFFSET_DB, Calibration.sanitizeStoredOffset(DEFAULT_CALIBRATION_OFFSET_DB + 60.5f), 0f)
        assertEquals(DEFAULT_CALIBRATION_OFFSET_DB, Calibration.sanitizeStoredOffset(DEFAULT_CALIBRATION_OFFSET_DB - 60.5f), 0f)
    }

    @Test
    fun `stored offset within range is kept`() {
        assertEquals(97.5f, Calibration.sanitizeStoredOffset(97.5f), 0f)
        assertEquals(DEFAULT_CALIBRATION_OFFSET_DB + 60f, Calibration.sanitizeStoredOffset(DEFAULT_CALIBRATION_OFFSET_DB + 60f), 0f)
        assertEquals(DEFAULT_CALIBRATION_OFFSET_DB - 60f, Calibration.sanitizeStoredOffset(DEFAULT_CALIBRATION_OFFSET_DB - 60f), 0f)
    }

    @Test
    fun `display rounding returns null instead of throwing for non-finite values`() {
        assertEquals(46, 45.6f.roundToIntOrNull())
        assertNull(Float.NaN.roundToIntOrNull())
        assertNull(Float.POSITIVE_INFINITY.roundToIntOrNull())
    }
}
