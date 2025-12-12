package com.receipt2report.offline.util

import java.util.Locale

fun money(n: Double): String = "$" + String.format(Locale.US, "%.2f", n)
