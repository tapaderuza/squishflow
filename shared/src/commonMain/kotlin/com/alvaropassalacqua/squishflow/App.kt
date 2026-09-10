package com.alvaropassalacqua.squishflow

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.kmp.ui.revenuecatui.Paywall
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun App() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ink,
            background = Cream,
            surface = SoftWhite,
        ),
    ) {
        val timerViewModel = viewModel { TimerViewModel() }
        val uiState by timerViewModel.uiState.collectAsStateWithLifecycle()
        val blockedPackage by InterceptEvents.blockedPackage.collectAsStateWithLifecycle()
        val selectionRequest by SelectionEvents.requests.collectAsStateWithLifecycle()
        val lifecycleOwner = LocalLifecycleOwner.current
        var onboardingComplete by remember { mutableStateOf(FocusPreferences.hasCompletedOnboarding()) }
        var protectedAppCount by remember { mutableIntStateOf(FocusPreferences.selectedAppCount()) }
        var protectionEnabled by remember { mutableStateOf(FocusPreferences.isProtectionEnabled()) }
        var skipProtectionSetup by remember { mutableStateOf(false) }
        var showPaywall by remember { mutableStateOf(false) }
        var showPremiumIntro by remember { mutableStateOf(false) }
        var showRescueMode by remember { mutableStateOf(false) }
        var showJourney by remember { mutableStateOf(false) }
        var conversionPromptShown by remember { mutableStateOf(false) }
        val restoredMission = remember { deserializeMission(MissionPersistence.load()) }
        var mission by remember { mutableStateOf(restoredMission?.mission) }
        var missionAccepted by remember { mutableStateOf(restoredMission?.accepted == true) }
        var activeBlockIndex by remember { mutableIntStateOf(restoredMission?.index ?: 0) }
        var showReflection by remember { mutableStateOf(false) }
        var handledCompletions by remember { mutableIntStateOf(0) }

        LaunchedEffect(Unit) {
            if (RevenueCatManager.configure()) RevenueCatManager.refreshEntitlement()
        }

        LaunchedEffect(mission, missionAccepted, activeBlockIndex) {
            mission?.let { MissionPersistence.save(it.serialize(activeBlockIndex, missionAccepted)) }
                ?: MissionPersistence.clear()
        }

        LaunchedEffect(selectionRequest) {
            if (selectionRequest > 0 && blockedPackage == null) onboardingComplete = false
        }

        LaunchedEffect(uiState.completedSessions) {
            if (uiState.completedSessions > handledCompletions && missionAccepted) {
                handledCompletions = uiState.completedSessions
                showReflection = true
            }
            if (uiState.completedSessions == 1 && !conversionPromptShown) {
                conversionPromptShown = true
                delay(1_400)
                showPremiumIntro = true
            }
        }

        DisposableEffect(lifecycleOwner, timerViewModel) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    protectionEnabled = FocusPreferences.isProtectionEnabled()
                }
                // ON_PAUSE also fires for system dialogs; ON_STOP means the app is actually hidden.
                // A system interruption is not evidence of user failure. Explicit abandonment
                // and protected-app interception remain the only failure signals in this MVP.
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        when {
            blockedPackage != null -> FocusInterventionScreen(
                packageName = blockedPackage!!,
                onReturn = InterceptEvents::dismiss,
                onOpenBriefly = {
                    BlockedAppController.openForOneMinute(blockedPackage!!)
                    InterceptEvents.dismiss()
                },
            )
            !onboardingComplete -> DistractionPicker { selected ->
                FocusPreferences.completeOnboarding(selected)
                protectedAppCount = selected.size
                protectionEnabled = FocusPreferences.isProtectionEnabled()
                skipProtectionSetup = false
                onboardingComplete = true
            }
            !protectionEnabled && !skipProtectionSetup -> ProtectionSetupScreen(
                onEnable = FocusPreferences::openProtectionSettings,
                onSkip = { skipProtectionSetup = true },
            )
            mission == null -> MissionPlannerScreen { planned ->
                mission = planned
                missionAccepted = false
                activeBlockIndex = 0
            }
            !missionAccepted -> MissionReviewScreen(
                mission = mission!!,
                onBack = { mission = null },
                onDurationChanged = { index, minutes ->
                    mission = mission!!.copy(blocks = mission!!.blocks.mapIndexed { i, block ->
                        if (i == index) block.copy(minutes = minutes) else block
                    })
                },
                onStart = {
                    val first = mission!!.blocks.indexOfFirst { it.kind == BlockKind.FOCUS }.coerceAtLeast(0)
                    activeBlockIndex = first
                    timerViewModel.selectDuration(mission!!.blocks[first].minutes)
                    missionAccepted = true
                },
            )
            mission!!.blocks.getOrNull(activeBlockIndex)?.kind == BlockKind.BREAK -> MissionBreakScreen(
                block = mission!!.blocks[activeBlockIndex],
                nextBlock = mission!!.blocks.getOrNull(activeBlockIndex + 1),
                onContinue = {
                    val next = activeBlockIndex + 1
                    if (next > mission!!.blocks.lastIndex) {
                        mission = null
                        missionAccepted = false
                    } else {
                        activeBlockIndex = next
                        mission!!.blocks[next].takeIf { it.kind == BlockKind.FOCUS }
                            ?.let { timerViewModel.selectDuration(it.minutes) }
                    }
                },
                onFinish = { mission = null; missionAccepted = false },
            )
            showJourney -> JourneyScreen(
                state = uiState,
                onBack = { showJourney = false },
                onPremium = { showJourney = false; showPremiumIntro = true },
            )
            showRescueMode -> RescueModeScreen(
                onDismiss = { showRescueMode = false },
                onReward = { seconds ->
                    timerViewModel.applyTimeReward(seconds)
                    showRescueMode = false
                },
            )
            showReflection -> SessionReflectionScreen(
                completedBlock = mission!!.blocks.getOrNull(activeBlockIndex)
                    ?: MissionBlock("Focus session", uiState.selectedMinutes),
                onRated = { feeling ->
                    val next = (activeBlockIndex + 1).takeIf { it <= mission!!.blocks.lastIndex }
                    showReflection = false
                    if (next == null) {
                        mission = null
                        missionAccepted = false
                    } else {
                        activeBlockIndex = next
                        val nextBlock = mission!!.blocks[next]
                        if (nextBlock.kind == BlockKind.FOCUS) {
                            timerViewModel.selectDuration(AdaptiveFocusCoach.nextMinutes(nextBlock.minutes, feeling))
                        }
                    }
                },
                onFinish = {
                    showReflection = false
                    mission = null
                    missionAccepted = false
                },
            )
            showPaywall -> {
                val options = remember {
                    PaywallOptions(dismissRequest = { showPaywall = false }) {
                        shouldDisplayDismissButton = true
                    }
                }
                Paywall(options)
            }
            showPremiumIntro -> PremiumIntroScreen(
                onDismiss = { showPremiumIntro = false },
                onSeePlans = {
                    showPremiumIntro = false
                    showPaywall = true
                },
            )
            else -> FocusScreen(
                uiState = uiState,
                missionBlock = mission!!.blocks.getOrNull(activeBlockIndex),
                onDurationSelected = timerViewModel::selectDuration,
                onPrimaryAction = {
                    if (uiState.isSessionActive) timerViewModel.failSession()
                    else timerViewModel.startSession()
                },
                onPremium = { showPremiumIntro = true },
                onRescue = { showRescueMode = true },
                onJourney = { showJourney = true },
                protectedAppCount = protectedAppCount,
                onManageApps = {
                    FocusPreferences.resetOnboarding()
                    onboardingComplete = false
                },
            )
        }
    }
}

