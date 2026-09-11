package com.alvaropassalacqua.squishflow

/**
 * Where a running focus block is remembered across process death.
 *
 * The timer counts down on a monotonic clock, which is the right clock for a
 * countdown — it cannot be dragged by the user changing the time — but it does
 * not survive the process. A focus app whose whole proposition is that you put
 * the phone down cannot lose the block the moment the system reclaims memory.
 * So the deadline is also written down in wall-clock time, and on the next
 * launch the block is either resumed with what is left or credited as finished.
 */
interface SessionStore {
    fun save(session: SavedSession)
    fun load(): SavedSession?
    fun clear()
}

/**
 * @property deadlineEpochMillis When the block ends, in wall-clock time.
 * @property totalSeconds The block's full length, so progress resumes correctly.
 */
data class SavedSession(val deadlineEpochMillis: Long, val totalSeconds: Int)

/** The production store, backed by the same preferences as everything else. */
object SquishySessionStore : SessionStore {
    override fun save(session: SavedSession) =
        SquishySettings.saveSession(session.deadlineEpochMillis, session.totalSeconds)

    override fun load(): SavedSession? {
        val (deadline, total) = SquishySettings.loadSession() ?: return null
        return SavedSession(deadline, total)
    }

    override fun clear() = SquishySettings.clearSession()
}

/** Keeps nothing. For tests, and for callers that must not persist. */
object NoSessionStore : SessionStore {
    override fun save(session: SavedSession) = Unit
    override fun load(): SavedSession? = null
    override fun clear() = Unit
}
