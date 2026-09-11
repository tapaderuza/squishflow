package com.alvaropassalacqua.squishflow

/**
 * The few places the app sends people outside itself.
 *
 * The policy pages are served from the repository (`docs/`, GitHub Pages), so
 * the text a store reviewer reads is the text under version control. The
 * subscription page is the store's own: cancelling happens there, and the app
 * says so rather than pretending to offer it.
 */
object Links {
    const val SITE = "https://tapaderuza.github.io/squishflow"
    const val PRIVACY = "$SITE/privacy.html"
    const val TERMS = "$SITE/terms.html"
    const val SUPPORT = "$SITE/support.html"
}

/** Open [url] in the system browser (or the store app, for store URLs). */
expect fun openUrl(url: String)

/** Where this platform lets a person manage or cancel a subscription. */
expect val manageSubscriptionUrl: String
