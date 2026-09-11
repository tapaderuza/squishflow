package com.alvaropassalacqua.squishflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

enum class SquishyState { TENSE, RELAXING, COMPRESSED }

data class TimerUiState(
    val squishyState: SquishyState = SquishyState.TENSE,
    val remainingSeconds: Int = 15 * 60,
    val totalSeconds: Int = 15 * 60,
    val selectedMinutes: Int = 15,
    val completedSessions: Int = 0,
    val lastSessionCompleted: Boolean = false,
    val boostsUsed: Int = 0,
    val focusedSeconds: Int = 0,
    val rescuedSeconds: Int = 0,
    val failedSessions: Int = 0,
    val sessionIntention: String = "Create",
) {
    val isSessionActive: Boolean get() = squishyState == SquishyState.RELAXING
    val progress: Float get() = if (totalSeconds <= 0) 0f else
        (1f - remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
    val level: Int get() = completedSessions / 3 + 1
    val levelProgress: Float get() = (completedSessions % 3) / 3f
}

private val monotonicOrigin = TimeSource.Monotonic.markNow()
private fun monotonicMillis(): Long = monotonicOrigin.elapsedNow().inWholeMilliseconds

@OptIn(ExperimentalTime::class)
private fun wallClockMillis(): Long = Clock.System.now().toEpochMilliseconds()

class TimerViewModel internal constructor(
    private val nowMillis: () -> Long = ::monotonicMillis,
    private val autoTick: Boolean = true,
    /**
     * Wall-clock time, used only to write the deadline down. The countdown itself
     * stays on the monotonic clock so a changed system time cannot shorten or
     * stretch a block; wall-clock is what survives the process.
     */
    private val wallMillis: () -> Long = ::wallClockMillis,
    private val store: SessionStore = SquishySessionStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()
    private var timerJob: Job? = null
    private var deadlineMillis: Long? = null

    fun selectIntention(intention: String) {
        if (!_uiState.value.isSessionActive) _uiState.value = _uiState.value.copy(sessionIntention = intention)
    }

    fun selectDuration(minutes: Int) {
        if (_uiState.value.isSessionActive) return
        require(minutes in 1..180) { "Duration must be between 1 and 180 minutes" }
        val seconds = minutes * 60
        _uiState.value = _uiState.value.copy(
            selectedMinutes = minutes, remainingSeconds = seconds, totalSeconds = seconds,
            squishyState = SquishyState.TENSE, lastSessionCompleted = false,
        )
    }

    fun startSession() {
        val seconds = _uiState.value.selectedMinutes * 60
        beginCountdown(remainingSeconds = seconds, totalSeconds = seconds)
        store.save(SavedSession(deadlineEpochMillis = wallMillis() + seconds * 1_000L, totalSeconds = seconds))
    }

    /**
     * Pick up a block the process was killed in the middle of.
     *
     * If its deadline is still ahead, the countdown resumes with what is left.
     * If it passed while the app was gone, the block is credited as finished:
     * the person did the focusing, and the app being reclaimed by the system
     * is not their failure.
     */
    fun resumeIfSaved() {
        val saved = store.load() ?: return
        if (_uiState.value.isSessionActive) return
        val remainingMillis = saved.deadlineEpochMillis - wallMillis()
        if (remainingMillis > 0) {
            val remaining = ((remainingMillis + 999L) / 1_000L).toInt()
            _uiState.value = _uiState.value.copy(selectedMinutes = (saved.totalSeconds / 60).coerceAtLeast(1))
            beginCountdown(remainingSeconds = remaining, totalSeconds = saved.totalSeconds)
        } else {
            _uiState.value = _uiState.value.copy(
                remainingSeconds = 0, totalSeconds = saved.totalSeconds,
                squishyState = SquishyState.RELAXING,
            )
            completeSession()
        }
    }

    private fun beginCountdown(remainingSeconds: Int, totalSeconds: Int) {
        timerJob?.cancel()
        deadlineMillis = nowMillis() + remainingSeconds * 1_000L
        _uiState.value = _uiState.value.copy(
            squishyState = SquishyState.RELAXING, remainingSeconds = remainingSeconds,
            totalSeconds = totalSeconds, lastSessionCompleted = false, boostsUsed = 0,
        )
        if (autoTick) timerJob = viewModelScope.launch {
            while (_uiState.value.isSessionActive) {
                delay(250)
                updateFromClock()
            }
        }
    }

    /** Uses a monotonic deadline, so delayed frames never make the session run long. */
    internal fun updateFromClock() {
        val current = _uiState.value
        val deadline = deadlineMillis ?: return
        if (!current.isSessionActive) return
        val remaining = ((deadline - nowMillis() + 999L) / 1_000L).coerceAtLeast(0L).toInt()
        if (remaining == 0) completeSession()
        else if (remaining != current.remainingSeconds) _uiState.value = current.copy(remainingSeconds = remaining)
    }

    private fun completeSession() {
        val current = _uiState.value
        timerJob?.cancel()
        timerJob = null
        deadlineMillis = null
        store.clear()
        _uiState.value = current.copy(
            squishyState = SquishyState.TENSE, remainingSeconds = 0,
            completedSessions = current.completedSessions + 1,
            focusedSeconds = current.focusedSeconds + current.totalSeconds, lastSessionCompleted = true,
        )
    }

    fun applyTimeReward(seconds: Int) {
        require(seconds >= 0) { "Reward cannot be negative" }
        val current = _uiState.value
        val deadline = deadlineMillis
        if (!current.isSessionActive || current.boostsUsed >= 1 || deadline == null) return
        val rewarded = minOf(seconds, current.remainingSeconds)
        deadlineMillis = deadline - rewarded * 1_000L
        store.load()?.let { saved ->
            store.save(saved.copy(deadlineEpochMillis = saved.deadlineEpochMillis - rewarded * 1_000L))
        }
        _uiState.value = current.copy(
            boostsUsed = current.boostsUsed + 1, rescuedSeconds = current.rescuedSeconds + rewarded,
        )
        updateFromClock()
    }

    fun failSession() {
        val current = _uiState.value
        if (!current.isSessionActive) return
        timerJob?.cancel()
        timerJob = null
        deadlineMillis = null
        store.clear()
        _uiState.value = current.copy(
            squishyState = SquishyState.COMPRESSED,
            failedSessions = current.failedSessions + 1, lastSessionCompleted = false,
        )
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
