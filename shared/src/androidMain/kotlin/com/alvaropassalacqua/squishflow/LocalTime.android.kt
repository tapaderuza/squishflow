package com.alvaropassalacqua.squishflow

import java.util.Calendar

actual fun localHourOfDay(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
