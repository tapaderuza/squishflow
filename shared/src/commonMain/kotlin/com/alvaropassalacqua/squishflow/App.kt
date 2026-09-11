package com.alvaropassalacqua.squishflow

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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


/**
 * How many blocks a person finishes before Squishflow ever mentions money.
 *
 * PRODUCT_DECISIONS.md: never before first value. Asking after a single block
 * interrupts the exact moment the product is trying to prove itself.
 */
private const val PAYWALL_AFTER_SESSIONS = 3

/** How long a locked material stays in the hand before the paywall appears. */
private const val TRIAL_MILLIS = 6_000L

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
        val reducedMotion = rememberReducedMotion()
        var welcomeSeen by remember { mutableStateOf(SquishySettings.hasSeenWelcome()) }
        var onboardingComplete by remember { mutableStateOf(FocusPreferences.hasCompletedOnboarding()) }
        var protectedAppCount by remember { mutableIntStateOf(FocusPreferences.selectedAppCount()) }
        var protectionEnabled by remember { mutableStateOf(FocusPreferences.isProtectionEnabled()) }
        var skipProtectionSetup by remember { mutableStateOf(SquishySettings.hasDeclinedProtection()) }
        var showPaywall by remember { mutableStateOf(false) }
        var showPremiumIntro by remember { mutableStateOf(false) }
        var showRescueMode by remember { mutableStateOf(false) }
        var showJourney by remember { mutableStateOf(false) }
        var trialMaterial by remember { mutableStateOf<SquishyMaterial?>(null) }
        var justUnlocked by remember { mutableStateOf<SquishyMaterial?>(null) }
        var conversionPromptShown by remember { mutableStateOf(SquishySettings.hasSeenConversionPrompt()) }
        val isPremium by RevenueCatManager.isPremium.collectAsStateWithLifecycle()
        var lifetime by remember { mutableStateOf(LifetimeStats.load()) }
        var material by remember { mutableStateOf(loadSquishyMaterial(isPremium = false)) }
        val restoredMission = remember { deserializeMission(MissionPersistence.load()) }
        var mission by remember { mutableStateOf(restoredMission?.mission) }
        var missionAccepted by remember { mutableStateOf(restoredMission?.accepted == true) }
        var activeBlockIndex by remember { mutableIntStateOf(restoredMission?.index ?: 0) }
        var showReflection by remember { mutableStateOf(false) }
        var handledCompletions by remember { mutableIntStateOf(0) }

        LaunchedEffect(Unit) {
            // Before anything else: a block the system killed us in the middle of
            // is either still running or already earned.
            timerViewModel.resumeIfSaved()
            if (RevenueCatManager.configure()) RevenueCatManager.refreshEntitlement()
        }

        LaunchedEffect(isPremium) {
            material = loadSquishyMaterial(isPremium)
        }

        LaunchedEffect(trialMaterial) {
            if (trialMaterial == null) return@LaunchedEffect
            delay(TRIAL_MILLIS)
            // Hand the body back before asking for money, so the upgrade screen
            // opens over the free material rather than over one they cannot keep.
            trialMaterial = null
            showPremiumIntro = true
        }

        LaunchedEffect(mission, missionAccepted, activeBlockIndex) {
            mission?.let { MissionPersistence.save(it.serialize(activeBlockIndex, missionAccepted)) }
                ?: MissionPersistence.clear()
        }

        LaunchedEffect(selectionRequest) {
            if (selectionRequest > 0 && blockedPackage == null) onboardingComplete = false
        }

        LaunchedEffect(uiState.completedSessions) {
            if (uiState.completedSessions > handledCompletions) {
                // Bank the block that just finished, so the earn ladder survives
                // process death rather than resetting with the in-memory timer.
                val before = lifetime.focusedMinutes
                SquishySettings.addFocusedMinutes(uiState.totalSeconds / 60)
                SquishySettings.recordBlock(completed = true)
                lifetime = LifetimeStats.load()
                justUnlocked = materialUnlockedBetween(before, lifetime.focusedMinutes)
            }
            if (uiState.completedSessions > handledCompletions && missionAccepted) {
                handledCompletions = uiState.completedSessions
                showReflection = true
            }
            if (uiState.completedSessions >= PAYWALL_AFTER_SESSIONS && !conversionPromptShown) {
                conversionPromptShown = true
                SquishySettings.markConversionPromptSeen()
                delay(1_400)
                showPremiumIntro = true
            }
        }

        var handledFailures by remember { mutableIntStateOf(0) }
        LaunchedEffect(uiState.failedSessions) {
            if (uiState.failedSessions > handledFailures) {
                handledFailures = uiState.failedSessions
                SquishySettings.recordBlock(completed = false)
                lifetime = LifetimeStats.load()
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

        val destination = when {
            blockedPackage != null -> Destination.Intervention
            !welcomeSeen -> Destination.Welcome
            !onboardingComplete -> Destination.ChooseDistractions
            !protectionEnabled && !skipProtectionSetup -> Destination.ProtectionSetup
            mission == null -> Destination.Planner
            !missionAccepted -> Destination.MissionReview
            mission!!.blocks.getOrNull(activeBlockIndex)?.kind == BlockKind.BREAK -> Destination.Break
            showJourney -> Destination.Journey
            showRescueMode -> Destination.Rescue
            showReflection -> Destination.Reflection
            showPaywall -> Destination.Paywall
            showPremiumIntro -> Destination.PremiumIntro
            else -> Destination.Focus
        }

        AnimatedContent(
            targetState = destination,
            transitionSpec = {
                transitionFor(from = initialState, to = targetState, reducedMotion = reducedMotion)
            },
            label = "screen",
        ) { current ->
            when (current) {
            Destination.Intervention -> FocusInterventionScreen(
                packageName = blockedPackage!!,
                onReturn = InterceptEvents::dismiss,
                onOpenBriefly = {
                    BlockedAppController.openForOneMinute(blockedPackage!!)
                    InterceptEvents.dismiss()
                },
            )
            Destination.Welcome -> WelcomeScreen(
                onDone = {
                    SquishySettings.markWelcomeSeen()
                    welcomeSeen = true
                },
            )
            Destination.ChooseDistractions -> DistractionPicker { selected ->
                FocusPreferences.completeOnboarding(selected)
                protectedAppCount = selected.size
                protectionEnabled = FocusPreferences.isProtectionEnabled()
                skipProtectionSetup = false
                onboardingComplete = true
            }
            Destination.ProtectionSetup -> ProtectionSetupScreen(
                onEnable = FocusPreferences::openProtectionSettings,
                onSkip = {
                    SquishySettings.markProtectionDeclined()
                    skipProtectionSetup = true
                },
            )
            Destination.Planner -> MissionPlannerScreen { planned ->
                mission = planned
                missionAccepted = false
                activeBlockIndex = 0
            }
            Destination.MissionReview -> MissionReviewScreen(
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
            Destination.Break -> MissionBreakScreen(
                block = mission!!.blocks[activeBlockIndex],
                nextBlock = mission!!.blocks.getOrNull(activeBlockIndex + 1),
                material = material,
                reducedMotion = reducedMotion,
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
            Destination.Journey -> JourneyScreen(
                stats = lifetime,
                selected = material,
                isPremium = isPremium,
                onBack = { showJourney = false },
                onPremium = { showJourney = false; showPremiumIntro = true },
                onSelect = {
                    material = it
                    SquishySettings.saveMaterialKey(it.name)
                    showJourney = false
                },
                onTryLocked = {
                    trialMaterial = it
                    showJourney = false
                },
            )
            Destination.Rescue -> RescueModeScreen(
                onDismiss = { showRescueMode = false },
                onReward = { seconds ->
                    timerViewModel.applyTimeReward(seconds)
                    showRescueMode = false
                },
            )
            Destination.Reflection -> SessionReflectionScreen(
                completedBlock = mission!!.blocks.getOrNull(activeBlockIndex)
                    ?: MissionBlock("Focus session", uiState.selectedMinutes),
                bankedMinutes = uiState.totalSeconds / 60,
                justUnlocked = justUnlocked,
                material = material,
                onWearUnlocked = {
                    material = it
                    SquishySettings.saveMaterialKey(it.name)
                    justUnlocked = null
                },
                focusedMinutes = lifetime.focusedMinutes,
                isPremium = isPremium,
                reducedMotion = reducedMotion,
                onRated = { feeling ->
                    justUnlocked = null
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
            Destination.Paywall -> {
                val options = remember {
                    PaywallOptions(dismissRequest = { showPaywall = false }) {
                        shouldDisplayDismissButton = true
                    }
                }
                Paywall(options)
            }
            Destination.PremiumIntro -> PremiumIntroScreen(
                onDismiss = { showPremiumIntro = false },
                onSeePlans = {
                    showPremiumIntro = false
                    showPaywall = true
                },
            )
            Destination.Focus -> FocusScreen(
                uiState = uiState,
                missionBlock = mission!!.blocks.getOrNull(activeBlockIndex),
                material = trialMaterial ?: material,
                completedBlocks = lifetime.completedBlocks,
                minutesToNextBody = nextMaterialToEarn(lifetime.focusedMinutes)
                    ?.takeIf { !isPremium }
                    ?.let { it.unlockMinutes - lifetime.focusedMinutes },
                reducedMotion = reducedMotion,
                isTrialling = trialMaterial != null,
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
            Text(
                "Turn on your pause.",
                color = Ink,
                fontSize = 31.sp,
                lineHeight = 37.sp,
                fontWeight = FontWeight.Light,
            )
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
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = OnLight),
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
            Text(
                "Did you open it on purpose?",
                color = Ink,
                fontSize = 29.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.Light,
            )
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
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = OnLight),
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
    material: SquishyMaterial,
    completedBlocks: Int,
    minutesToNextBody: Int?,
    reducedMotion: Boolean,
    isTrialling: Boolean,
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
                completedBlocks = completedBlocks,
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
                text = SquishyVoice.line(
                    state = uiState.squishyState,
                    justCompleted = uiState.lastSessionCompleted,
                    completedBlocks = completedBlocks,
                    minutesToNextBody = minutesToNextBody,
                ),
                color = Muted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(18.dp))

            SquishyStage(
                state = uiState.squishyState,
                progress = uiState.progress,
                accent = accent,
                material = material,
                reducedMotion = reducedMotion,
                onSquish = {
                    if (!uiState.isSessionActive) {
                        tensionReleased = (tensionReleased + 1).coerceAtMost(3)
                    }
                },
            )

            Spacer(Modifier.height(12.dp))

            // Hidden mid-session: choosing a new toy is exactly the kind of small
            // decision a focus block is supposed to protect you from.
            if (!uiState.isSessionActive) {
                CurrentMaterialLine(
                    material = material,
                    isTrialling = isTrialling,
                    onOpenShelf = onJourney,
                )
                Spacer(Modifier.height(12.dp))
            }


            Text(
                text = formatTime(uiState.remainingSeconds),
                color = Ink,
                fontSize = 60.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp,
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
                            "I need a boost  →",
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

            Spacer(Modifier.height(18.dp))
        }
    }
}

/** The body you are holding, and the way into the shelf. */
@Composable
private fun CurrentMaterialLine(
    material: SquishyMaterial,
    isTrialling: Boolean,
    onOpenShelf: () -> Unit,
) {
    Text(
        text = if (isTrialling) {
            "Trying ${material.displayName} — squeeze it while you can"
        } else {
            "${material.displayName}  ›"
        },
        color = if (isTrialling) Coral else Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onOpenShelf)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${material.displayName}. Open the shelf to change it."
            },
    )
}

@Composable
private fun Header(
    completedBlocks: Int,
    onJourney: () -> Unit,
    protectedAppCount: Int,
    onManageApps: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        QuietAction(
            label = if (protectedAppCount == 0) "Protect apps" else "$protectedAppCount protected",
            onClick = onManageApps,
        )
        Spacer(Modifier.width(18.dp))
        QuietAction(
            label = if (completedBlocks == 1) "1 block" else "$completedBlocks blocks",
            onClick = onJourney,
        )
    }
}

/** A tappable label. No container, no border: weight comes from the type alone. */
@Composable
private fun QuietAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
    )
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

