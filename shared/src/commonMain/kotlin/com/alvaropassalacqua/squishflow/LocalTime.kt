package com.alvaropassalacqua.squishflow

/** The hour of the day where the phone is, 0–23. */
expect fun localHourOfDay(): Int

/**
 * Whether it is late enough that the body should look like it.
 *
 * Between eleven at night and five in the morning the eyes sit heavier and
 * the idle line says so. Not a lecture about sleep: one sentence, and the
 * block still starts if that is what is wanted.
 */
fun isLateNight(hour: Int): Boolean = hour >= 23 || hour < 5
