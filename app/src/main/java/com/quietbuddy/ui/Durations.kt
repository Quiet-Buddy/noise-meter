package com.quietbuddy.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quietbuddy.R

/** A number of minutes as "5 h 12 min" (or "12 min" under an hour), with the units in the app's language. */
@Composable
fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) stringResource(R.string.duration_hours_minutes, h, m) else stringResource(R.string.duration_minutes, m)
}