/**
 * A way to buy focus time back.
 *
 * Every action runs on trust: a short preparation countdown, then a button. The
 * camera-verified burpee that used to sit here has gone. It could not work on
 * iOS without a Mac to build it on, it cost fifty megabytes of pose model on
 * Android, and above all a product whose voice is "no guilt" should not point a
 * camera at somebody to check they did a burpee.
 *
 * The one thing the app *can* verify honestly is a squish, so that action counts
 * real ones on the body instead of running a timer.
 */
private data class RescueAction(
    val title: String,
    val instruction: String,
    val rewardSeconds: Int,
    val preparationSeconds: Int,
    val countsSquishes: Boolean = false,
)

private const val RESCUE_SQUISHES = 10

@Composable
private fun RescueModeScreen(
    onDismiss: () -> Unit,
    onReward: (Int) -> Unit,
) {
    val actions = remember {
        listOf(
            RescueAction("1 burpee", "Down, floor, up. One is enough.", 5 * 60, 12),
            RescueAction("20 jumping jacks", "Move your whole body and change your state.", 5 * 60, 15),
            RescueAction("Guided breathing", "Breathe in for 4. Out for 6. Repeat.", 3 * 60, 30),
            RescueAction("10 squishes", "Put the restlessness straight into Squishy.", 60, 0, countsSquishes = true),
        )
    }
    var selected by remember { mutableStateOf<RescueAction?>(null) }
    var countdown by remember { mutableIntStateOf(0) }
    var squishes by remember { mutableIntStateOf(0) }

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
                        squishes = 0
                    }
                }) { Text(if (selected == null) "Close" else "Back", color = Muted) }
                Text("RESCUE MODE", color = Coral, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                Spacer(Modifier.width(64.dp))
            }

            Spacer(Modifier.weight(0.55f))

            if (selected == null) {
                Text(
                    "Change your state.",
                    color = Ink,
                    fontSize = 34.sp,
                    lineHeight = 41.sp,
                    fontWeight = FontWeight.Light,
                )
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
                        Text("→", color = Coral, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                val action = selected!!
                if (action.countsSquishes) {
                    val done = squishes >= RESCUE_SQUISHES
                    SquishyStage(
                        state = if (done) SquishyState.RELAXING else SquishyState.TENSE,
                        progress = squishes.toFloat() / RESCUE_SQUISHES,
                        accent = if (done) Sage else Coral,
                        onSquish = { squishes = (squishes + 1).coerceAtMost(RESCUE_SQUISHES) },
                        stageSize = 250.dp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        if (done) "That is the restlessness dealt with." else "${RESCUE_SQUISHES - squishes} to go",
                        color = if (done) Sage else Ink,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Light,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(action.instruction, color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick = { onReward(action.rewardSeconds) },
                        enabled = done,
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(29.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Ink),
                    ) {
                        Text(if (done) "Claim the minute" else "Squeeze", fontWeight = FontWeight.Bold)
                    }
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
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SquishyMaterial.entries.filter { it.isPro }.forEach { body ->
                    MaterialOrb(
                        material = body,
                        tint = SquishyState.RELAXING.bodyTint(),
                        shade = SquishyState.RELAXING.bodyShadow(),
                        modifier = Modifier.size(52.dp),
                    )
                }
            }
            Spacer(Modifier.height(30.dp))
            Text(
                "You can earn all of this.",
                color = Color.White,
                fontSize = 34.sp,
                lineHeight = 41.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Every squishy unlocks with focused minutes. Pro is for people who would rather not wait.",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 15.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(38.dp))
            PremiumBenefit("01", "Every body, now", "The four you are working towards, without the wait.")
            PremiumBenefit("02", "Smarter rescues", "Turn movement and breath into time you get back.")
            PremiumBenefit("03", "No ads, ever, for anyone", "Subscriptions are the only thing this app sells.")
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onSeePlans,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(29.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Premium,
                    contentColor = OnLight,
                ),
            ) {
                Text("See Premium plans", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Cancel anytime · keep every squishy you earned",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
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
        Text(number, color = Color.White.copy(alpha = 0.32f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(5, 15, 25, 45).forEach { minutes ->
            val active = selected == minutes
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelected(minutes) }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
                    .semantics {
                        role = Role.RadioButton
                        this.selected = active
                        contentDescription = "$minutes minute block"
                    },
            ) {
                Text(
                    text = "$minutes",
                    color = if (active) Ink else Muted,
                    fontSize = 16.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier
                        .size(width = 14.dp, height = 1.5.dp)
                        .background(if (active) Ink else Color.Transparent),
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
