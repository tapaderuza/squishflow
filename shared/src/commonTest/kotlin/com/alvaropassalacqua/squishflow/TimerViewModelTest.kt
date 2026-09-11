package com.alvaropassalacqua.squishflow

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

/** A store that remembers, so the lifecycle across a "process death" can be asserted. */
private class MemoryStore : SessionStore {
    var saved: SavedSession? = null
    override fun save(session: SavedSession) { saved = session }
    override fun load(): SavedSession? = saved
    override fun clear() { saved = null }
}

class SessionPersistenceTest {

    @Test fun startingABlockWritesItsDeadlineDown() {
        val store = MemoryStore()
        val vm = TimerViewModel(nowMillis = { 0L }, autoTick = false, wallMillis = { 1_000_000L }, store = store)
        vm.selectDuration(15)
        vm.startSession()

        assertEquals(SavedSession(deadlineEpochMillis = 1_000_000L + 15 * 60_000L, totalSeconds = 900), store.saved)
    }

    @Test fun finishingOrAbandoningForgetsIt() {
        val store = MemoryStore()
        var now = 0L
        val vm = TimerViewModel(nowMillis = { now }, autoTick = false, wallMillis = { 0L }, store = store)
        vm.selectDuration(1)
        vm.startSession()
        now = 60_000L
        vm.updateFromClock()
        assertEquals(null, store.saved, "A finished block must not be resumed on the next launch")

        vm.selectDuration(1)
        vm.startSession()
        vm.failSession()
        assertEquals(null, store.saved, "Nor an abandoned one")
    }

    @Test fun aBlockStillRunningIsResumedWithWhatIsLeft() {
        // The process died with 15 minutes on the clock; 5 have passed since.
        val store = MemoryStore().apply {
            saved = SavedSession(deadlineEpochMillis = 900_000L, totalSeconds = 900)
        }
        val vm = TimerViewModel(nowMillis = { 0L }, autoTick = false, wallMillis = { 300_000L }, store = store)

        vm.resumeIfSaved()

        val state = vm.uiState.value
        assertTrue(state.isSessionActive, "Should be counting down again")
        assertEquals(600, state.remainingSeconds)
        assertEquals(900, state.totalSeconds, "Progress must resume against the original length")
    }

    @Test fun aBlockThatEndedWhileAwayIsCreditedNotLost() {
        // Nothing the person did ended this block; the system did. They focused.
        val store = MemoryStore().apply {
            saved = SavedSession(deadlineEpochMillis = 900_000L, totalSeconds = 900)
        }
        val vm = TimerViewModel(nowMillis = { 0L }, autoTick = false, wallMillis = { 2_000_000L }, store = store)

        vm.resumeIfSaved()

        val state = vm.uiState.value
        assertFalse(state.isSessionActive)
        assertTrue(state.lastSessionCompleted)
        assertEquals(1, state.completedSessions)
        assertEquals(900, state.focusedSeconds, "The full block is banked")
        assertEquals(null, store.saved, "and it is not credited twice on the next launch")
    }

    @Test fun nothingSavedMeansNothingHappens() {
        val vm = TimerViewModel(nowMillis = { 0L }, autoTick = false, store = MemoryStore())
        vm.resumeIfSaved()
        assertFalse(vm.uiState.value.isSessionActive)
        assertEquals(0, vm.uiState.value.completedSessions)
    }

    @Test fun aRescueMovesTheWrittenDeadlineToo() {
        val store = MemoryStore()
        val vm = TimerViewModel(nowMillis = { 0L }, autoTick = false, wallMillis = { 0L }, store = store)
        vm.selectDuration(10)
        vm.startSession()
        vm.applyTimeReward(120)

        assertEquals(600_000L - 120_000L, store.saved?.deadlineEpochMillis,
            "Otherwise a resumed block would forget the minutes a rescue bought")
    }
}
