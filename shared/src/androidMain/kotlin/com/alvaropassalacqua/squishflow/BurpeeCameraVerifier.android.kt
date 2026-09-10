package com.alvaropassalacqua.squishflow

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

@Composable
actual fun BurpeeCameraVerifier(
    onVerified: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { allowed ->
        granted = allowed
        denied = !allowed
    }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }

    when {
        granted -> LiveBurpeeCamera(onVerified = onVerified, onCancel = onCancel)
        denied -> CameraPermissionDenied(
            onRetry = { launcher.launch(Manifest.permission.CAMERA) },
            onCancel = onCancel,
        )
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Solicitando acceso a la camara...", color = Color(0xFF20201E))
        }
    }
}

@Composable
private fun CameraPermissionDenied(
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("La camara esta desactivada", fontSize = 25.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(12.dp))
        Text(
            "Necesitamos ver tu cuerpo completo para verificar el movimiento. Ninguna imagen se guarda.",
            color = Color(0xFF7C7A73),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onRetry) { Text("Permitir camara") }
        TextButton(onClick = onCancel) { Text("Volver") }
    }
}

@Composable
private fun LiveBurpeeCamera(
    onVerified: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var status by remember { mutableStateOf("Alejate hasta que se vea tu cuerpo completo") }
    var progress by remember { mutableIntStateOf(0) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val detector = remember {
        PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build(),
        )
    }
    val analyzer = remember {
        BurpeePoseAnalyzer(
            onProgress = { step, message ->
                mainExecutor.execute {
                    progress = step
                    status = message
                }
            },
            onVerified = {
                mainExecutor.execute(onVerified)
            },
            detector = detector,
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            detector.close()
            analysisExecutor.shutdown()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = surfaceProvider
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { it.setAnalyzer(analysisExecutor, analyzer) }
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            analysis,
                        )
                    }, mainExecutor)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.48f), Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                    ),
                ),
        )

        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancelar", color = Color.White)
            }
            Surface(
                color = Color.Black.copy(alpha = 0.38f),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(
                    "EN EL DISPOSITIVO",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    Surface(
                        modifier = Modifier.size(if (index < progress) 10.dp else 7.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (index < progress) Color(0xFF8FD0A7) else Color.White.copy(alpha = 0.35f),
                    ) {}
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                status,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                "De pie  ->  suelo  ->  de pie",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 12.sp,
            )
        }
    }
}

private class BurpeePoseAnalyzer(
    private val onProgress: (Int, String) -> Unit,
    private val onVerified: () -> Unit,
    private val detector: com.google.mlkit.vision.pose.PoseDetector,
) : ImageAnalysis.Analyzer {
    private enum class Phase { FIND_STANDING, FIND_DOWN, FIND_RETURN, DONE }

    private var phase = Phase.FIND_STANDING
    private var stableFrames = 0
    private val processing = AtomicBoolean(false)

    override fun analyze(imageProxy: androidx.camera.core.ImageProxy) {
        if (!processing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            processing.set(false)
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        detector.process(image)
            .addOnSuccessListener { evaluate(it) }
            .addOnFailureListener {
                onProgress(progressForPhase(), "No puedo leer la pose. Mejora la luz.")
            }
            .addOnCompleteListener {
                processing.set(false)
                imageProxy.close()
            }
    }

    private fun evaluate(pose: Pose) {
        if (phase == Phase.DONE) return
        val required = listOf(
            PoseLandmark.LEFT_SHOULDER,
            PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_KNEE,
            PoseLandmark.RIGHT_KNEE,
            PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_ANKLE,
            PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_WRIST,
        ).mapNotNull(pose::getPoseLandmark)

        if (required.size < 10 || required.any { it.inFrameLikelihood < 0.55f }) {
            stableFrames = 0
            onProgress(progressForPhase(), "Coloca el cuerpo completo dentro del marco")
            return
        }

        fun midY(a: Int, b: Int) =
            (pose.getPoseLandmark(a)!!.position.y + pose.getPoseLandmark(b)!!.position.y) / 2f

        val shoulderY = midY(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        val hipY = midY(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
        val kneeY = midY(PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE)
        val ankleY = midY(PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE)
        val wristY = midY(PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST)

        val fullHeight = ankleY - shoulderY
        val standing = fullHeight > 220f && shoulderY < hipY && hipY < kneeY && kneeY < ankleY
        val torso = abs(hipY - shoulderY).coerceAtLeast(1f)
        val legs = abs(kneeY - hipY).coerceAtLeast(1f)
        val down = (abs(shoulderY - hipY) < legs * 0.85f && wristY > shoulderY) ||
            (hipY > kneeY - torso * 0.28f && wristY > shoulderY)

        val matches = when (phase) {
            Phase.FIND_STANDING -> standing
            Phase.FIND_DOWN -> down
            Phase.FIND_RETURN -> standing
            Phase.DONE -> false
        }

        if (matches) stableFrames++ else stableFrames = (stableFrames - 1).coerceAtLeast(0)
        if (stableFrames < 4) {
            onProgress(
                progressForPhase(),
                when (phase) {
                    Phase.FIND_STANDING -> "Ponte de pie y muestra el cuerpo completo"
                    Phase.FIND_DOWN -> "Baja: manos al suelo y piernas atras"
                    Phase.FIND_RETURN -> "Vuelve arriba para completar"
                    Phase.DONE -> "Burpee verificado"
                },
            )
            return
        }

        stableFrames = 0
        phase = when (phase) {
            Phase.FIND_STANDING -> Phase.FIND_DOWN
            Phase.FIND_DOWN -> Phase.FIND_RETURN
            Phase.FIND_RETURN -> Phase.DONE
            Phase.DONE -> Phase.DONE
        }
        if (phase == Phase.DONE) {
            onProgress(3, "Burpee verificado")
            onVerified()
        } else {
            onProgress(
                progressForPhase(),
                if (phase == Phase.FIND_DOWN) "Ahora baja y lleva las manos al suelo"
                else "Perfecto. Vuelve a ponerte de pie",
            )
        }
    }

    private fun progressForPhase(): Int = when (phase) {
        Phase.FIND_STANDING -> 0
        Phase.FIND_DOWN -> 1
        Phase.FIND_RETURN -> 2
        Phase.DONE -> 3
    }
}
