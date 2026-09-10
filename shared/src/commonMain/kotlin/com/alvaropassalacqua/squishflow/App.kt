package com.alvaropassalacqua.squishflow

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import kotlin.math.abs

private val Cream = Color(0xFF0C0E0D)
private val Ink = Color(0xFFF2F0E9)
private val Muted = Color(0xFF969991)
private val Sage = Color(0xFF8DD6AA)
private val Coral = Color(0xFFFF806C)
private val Lavender = Color(0xFFC5A8F2)
private val SoftWhite = Color(0xFF171A18)

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
                    ?: MissionBlock("Sesión de foco", uiState.selectedMinutes),
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
            Text("Activa tu pausa.", color = Ink, fontSize = 31.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(12.dp))
            Text(
                "Android necesita tu permiso una sola vez para detectar las apps elegidas. Squishflow nunca lee lo que haces dentro.",
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
                Text("Activar protección en Android", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onSkip) {
                Text("Ahora no · usar solo el temporizador", color = Muted, fontSize = 12.sp)
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
            Text("PAUSA CONSCIENTE", color = Sage, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            Spacer(Modifier.weight(0.45f))
            Text("¿La abriste por decisión?", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(9.dp))
            Text(
                "Aprieta a Squishy tres veces antes de continuar.",
                color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            TimerStage(
                uiState = TimerUiState(squishyState = SquishyState.TENSE),
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
            ) { Text("Volver a lo importante", fontWeight = FontWeight.Bold) }
            TextButton(onClick = onOpenBriefly, enabled = squishes >= 3) {
                Text(
                    if (squishes < 3) "Respira y aprieta · ${3 - squishes}" else "Entrar durante 1 minuto",
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
                    "MISIÓN · ${missionBlock.title}",
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
                    uiState.lastSessionCompleted -> "Has vuelto. Eso cuenta."
                    uiState.isSessionActive -> "Solo esto. Solo ahora."
                    uiState.squishyState == SquishyState.COMPRESSED -> "Sin culpa. Vuelve cuando quieras."
                    else -> "Toca. Respira. Empieza."
                },
                color = Muted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(18.dp))

            TimerStage(
                uiState = uiState,
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
                    SquishyState.TENSE -> "LISTO"
                    SquishyState.RELAXING -> "EN FOCO"
                    SquishyState.COMPRESSED -> "INTERRUMPIDO"
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
                            "Necesito un impulso  ->",
                            color = Ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    Text(
                        "Impulso usado · protege el tiempo que queda",
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
                    "Aprieta o estira a Squishy · ${3 - tensionReleased} gestos para relajarlo"
                else
                    "Squishy esta listo para proteger tu atencion",
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
                "AI FOCUS COMPANION",
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
private fun TimerStage(
    uiState: TimerUiState,
    accent: Color,
    onSquish: () -> Unit,
) {
    val x = remember { Animatable(0f) }
    val y = remember { Animatable(0f) }
    val burst = remember { Animatable(1f) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressDepth by animateFloatAsState(
        targetValue = if (isPressed) 0.76f else 1f,
        animationSpec = spring(
            dampingRatio = if (isPressed) 0.82f else 0.46f,
            stiffness = if (isPressed) 520f else 115f,
        ),
    )
    val pressSpread by animateFloatAsState(
        targetValue = if (isPressed) 1.16f else 1f,
        animationSpec = spring(dampingRatio = 0.48f, stiffness = 130f),
    )
    val scope = rememberCoroutineScope()
    val stateScale by animateFloatAsState(
        targetValue = if (uiState.squishyState == SquishyState.COMPRESSED) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    )

    Box(
        modifier = Modifier.size(292.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = Ink.copy(alpha = 0.055f),
                style = Stroke(width = 7.dp.toPx()),
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * uiState.progress,
                useCenter = false,
                style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round),
            )

            val p = burst.value
            if (p < 1f) {
                val directions = listOf(
                    -1.0f to -0.25f, -0.72f to -0.72f, -0.2f to -1f,
                    0.35f to -0.92f, 0.82f to -0.55f, 1f to 0.08f,
                    0.72f to 0.72f, 0.12f to 1f, -0.55f to 0.82f,
                    -0.92f to 0.45f,
                )
                directions.forEachIndexed { index, direction ->
                    val distance = (78f + index * 4f) * p * density
                    drawCircle(
                        color = (if (index % 3 == 0) Coral else accent).copy(alpha = (1f - p) * 0.9f),
                        radius = (3.5f + (index % 3) * 1.4f) * density * (1f - p * 0.35f),
                        center = center + androidx.compose.ui.geometry.Offset(direction.first * distance, direction.second * distance),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(250.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = "Squishy, compañero de concentración interactivo"
                    stateDescription = when (uiState.squishyState) {
                        SquishyState.TENSE -> "Tenso, preparado para empezar"
                        SquishyState.RELAXING -> "Relajándose durante la sesión"
                        SquishyState.COMPRESSED -> "Comprimido después de una interrupción"
                    }
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    onSquish()
                    scope.launch {
                        burst.snapTo(0f)
                        burst.animateTo(1f, androidx.compose.animation.core.tween(720))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            scope.launch {
                                burst.snapTo(0f)
                                burst.animateTo(1f, androidx.compose.animation.core.tween(720))
                            }
                        },
                        onDragEnd = {
                            onSquish()
                            scope.launch {
                                launch { x.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                                launch { y.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                launch { x.animateTo(0f) }
                                launch { y.animateTo(0f) }
                            }
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            x.snapTo((x.value + dragAmount.x).coerceIn(-110f, 110f))
                            y.snapTo((y.value + dragAmount.y).coerceIn(-90f, 90f))
                        }
                    }
                }
                .graphicsLayer {
                    translationX = x.value
                    translationY = y.value
                    rotationZ = x.value / 16f
                    scaleX = stateScale * pressSpread * (1f + abs(y.value) / 520f)
                    scaleY = stateScale * pressDepth * (1f - abs(y.value) / 620f)
                },
            contentAlignment = Alignment.Center,
        ) {
            SquishyBody(
                state = uiState.squishyState,
                deformationX = x.value,
                deformationY = y.value,
                modifier = Modifier.fillMaxSize(),
            )
            SquishyFace(
                state = uiState.squishyState,
                lookX = x.value,
                lookY = y.value,
                modifier = Modifier.fillMaxSize(),
            )
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
            if (released >= 3) "TENSION LIBERADA" else "LIBERA TENSION",
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

@Composable
private fun SquishyBody(
    state: SquishyState,
    deformationX: Float,
    deformationY: Float,
    modifier: Modifier = Modifier,
) {
    val motion = androidx.compose.animation.core.rememberInfiniteTransition(label = "squishy-breath")
    val breath by motion.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.018f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = if (state == SquishyState.RELAXING) 1900 else 2800,
                easing = androidx.compose.animation.core.FastOutSlowInEasing,
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "breath",
    )
    val base = when (state) {
        SquishyState.TENSE -> Color(0xFFFF806C)
        SquishyState.RELAXING -> Color(0xFF8DD6AA)
        SquishyState.COMPRESSED -> Color(0xFFC5A8F2)
    }
    val deep = when (state) {
        SquishyState.TENSE -> Color(0xFFB9473B)
        SquishyState.RELAXING -> Color(0xFF3E8960)
        SquishyState.COMPRESSED -> Color(0xFF77569F)
    }

    Canvas(
        modifier.graphicsLayer {
            scaleX = breath
            scaleY = breath
        },
    ) {
        val w = size.width
        val h = size.height
        val dx = (deformationX / 110f).coerceIn(-1f, 1f)
        val dy = (deformationY / 90f).coerceIn(-1f, 1f)
        val compressed = if (state == SquishyState.COMPRESSED) 0.08f else 0f
        val top = h * (0.095f + compressed + kotlin.math.abs(dy) * 0.025f)
        val bottom = h * (0.89f - compressed * 0.2f)
        val centerShift = dx * w * 0.035f

        drawOval(
            color = Color.Black.copy(alpha = 0.32f),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.19f, h * 0.83f),
            size = androidx.compose.ui.geometry.Size(w * 0.62f, h * 0.10f),
        )

        val blob = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.50f + centerShift, top)
            cubicTo(
                w * 0.72f + centerShift, h * (0.07f + compressed),
                w * (0.91f + dx * 0.025f), h * 0.24f,
                w * (0.89f + dx * 0.035f), h * 0.46f,
            )
            cubicTo(
                w * (0.96f + dx * 0.025f), h * 0.65f,
                w * 0.78f, bottom,
                w * 0.56f, bottom,
            )
            cubicTo(
                w * 0.48f, h * 0.94f,
                w * 0.37f, h * 0.91f,
                w * 0.31f, bottom * 0.98f,
            )
            cubicTo(
                w * 0.10f, h * 0.82f,
                w * (0.04f + dx * 0.025f), h * 0.61f,
                w * (0.11f + dx * 0.035f), h * 0.43f,
            )
            cubicTo(
                w * (0.09f + dx * 0.025f), h * 0.22f,
                w * 0.28f + centerShift, h * (0.07f + compressed),
                w * 0.50f + centerShift, top,
            )
            close()
        }

        drawPath(
            path = blob,
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.52f),
                    base,
                    deep,
                ),
                center = androidx.compose.ui.geometry.Offset(w * (0.34f + dx * 0.02f), h * 0.25f),
                radius = w * 0.78f,
            ),
        )
        drawPath(
            path = blob,
            color = Color.White.copy(alpha = 0.10f),
            style = Stroke(width = 1.5.dp.toPx()),
        )

        clipPath(blob) {
            drawOval(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.12f), Color.Transparent),
                ),
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.10f, h * 0.62f),
                size = androidx.compose.ui.geometry.Size(w * 0.82f, h * 0.25f),
            )
            listOf(
                0.29f to 0.70f, 0.68f to 0.26f, 0.73f to 0.58f, 0.39f to 0.28f,
            ).forEachIndexed { index, point ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.055f + index * 0.012f),
                    radius = (5f + index * 1.7f) * density,
                    center = androidx.compose.ui.geometry.Offset(w * point.first, h * point.second),
                )
            }
        }

        drawOval(
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
            ),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.24f, h * (0.16f + compressed)),
            size = androidx.compose.ui.geometry.Size(w * 0.27f, h * 0.12f),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.13f),
            radius = w * 0.045f,
            center = androidx.compose.ui.geometry.Offset(w * 0.76f, h * 0.68f),
        )
    }
}
@Composable
private fun SquishyFace(
    state: SquishyState,
    lookX: Float,
    lookY: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val faceInk = Color(0xFF151713)
        val shift = (lookX / 16f).coerceIn(-7f, 7f)
        val verticalShift = (lookY / 20f).coerceIn(-4f, 4f)
        val eyeY = size.height * if (state == SquishyState.COMPRESSED) 0.47f else 0.43f
        val eyeGap = size.width * 0.105f
        val radius = if (state == SquishyState.COMPRESSED) 5.dp.toPx() else 7.5.dp.toPx()
        val left = androidx.compose.ui.geometry.Offset(size.width / 2f - eyeGap + shift, eyeY + verticalShift)
        val right = androidx.compose.ui.geometry.Offset(size.width / 2f + eyeGap + shift, eyeY + verticalShift)

        if (state == SquishyState.COMPRESSED) {
            drawLine(
                faceInk,
                start = left + androidx.compose.ui.geometry.Offset(-7.dp.toPx(), -4.dp.toPx()),
                end = left + androidx.compose.ui.geometry.Offset(7.dp.toPx(), 4.dp.toPx()),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                faceInk,
                start = right + androidx.compose.ui.geometry.Offset(-7.dp.toPx(), 4.dp.toPx()),
                end = right + androidx.compose.ui.geometry.Offset(7.dp.toPx(), -4.dp.toPx()),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
        } else {
            drawCircle(faceInk, radius, center = left)
            drawCircle(faceInk, radius, center = right)
            drawCircle(Color.White.copy(alpha = 0.9f), radius * 0.28f, center = left + androidx.compose.ui.geometry.Offset(-radius * 0.25f, -radius * 0.28f))
            drawCircle(Color.White.copy(alpha = 0.9f), radius * 0.28f, center = right + androidx.compose.ui.geometry.Offset(-radius * 0.25f, -radius * 0.28f))
        }

        drawCircle(
            Color(0xFFFF8B86).copy(alpha = if (state == SquishyState.RELAXING) 0.42f else 0.25f),
            radius = 10.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(size.width * 0.35f, size.height * 0.52f),
        )
        drawCircle(
            Color(0xFFFF8B86).copy(alpha = if (state == SquishyState.RELAXING) 0.42f else 0.25f),
            radius = 10.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(size.width * 0.65f, size.height * 0.52f),
        )

        drawArc(
            color = faceInk,
            startAngle = if (state == SquishyState.COMPRESSED) 205f else 20f,
            sweepAngle = if (state == SquishyState.RELAXING) 140f else 130f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.43f, size.height * 0.49f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.14f, size.height * 0.09f),
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
        )
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
            RescueAction("1 burpee", "Verificado con la camara.", 5 * 60, 0),
            RescueAction("20 jumping jacks", "Mueve todo el cuerpo y cambia tu estado.", 5 * 60, 15),
            RescueAction("Respiracion consciente", "Inhala 4 segundos. Exhala 6. Repite.", 3 * 60, 30),
            RescueAction("10 squishes", "Descarga la inquietud directamente en Squishy.", 60, 8),
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
                }) { Text(if (selected == null) "Cerrar" else "Atras", color = Muted) }
                Text("RESCUE MODE", color = Coral, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                Spacer(Modifier.width(64.dp))
            }

            Spacer(Modifier.weight(0.55f))

            if (selected == null) {
                Text("Cambia tu estado.", color = Ink, fontSize = 34.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Una accion real puede comprarte tiempo de foco.\nElige solo una.",
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
                                if (action.rewardSeconds == 60) "-1 minuto" else "-${action.rewardSeconds / 60} minutos",
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
                    Text("Verificacion por camara", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Light)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Analizaremos tu postura en directo para reconocer un burpee completo: de pie, suelo y de pie.",
                        color = Muted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp,
                    )
                    Spacer(Modifier.height(28.dp))
                    Surface(color = Sage.copy(alpha = 0.12f), shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text("PRIVACIDAD", color = Sage, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "El analisis ocurre en este dispositivo. No grabamos, guardamos ni enviamos imagenes.",
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
                    ) { Text("Entiendo - activar camara", fontWeight = FontWeight.Bold) }
                } else {
                    Text(
                        if (countdown > 0) countdown.toString() else "LISTO",
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
                        Text(if (countdown > 0) "Hazlo ahora" else "Hecho - descontar tiempo", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Text(
                "Un rescate por sesion. Sin culpa, sin atajos infinitos.",
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
                    Text("Cerrar", color = Color.White.copy(alpha = 0.72f))
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
                "Tu foco no necesita fuerza.",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Light,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Un reset sensorial para volver antes de perderte.",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(38.dp))
            PremiumBenefit("01", "Rescates inteligentes", "Convierte movimiento y respiracion en tiempo recuperado.")
            PremiumBenefit("02", "Squishies coleccionables", "Nuevos materiales, colores y personalidades.")
            PremiumBenefit("03", "Tu mapa de atencion", "Descubre que duracion y rescate funcionan para ti.")
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
                Text("Ver planes Premium", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Cancela cuando quieras",
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
            if (active) "Terminar sesión" else "Empezar foco",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun formatTime(totalSeconds: Int): String =
    (totalSeconds / 60).toString().padStart(2, '0') + ":" +
        (totalSeconds % 60).toString().padStart(2, '0')
