package com.example.squishfocus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TimerViewModelTest {
    @Test fun countdownUsesElapsedTimeInsteadOfTickCount() {
        var now = 1_000L
        val viewModel = TimerViewModel(nowMillis = { now }, autoTick = false)
        viewModel.selectDuration(1)
        viewModel.startSession()
        now += 30_400L
        viewModel.updateFromClock()
        assertEquals(30, viewModel.uiState.value.remainingSeconds)
        assertEquals(SquishyState.RELAXING, viewModel.uiState.value.squishyState)
    }

    @Test fun reachingDeadlineCompletesExactlyOnce() {
        var now = 0L
        val viewModel = TimerViewModel(nowMillis = { now }, autoTick = false)
        viewModel.selectDuration(1)
        viewModel.startSession()
        now = 60_000L
        viewModel.updateFromClock()
        viewModel.updateFromClock()
        val state = viewModel.uiState.value
        assertFalse(state.isSessionActive)
        assertTrue(state.lastSessionCompleted)
        assertEquals(1, state.completedSessions)
        assertEquals(60, state.focusedSeconds)
    }

    @Test fun backgroundFailureIsIdempotent() {
        val viewModel = TimerViewModel(nowMillis = { 0L }, autoTick = false)
        viewModel.startSession()
        viewModel.failSession()
        viewModel.failSession()
        assertEquals(SquishyState.COMPRESSED, viewModel.uiState.value.squishyState)
        assertEquals(1, viewModel.uiState.value.failedSessions)
    }

    @Test fun rejectsUnsafeDuration() {
        val viewModel = TimerViewModel(nowMillis = { 0L }, autoTick = false)
        assertFailsWith<IllegalArgumentException> { viewModel.selectDuration(0) }
        assertFailsWith<IllegalArgumentException> { viewModel.selectDuration(181) }
    }
}
