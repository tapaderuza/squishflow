package com.alvaropassalacqua.squishflow

import com.revenuecat.purchases.kmp.models.PackageType

/**
 * What one plan row says.
 *
 * Pure functions over the store's own numbers, so the copy on the plans screen
 * is held to the same tests as every other sentence in the product.
 */
data class PlanLine(val title: String, val note: String)

/** The row for a package: what it is called and the one thing worth knowing about it. */
internal fun planLine(type: PackageType, formattedPrice: String, amountMicros: Long): PlanLine = when (type) {
    PackageType.LIFETIME -> PlanLine("Lifetime", "Once. Yours for good.")
    PackageType.ANNUAL -> PlanLine("Yearly", "${monthlyEquivalent(formattedPrice, amountMicros, 12)} a month, billed once")
    PackageType.SIX_MONTH -> PlanLine("Six months", "${monthlyEquivalent(formattedPrice, amountMicros, 6)} a month")
    PackageType.THREE_MONTH -> PlanLine("Three months", "${monthlyEquivalent(formattedPrice, amountMicros, 3)} a month")
    PackageType.TWO_MONTH -> PlanLine("Two months", "${monthlyEquivalent(formattedPrice, amountMicros, 2)} a month")
    PackageType.MONTHLY -> PlanLine("Monthly", "Cancel any time.")
    PackageType.WEEKLY -> PlanLine("Weekly", "Cancel any time.")
    PackageType.UNKNOWN, PackageType.CUSTOM -> PlanLine("Pro", "")
}

/**
 * A per-month price in the same currency dress as [formatted].
 *
 * The store hands back a localised string and a micro-amount, not a currency
 * formatter, so the symbol and its side are lifted from the string it gave us:
 * "$79.98" divides to "$6.67", "79,98 €" to "6,67 €".
 */
internal fun monthlyEquivalent(formatted: String, amountMicros: Long, months: Int): String {
    require(months > 0)
    val prefix = formatted.takeWhile { !it.isDigit() }
    val suffix = formatted.takeLastWhile { !it.isDigit() }
    val separator = if (',' in formatted && '.' !in formatted) ',' else '.'

    val cents = (amountMicros / months + 5_000) / 10_000
    val whole = cents / 100
    val fraction = (cents % 100).toString().padStart(2, '0')
    return "$prefix$whole$separator$fraction$suffix"
}

/** Which row to preselect: the yearly plan if there is one, else the first. */
internal fun defaultPlanIndex(types: List<PackageType>): Int =
    types.indexOf(PackageType.ANNUAL).takeIf { it >= 0 } ?: 0