@Composable
private fun ProtectionSetupScreen(
    onEnable: () -> Unit,
    onSkip: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = Cream) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.8f))
            Box(
                Modifier.size(96.dp).clip(CircleShape).background(Sage.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("S", color = Sage, fontSize = 34.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(30.dp))
            Text("Turn on your pause.", color = Ink, fontSize = 31.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(12.dp))
            Text(
                "Android needs your permission once to detect the apps you picked. Squishflow never reads what you do inside them.",
                color = Muted,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onEnable,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(29.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color(0xFF151713)),
            ) {
                Text("Enable protection on Android", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onSkip) {
                Text("Not now · timer only", color = Muted, fontSize = 12.sp)
            }
        }
    }
}
@Composable
private fun FocusInterventionScreen(
    packageName: String,
    onReturn: () -> Unit,
    onOpenBriefly: () -> Unit,
) {
    var squishes by remember(packageName) { mutableIntStateOf(0) }
    Surface(Modifier.fillMaxSize(), color = Cream) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(18.dp))
            Text("CONSCIOUS PAUSE", color = Sage, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            Spacer(Modifier.weight(0.45f))
            Text("Did you open it on purpose?", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(9.dp))
            Text(
                "Squeeze Squishy three times to continue.",
                color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            SquishyStage(
                state = SquishyState.TENSE,
                progress = squishes / 3f,
                accent = Sage,
                onSquish = { squishes = (squishes + 1).coerceAtMost(3) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(3) { index ->
                    Box(
                        Modifier.size(if (index < squishes) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (index < squishes) Sage else Ink.copy(alpha = 0.14f)),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onReturn,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color(0xFF151713)),
            ) { Text("Back to what matters", fontWeight = FontWeight.Bold) }
            TextButton(onClick = onOpenBriefly, enabled = squishes >= 3) {
                Text(
                    if (squishes < 3) "Breathe and squeeze · ${3 - squishes}" else "Open it for 1 minute",
                    color = if (squishes >= 3) Muted else Muted.copy(alpha = 0.42f),
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.weight(0.35f))
            Text(packageName, color = Muted.copy(alpha = 0.35f), fontSize = 9.sp)
        }
    }
}
@Composable
private fun FocusScreen(
    uiState: TimerUiState,
    missionBlock: MissionBlock?,
    onDurationSelected: (Int) -> Unit,
    onPrimaryAction: () -> Unit,
    onPremium: () -> Unit,
    onRescue: () -> Unit,
    onJourney: () -> Unit,
    protectedAppCount: Int,
    onManageApps: () -> Unit,
) {
    var tensionReleased by remember { mutableIntStateOf(0) }
    val accent by animateColorAsState(
        targetValue = when (uiState.squishyState) {
            SquishyState.TENSE -> Coral
            SquishyState.RELAXING -> Sage
            SquishyState.COMPRESSED -> Lavender
        },
    )

    Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header(
                completedSessions = uiState.completedSessions,
                onPremium = onPremium,
                onJourney = onJourney,
                protectedAppCount = protectedAppCount,
                onManageApps = onManageApps,
            )

            if (missionBlock != null) {
                Text(
                    "MISSION · ${missionBlock.title}",
                    color = Ink.copy(alpha = .48f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    maxLines = 1,
                )
            }

            Spacer(Modifier.weight(0.45f))

            Text(
                text = when {
                    uiState.lastSessionCompleted -> "You came back. That counts."
                    uiState.isSessionActive -> "Just this. Just now."
                    uiState.squishyState == SquishyState.COMPRESSED -> "No guilt. Come back whenever."
                    else -> "Tap. Breathe. Begin."
                },
                color = Muted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(18.dp))

            SquishyStage(
                state = uiState.squishyState,
                progress = uiState.progress,
                accent = accent,
                onSquish = {
                    if (!uiState.isSessionActive) {
                        tensionReleased = (tensionReleased + 1).coerceAtMost(3)
                    }
                },
            )

            Spacer(Modifier.height(14.dp))


            Text(
                text = formatTime(uiState.remainingSeconds),
                color = Ink,
                fontSize = 60.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp,
            )

            Text(
                text = when (uiState.squishyState) {
                    SquishyState.TENSE -> "READY"
                    SquishyState.RELAXING -> "IN FOCUS"
                    SquishyState.COMPRESSED -> "INTERRUPTED"
                },
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )

            Spacer(Modifier.height(26.dp))

            if (!uiState.isSessionActive) {
                DurationPicker(
                    selected = uiState.selectedMinutes,
                    onSelected = onDurationSelected,
                )
                Spacer(Modifier.height(20.dp))
            } else {
                if (uiState.boostsUsed == 0) {
                    TextButton(onClick = onRescue) {
                        Text(
                            "I need a boost  ->",
                            color = Ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    Text(
                        "Boost used · protect the time that is left",
                        color = Muted,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            PrimaryAction(
                active = uiState.isSessionActive,
                onClick = onPrimaryAction,
            )

            Spacer(Modifier.weight(0.6f))

            Text(
                if (tensionReleased < 3 && !uiState.isSessionActive)
                    "Squeeze or stretch Squishy · ${3 - tensionReleased} to go"
                else
                    "Squishy is ready to protect your attention",
                color = Muted.copy(alpha = 0.75f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 18.dp),
            )
        }
    }
}

@Composable
private fun Header(
    completedSessions: Int,
    onPremium: () -> Unit,
    onJourney: () -> Unit,
    protectedAppCount: Int,
    onManageApps: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "SQUISHFLOW",
                color = Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.2.sp,
            )
            Text(
                "YOUR FOCUS COMPANION",
                color = Muted.copy(alpha = 0.72f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.7.sp,
            )
        }
        TextButton(
            onClick = onManageApps,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.textButtonColors(containerColor = SoftWhite, contentColor = Muted),
        ) {
            Text("$protectedAppCount APP", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        }
        Spacer(Modifier.width(7.dp))

        TextButton(
            onClick = onJourney,
            modifier = Modifier.size(42.dp),
            contentPadding = PaddingValues(0.dp),
            shape = CircleShape,
            colors = ButtonDefaults.textButtonColors(
                containerColor = SoftWhite,
                contentColor = Ink,
            ),
        ) {
            Text(
                completedSessions.toString().padStart(2, '0'),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(8.dp))
        TextButton(
            onClick = onPremium,
            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.textButtonColors(
                containerColor = Color(0xFFFFD86B),
                contentColor = Color(0xFF151713),
            ),
        ) {
            Text("PRO", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}
@Composable
private fun ReleaseMeter(released: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            if (released >= 3) "TENSION RELEASED" else "RELEASE TENSION",
            color = if (released >= 3) Sage else Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
        )
        repeat(3) { index ->
            Box(
                Modifier
                    .size(if (index < released) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (index < released) Sage else Ink.copy(alpha = 0.12f)),
            )
        }
    }
}

private data class RescueAction(
    val title: String,
    val instruction: String,
    val rewardSeconds: Int,
    val preparationSeconds: Int,
)

@Composable
private fun RescueModeScreen(
    onDismiss: () -> Unit,
    onReward: (Int) -> Unit,
) {
    val actions = remember {
        listOf(
            RescueAction("1 burpee", "Verified by camera.", 5 * 60, 0),
            RescueAction("20 jumping jacks", "Move your whole body and change your state.", 5 * 60, 15),
            RescueAction("Guided breathing", "Breathe in for 4. Out for 6. Repeat.", 3 * 60, 30),
            RescueAction("10 squishes", "Put the restlessness straight into Squishy.", 60, 8),
        )
    }
    var selected by remember { mutableStateOf<RescueAction?>(null) }
    var countdown by remember { mutableIntStateOf(0) }
    var cameraConsent by remember { mutableStateOf(false) }

    if (selected?.title == "1 burpee" && cameraConsent) {
        BurpeeCameraVerifier(
            onVerified = { onReward(5 * 60) },
            onCancel = { cameraConsent = false },
        )
        return
    }

    LaunchedEffect(selected) {
        val action = selected ?: return@LaunchedEffect
        countdown = action.preparationSeconds
        while (countdown > 0) {
            delay(1_000)
            countdown -= 1
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = {
                    if (selected == null) onDismiss() else {
                        selected = null
                        cameraConsent = false
                    }
                }) { Text(if (selected == null) "Close" else "Back", color = Muted) }
                Text("RESCUE MODE", color = Coral, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                Spacer(Modifier.width(64.dp))
            }

            Spacer(Modifier.weight(0.55f))

            if (selected == null) {
                Text("Change your state.", color = Ink, fontSize = 34.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.height(10.dp))
                Text(
                    "One real action buys back focus time.\nPick just one.",
                    color = Muted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                )
                Spacer(Modifier.height(32.dp))
                actions.forEach { action ->
                    OutlinedButton(
                        onClick = { selected = action },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).height(68.dp),
                        shape = RoundedCornerShape(22.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Ink.copy(alpha = 0.1f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = SoftWhite, contentColor = Ink),
                    ) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                            Text(action.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (action.rewardSeconds == 60) "-1 minute" else "-${action.rewardSeconds / 60} minutes",
                                color = Muted,
                                fontSize = 11.sp,
                            )
                        }
                        Text("->", color = Coral, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                val action = selected!!
                if (action.title == "1 burpee") {
                    Text("Camera verification", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Light)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "We read your pose live to recognise a full burpee: standing, floor, standing.",
                        color = Muted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp,
                    )
                    Spacer(Modifier.height(28.dp))
                    Surface(color = Sage.copy(alpha = 0.12f), shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text("PRIVACY", color = Sage, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Everything is analysed on this device. No image is recorded, stored or sent.",
                                color = Ink,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { cameraConsent = true },
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(29.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Ink),
                    ) { Text("Got it — turn on camera", fontWeight = FontWeight.Bold) }
                } else {
                    Text(
                        if (countdown > 0) countdown.toString() else "READY",
                        color = if (countdown > 0) Ink else Sage,
                        fontSize = if (countdown > 0) 88.sp else 42.sp,
                        fontWeight = FontWeight.Light,
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(action.title, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Text(action.instruction, color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
                    Spacer(Modifier.height(34.dp))
                    Button(
                        onClick = { onReward(action.rewardSeconds) },
                        enabled = countdown == 0,
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(29.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Ink),
                    ) {
                        Text(if (countdown > 0) "Do it now" else "Done — claim the time", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Text(
                "One rescue per session. No guilt, and no infinite escape hatch.",
                color = Muted.copy(alpha = 0.65f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PremiumIntroScreen(
    onDismiss: () -> Unit,
    onSeePlans: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF090B0A)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = Color.White.copy(alpha = 0.72f))
                }
            }
            Spacer(Modifier.weight(0.4f))
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFD86B)),
                contentAlignment = Alignment.Center,
            ) {
                Text("PRO", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "Focus should not take force.",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Light,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "A sensory reset, so you return before you drift.",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(38.dp))
            PremiumBenefit("01", "Smarter rescues", "Turn movement and breath into time you get back.")
            PremiumBenefit("02", "Collectable squishies", "New materials, colours and personalities.")
            PremiumBenefit("03", "Your attention map", "Learn which duration and which rescue actually work for you.")
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onSeePlans,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(29.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD86B),
                    contentColor = Color(0xFF151713),
                ),
            ) {
                Text("See Premium plans", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Cancel anytime",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
            )
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun PremiumBenefit(number: String, title: String, body: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(number, color = Color(0xFFFFD86B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(18.dp))
        Column {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(body, color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun DurationPicker(
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Ink.copy(alpha = 0.055f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf(5, 15, 25, 45).forEach { minutes ->
            val active = selected == minutes
            TextButton(
                onClick = { onSelected(minutes) },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (active) SoftWhite else Color.Transparent,
                    contentColor = if (active) Ink else Muted,
                ),
                contentPadding = PaddingValues(horizontal = 17.dp, vertical = 9.dp),
            ) {
                Text(
                    "$minutes min",
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun PrimaryAction(
    active: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.width(190.dp).height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) Color.Transparent else Ink,
            contentColor = if (active) Coral else Color.White,
        ),
        border = if (active) androidx.compose.foundation.BorderStroke(1.dp, Coral.copy(alpha = 0.5f)) else null,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Text(
            if (active) "End session" else "Start focus",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun formatTime(totalSeconds: Int): String =
    (totalSeconds / 60).toString().padStart(2, '0') + ":" +
        (totalSeconds % 60).toString().padStart(2, '0')
