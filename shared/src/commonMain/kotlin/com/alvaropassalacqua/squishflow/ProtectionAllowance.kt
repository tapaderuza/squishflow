package com.alvaropassalacqua.squishflow

/**
 * How many apps a person may put behind Squishy without paying.
 *
 * The bodies are the joy half of the model and are all earnable; this is the
 * utility half. Three is enough to prove the mechanic on the apps that
 * actually steal an afternoon, and somebody with six is somebody the app is
 * working for. Lifting the limit is the one Pro benefit that is not cosmetic,
 * and the pitch says so in plain numbers.
 *
 * Apps already protected are never un-protected when Pro lapses: the same
 * rule as earned bodies. The limit only decides whether one more can be added.
 */
const val FREE_PROTECTED_APPS = 3

/** Maximum protected apps for this person, or null for no limit. */
fun protectedAppAllowance(isPremium: Boolean): Int? = if (isPremium) null else FREE_PROTECTED_APPS

/** Whether one more app can be added to a selection of [selectedCount]. */
fun canProtectAnother(selectedCount: Int, isPremium: Boolean): Boolean =
    protectedAppAllowance(isPremium)?.let { selectedCount < it } ?: true
