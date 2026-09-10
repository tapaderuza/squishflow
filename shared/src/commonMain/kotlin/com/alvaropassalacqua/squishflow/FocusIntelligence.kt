package com.alvaropassalacqua.squishflow

data class MissionBlock(
    val title: String,
    val minutes: Int,
    val kind: BlockKind = BlockKind.FOCUS,
)

enum class BlockKind { FOCUS, BREAK }

data class FocusMission(
    val title: String,
    val originalGoal: String,
    val blocks: List<MissionBlock>,
)

sealed interface CoachRecommendation {
    data class ShorterSessions(val minutes: Int = 15) : CoachRecommendation
    data class DeepFocus(val minutes: Int = 45) : CoachRecommendation
    data object KeepRhythm : CoachRecommendation
}

enum class SessionFeeling { EASY, RIGHT, TOO_MUCH }

/**
 * Provider boundary for a server-backed model. API keys must live on that server, never here.
 * The local implementation keeps planning available offline and makes the MVP testable.
 */
interface MissionPlanner {
    suspend fun plan(goal: String, preferredMinutes: Int = 25): FocusMission
}

object AdaptiveFocusCoach {
    fun recommend(state: TimerUiState): CoachRecommendation = when {
        state.failedSessions >= 2 && state.completedSessions == 0 -> CoachRecommendation.ShorterSessions()
        state.focusedSeconds >= 90 * 60 -> CoachRecommendation.DeepFocus()
        else -> CoachRecommendation.KeepRhythm
    }

    fun nextMinutes(currentMinutes: Int, feeling: SessionFeeling): Int = when (feeling) {
        SessionFeeling.EASY -> currentMinutes + 5
        SessionFeeling.RIGHT -> currentMinutes
        SessionFeeling.TOO_MUCH -> currentMinutes - 5
    }.coerceIn(10, 60)
}
