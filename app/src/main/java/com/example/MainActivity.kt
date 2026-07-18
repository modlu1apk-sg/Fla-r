package com.example

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.CustomizationMenu
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var flashlightController: FlashlightController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        flashlightController = FlashlightController(this)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    FlasorApp(
                        activity = this,
                        controller = flashlightController
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        flashlightController.stopAll()
    }
}

// Simple helper to adjust screen brightness (0.0f to 1.0f)
fun setActivityBrightness(activity: ComponentActivity, brightness: Float) {
    val layoutParams = activity.window.attributes
    layoutParams.screenBrightness = brightness
    activity.window.attributes = layoutParams
}

@Composable
fun FlasorApp(
    activity: ComponentActivity,
    controller: FlashlightController
) {
    val context = LocalContext.current
    val isTorchOn by controller.isTorchOn.collectAsStateWithLifecycle()
    val errorMessage by controller.errorMessage.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("Flaşör") }
    val scope = rememberCoroutineScope()

    // Screen light configurations
    var isScreenLightOn by remember { mutableStateOf(false) }
    var screenLightColor by remember { mutableStateOf(Color.White) }
    var screenBrightnessValue by remember { mutableStateOf(0.8f) }

    // Special Fullscreen modes
    var isPoliceActive by remember { mutableStateOf(false) }
    var isDiscoActive by remember { mutableStateOf(false) }
    var discoSpeed by remember { mutableStateOf(200L) } // in milliseconds

    // Strobe configurations
    var strobeFrequencyHz by remember { mutableStateOf(5.0f) }
    var isStrobeActiveState by remember { mutableStateOf(false) }

    // Advanced Customization Menu State
    var showCustomizationMenu by remember { mutableStateOf(false) }

    // Show toast for hardware errors
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    // Auto-restoration of screen brightness when disabling screen light
    DisposableEffect(isScreenLightOn, isPoliceActive, isDiscoActive, screenBrightnessValue) {
        if (isScreenLightOn || isPoliceActive || isDiscoActive) {
            val brightness = if (isScreenLightOn) screenBrightnessValue else 1.0f
            setActivityBrightness(activity, brightness)
        } else {
            setActivityBrightness(activity, -1.0f) // system default
        }
        onDispose {
            setActivityBrightness(activity, -1.0f)
        }
    }

    // Fullscreen Screen Light Overlay
    if (isScreenLightOn) {
        FullscreenScreenLight(
            color = screenLightColor,
            onClose = { isScreenLightOn = false }
        )
        return
    }

    // Fullscreen Police Overlay
    if (isPoliceActive) {
        FullscreenPoliceLight(
            onClose = { isPoliceActive = false }
        )
        return
    }

    // Fullscreen Disco Overlay
    if (isDiscoActive) {
        FullscreenDiscoLight(
            speedMs = discoSpeed,
            onClose = { isDiscoActive = false }
        )
        return
    }

    // Advanced Customization Menu Overlay
    if (showCustomizationMenu) {
        CustomizationMenu(
            onDismiss = { showCustomizationMenu = false },
            onApply = { color, speed, pattern ->
                controller.stopAll()
                isStrobeActiveState = false
                isScreenLightOn = false
                isPoliceActive = false
                isDiscoActive = false

                when (pattern) {
                    "Sürekli" -> {
                        screenLightColor = color
                        isScreenLightOn = true
                        if (color == Color.White) {
                            controller.toggleTorch(true)
                        }
                    }
                    "Strobe" -> {
                        strobeFrequencyHz = speed
                        isStrobeActiveState = true
                        activeTab = "Stroboskop"
                    }
                    "S.O.S" -> {
                        activeTab = "Mors"
                        controller.startSOS()
                    }
                    "Darbe" -> {
                        controller.startHeartbeat()
                        Toast.makeText(context, "Darbe (Kalp Atışı) sinyali başlatıldı!", Toast.LENGTH_SHORT).show()
                    }
                    "Polis" -> {
                        activeTab = "Eğlence"
                        isPoliceActive = true
                    }
                    "Disko" -> {
                        activeTab = "Eğlence"
                        discoSpeed = (1000 / speed).toLong().coerceIn(50L, 2000L)
                        isDiscoActive = true
                    }
                }
            }
        )
    }

    // Main App Scaffold
    Scaffold(
        bottomBar = {
            FlasorBottomBar(
                activeTab = activeTab,
                onTabSelected = {
                    activeTab = it
                    // Stop running loops when switching tabs
                    controller.stopAll()
                }
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BackgroundDark, GradientEnd)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            AppHeader(onSettingsClick = { showCustomizationMenu = true })

            Spacer(modifier = Modifier.height(8.dp))

            // Main Content Area based on Tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                when (activeTab) {
                    "Flaşör" -> TorchScreen(controller = controller, isTorchOn = isTorchOn)
                    "Stroboskop" -> StrobeScreen(
                        controller = controller,
                        frequencyHz = strobeFrequencyHz,
                        onFrequencyChange = { strobeFrequencyHz = it },
                        isStrobeActive = isStrobeActiveState,
                        onStrobeActiveChange = { isStrobeActiveState = it }
                    )
                    "Mors" -> MorseScreen(controller = controller)
                    "Ekran Işığı" -> ScreenLightControlScreen(
                        onTurnOn = { color ->
                            screenLightColor = color
                            isScreenLightOn = true
                        },
                        brightness = screenBrightnessValue,
                        onBrightnessChange = { screenBrightnessValue = it }
                    )
                    "Eğlence" -> FunModesScreen(
                        onPoliceStart = { isPoliceActive = true },
                        onDiscoStart = { isDiscoActive = true },
                        discoSpeed = discoSpeed,
                        onDiscoSpeedChange = { discoSpeed = it }
                    )
                    "Hakkında" -> AboutScreen()
                }
            }
        }
    }
}

@Composable
fun AppHeader(onSettingsClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "FLAŞ IŞIK PRO",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "ÇOK FONKSİYONLU SİNYAL SİSTEMİ",
                fontSize = 10.sp,
                color = TextMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BorderDark)
                .clickable { onSettingsClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Ayarlar",
                tint = HighlightBlue,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ----------------------------------------------------
// TAB 1: TORCH SCREEN (CLASSIC POWER TOGGLE)
// ----------------------------------------------------
@Composable
fun TorchScreen(controller: FlashlightController, isTorchOn: Boolean) {
    val context = LocalContext.current
    var autoOffMinutes by remember { mutableStateOf<Int?>(null) }
    var timerJobState by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Timer logic to auto turn off
    LaunchedEffect(isTorchOn, autoOffMinutes) {
        if (isTorchOn && autoOffMinutes != null) {
            val totalSeconds = autoOffMinutes!! * 60
            for (sec in totalSeconds downTo 1) {
                val minsLeft = sec / 60
                val secsLeft = sec % 60
                timerJobState = String.format("%02d:%02d kaldı", minsLeft, secsLeft)
                delay(1000L)
            }
            controller.toggleTorch(false)
            autoOffMinutes = null
            timerJobState = null
        } else {
            timerJobState = null
        }
    }

    // Battery Percentage Checker
    val batteryPct = remember { mutableStateOf(100) }
    LaunchedEffect(Unit) {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) {
            batteryPct.value = (level * 100 / scale)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isTorchOn) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            
            // Neon Pulsing Power Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(230.dp)
                    .animateContentSize()
            ) {
                // Background radial light when on
                if (isTorchOn) {
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(HighlightBlue.copy(alpha = 0.20f), Color.Transparent),
                                        radius = size.width / 1.1f
                                    )
                                )
                            }
                    )
                }

                // Main Button Outlines
                Box(
                    modifier = Modifier
                        .size(175.dp * scaleFactor)
                        .shadow(
                            elevation = if (isTorchOn) 24.dp else 0.dp,
                            shape = CircleShape,
                            ambientColor = HighlightBlue,
                            spotColor = HighlightBlue
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(GradientStart, GradientEnd)
                            )
                        )
                        .border(
                            width = 4.dp,
                            color = if (isTorchOn) HighlightBlue else BorderMedium,
                            shape = CircleShape
                        )
                        .clickable {
                            controller.toggleTorch(!isTorchOn)
                        }
                        .testTag("torch_power_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🔦",
                            fontSize = 44.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "GÜÇ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighlightBlue,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // State Display Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isTorchOn) HighlightBlue.copy(alpha = 0.10f) else SurfaceDark,
                border = BorderStroke(1.dp, if (isTorchOn) HighlightBlue.copy(alpha = 0.3f) else BorderDark),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isTorchOn) HighlightBlue else Color(0xFFD32F2F))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isTorchOn) "FLAŞ AÇIK" else "FLAŞ KAPALI",
                        color = if (isTorchOn) HighlightBlue else TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Info bar (Battery & Timer)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Battery info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when {
                                batteryPct.value > 80 -> Icons.Filled.BatteryFull
                                batteryPct.value > 30 -> Icons.Filled.Battery5Bar
                                else -> Icons.Filled.BatteryAlert
                            },
                            contentDescription = "Pil Seviyesi",
                            tint = if (batteryPct.value > 30) Color(0xFF4CAF50) else Color(0xFFE53935)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Pil Durumu", color = TextMuted, fontSize = 10.sp)
                            Text("%${batteryPct.value}", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    // Divider line
                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(BorderDark))

                    // Auto-off status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.HourglassEmpty,
                            contentDescription = "Kapanma Zamanlayıcı",
                            tint = HighlightBlue
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Otomatik Kapanma", color = TextMuted, fontSize = 10.sp)
                            Text(
                                text = timerJobState ?: "Devre Dışı",
                                color = if (timerJobState != null) HighlightBlue else TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Auto-off presets title
            Text(
                text = "Kapanma Süresi Ayarla",
                color = TextLight,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            
            Spacer(modifier = Modifier.height(10.dp))

            // Presets buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 5, 10).forEach { mins ->
                    val isSelected = autoOffMinutes == mins
                    Button(
                        onClick = {
                            if (!isTorchOn) {
                                controller.toggleTorch(true)
                            }
                            autoOffMinutes = mins
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) HighlightBlue else SurfaceDark,
                            contentColor = if (isSelected) BackgroundDark else TextLight
                        ),
                        border = BorderStroke(1.dp, if (isSelected) HighlightBlue else BorderDark),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("${mins} dk", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        autoOffMinutes = null
                        timerJobState = null
                    },
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF5350).copy(alpha = 0.15f),
                        contentColor = Color(0xFFEF5350)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f))
                ) {
                    Text("İptal Et", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ----------------------------------------------------
// TAB 2: STROBESKOP (STROBE SCREEN)
// ----------------------------------------------------
@Composable
fun StrobeScreen(
    controller: FlashlightController,
    frequencyHz: Float,
    onFrequencyChange: (Float) -> Unit,
    isStrobeActive: Boolean,
    onStrobeActiveChange: (Boolean) -> Unit
) {
    // Synchronize strobe frequency shifts immediately if active
    LaunchedEffect(frequencyHz, isStrobeActive) {
        if (isStrobeActive) {
            controller.startStrobe(frequencyHz)
        } else {
            controller.stopAll()
        }
    }

    // Interactive Screen Flashing during active strobe
    var flashBlinkOn by remember { mutableStateOf(false) }
    LaunchedEffect(isStrobeActive, frequencyHz) {
        if (isStrobeActive) {
            val periodMs = (1000 / frequencyHz).toLong()
            while (true) {
                flashBlinkOn = true
                delay(periodMs / 2)
                flashBlinkOn = false
                delay(periodMs - (periodMs / 2))
            }
        } else {
            flashBlinkOn = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Strobe Visual Indicator Screen Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (flashBlinkOn) Color.White else SurfaceDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isStrobeActive) "FLAŞÖR BLİNK" else "Sinyal Sürücüsü Hazır",
                    color = if (flashBlinkOn) Color.Black else TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Big Start/Stop Switch
            Button(
                onClick = { onStrobeActiveChange(!isStrobeActive) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("strobe_start_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isStrobeActive) Color(0xFFEF5350) else HighlightBlue,
                    contentColor = if (isStrobeActive) Color.White else BackgroundDark
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = if (isStrobeActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isStrobeActive) "STROBOSKOPU DURDUR" else "STROBOSKOPU BAŞLAT",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Frequency Display Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Frekans Ayarı",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${String.format("%.1f", frequencyHz)} Hz",
                        color = HighlightBlue,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = frequencyHz,
                        onValueChange = onFrequencyChange,
                        valueRange = 1.0f..30.0f,
                        steps = 58,
                        colors = SliderDefaults.colors(
                            thumbColor = HighlightBlue,
                            activeTrackColor = HighlightBlue,
                            inactiveTrackColor = BorderDark
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hot-keys Presets
                    Text(
                        text = "Hazır Sinyal Hızları",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val speedPresets = listOf(
                             "Yavaş" to 2.0f,
                             "Orta" to 8.0f,
                             "Hızlı" to 15.0f,
                             "Çılgın" to 28.0f
                        )
                        speedPresets.forEach { (label, value) ->
                            val isCurrent = Math.abs(frequencyHz - value) < 0.2f
                            Button(
                                onClick = { onFrequencyChange(value) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCurrent) HighlightBlue.copy(alpha = 0.15f) else SurfaceDark,
                                    contentColor = if (isCurrent) HighlightBlue else TextLight
                                ),
                                border = BorderStroke(1.dp, if (isCurrent) HighlightBlue else BorderDark),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ----------------------------------------------------
// TAB 3: MORS SCREEN (TRANSLATOR / CUSTOM SENDER)
// ----------------------------------------------------
@Composable
fun MorseScreen(controller: FlashlightController) {
    val context = LocalContext.current
    var inputMessage by remember { mutableStateOf("SOS") }
    var isSending by remember { mutableStateOf(false) }
    var currentCharacter by remember { mutableStateOf(' ') }
    var isSignalPulseOn by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    // Observe active physical flash status to display flashing Morse code indicator on screen
    val isTorchOn by controller.isTorchOn.collectAsStateWithLifecycle()

    fun triggerSOS() {
        isSending = true
        controller.startSOS()
    }

    fun triggerCustomMorse() {
        if (inputMessage.trim().isEmpty()) {
            Toast.makeText(context, "Lütfen bir mesaj yazın!", Toast.LENGTH_SHORT).show()
            return
        }
        keyboardController?.hide()
        isSending = true
        controller.startMorse(
            text = inputMessage,
            onProgress = { char, isActive ->
                currentCharacter = char
                isSignalPulseOn = isActive
            },
            onFinished = {
                isSending = false
                currentCharacter = ' '
                isSignalPulseOn = false
            }
        )
    }

    fun stopTransmission() {
        controller.stopAll()
        isSending = false
        currentCharacter = ' '
        isSignalPulseOn = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // SOS Quick-Trigger Emergency Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1313)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFEF5350))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF5350)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ACİL SOS SİNYALİ",
                                color = Color(0xFFEF5350),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tek dokunuşla uluslararası acil durum mors kodunu başlatın.",
                            color = TextLight.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = { triggerSOS() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("S.O.S", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Custom Coder Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Mors Kod Dönüştürücü",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Yazılan metni otomatik olarak Mors sinyallerine çevirip flaşla yansıtır.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = inputMessage,
                        onValueChange = { inputMessage = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("morse_input_field"),
                        label = { Text("Göndermek İstediğiniz Kelime", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HighlightBlue,
                            unfocusedBorderColor = BorderDark,
                            focusedLabelColor = HighlightBlue,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = TextLight
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { triggerCustomMorse() }
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { triggerCustomMorse() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HighlightBlue),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null, tint = BackgroundDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SİNYAL GÖNDER", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        if (isSending) {
                            Button(
                                onClick = { stopTransmission() },
                                modifier = Modifier.height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("DURDUR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Transmission Terminal Display Card
            if (isSending) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BackgroundDark),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SİNYAL AKTİF",
                            color = HighlightBlue,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Flashing Visualizer Circle
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(if (isTorchOn || isSignalPulseOn) HighlightBlue else SurfaceDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WifiTethering,
                                contentDescription = null,
                                tint = if (isTorchOn || isSignalPulseOn) BackgroundDark else TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "İletilen Harf:",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (currentCharacter == ' ') "(Boşluk / Bekleme)" else currentCharacter.toString(),
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ----------------------------------------------------
// TAB 4: RENKLİ EKRAN (SCREEN LIGHT MODES)
// ----------------------------------------------------
@Composable
fun ScreenLightControlScreen(
    onTurnOn: (Color) -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit
) {
    var selectedColor by remember { mutableStateOf(Color.White) }

    val presetColors = listOf(
        Triple("Kar Beyaz", Color.White, Color(0xFFE2E8F0)),
        Triple("Sıcak Sarı", Color(0xFFFFECB3), Color(0xFFFFD54F)),
        Triple("Kızıl Kırmızı", Color(0xFFFFCDD2), Color(0xFFEF5350)),
        Triple("Fıstık Yeşili", Color(0xFFC8E6C9), Color(0xFF66BB6A)),
        Triple("Gök Mavisi", Color(0xFFB3E5FC), Color(0xFF29B6F6)),
        Triple("Neon Pembe", Color(0xFFF8BBD0), Color(0xFFEC407A))
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Card explaining feature
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Ekran Işığı / Gece Lambası",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cihazın ekranını seçtiğiniz renkte tam parlaklığa ayarlayarak soft bir aydınlatma sağlar.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Custom Brightness slider
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ekran Parlaklığı",
                            color = TextLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "%${(brightness * 100).toInt()}",
                            color = HighlightBlue,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                    Slider(
                        value = brightness,
                        onValueChange = onBrightnessChange,
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = HighlightBlue,
                            activeTrackColor = HighlightBlue,
                            inactiveTrackColor = BorderDark
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Color Selector Grid
            Text(
                text = "Bir Renk Seçin ve Başlatın",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presetColors.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { (label, rawColor, displayColor) ->
                            val isSelected = selectedColor == rawColor
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(76.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(displayColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) HighlightBlue else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        selectedColor = rawColor
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Start full screen button
            Button(
                onClick = { onTurnOn(selectedColor) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("start_screen_light_button"),
                colors = ButtonDefaults.buttonColors(containerColor = HighlightBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LightMode,
                    contentDescription = null,
                    tint = BackgroundDark
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EKRAN IŞIĞINI BAŞLAT",
                    color = BackgroundDark,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ----------------------------------------------------
// TAB 5: EĞLENCE (FUN PARTY MODES SCREEN)
// ----------------------------------------------------
@Composable
fun FunModesScreen(
    onPoliceStart: () -> Unit,
    onDiscoStart: () -> Unit,
    discoSpeed: Long,
    onDiscoSpeedChange: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Option 1: Police light
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocalPolice,
                            contentDescription = null,
                            tint = HighlightBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Polis Sireni Sinyali",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ekranı kırmızı ve mavi polis tepe lambası şeklinde yüksek hızda flaşörler.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onPoliceStart,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = HighlightBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("SİRENİ BAŞLAT", color = BackgroundDark, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option 2: Disco light
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = HighlightBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Disko Modu",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Canlı renk geçişleriyle disko ışıklandırması simülasyonu.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Speed controller
                    Text(
                        text = "Renk Değişim Hızı: ${discoSpeed}ms",
                        color = TextLight,
                        fontSize = 11.sp
                    )
                    Slider(
                        value = discoSpeed.toFloat(),
                        onValueChange = { onDiscoSpeedChange(it.toLong()) },
                        valueRange = 50f..600f,
                        colors = SliderDefaults.colors(
                            thumbColor = HighlightBlue,
                            activeTrackColor = HighlightBlue,
                            inactiveTrackColor = BorderDark
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onDiscoStart,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = HighlightBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("DİSKO IŞIKLARINI BAŞLAT", color = BackgroundDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ----------------------------------------------------
// TAB 6: HAKKINDA (ABOUT SCREEN WITH THE REQ DETAILS)
// ----------------------------------------------------
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Contact info requested explicitly by user:
    val phone = "+905426971582"
    val email = "cezali.1genc@gmail.com"
    val web = "http://www.modlu-1apk.gt.tc"

    fun callSerhat() {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Arama başarısız oldu.", Toast.LENGTH_SHORT).show()
        }
    }

    fun emailSerhat() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
                putExtra(Intent.EXTRA_SUBJECT, "Flaşör Pro Hakkında")
            }
            context.startActivity(Intent.createChooser(intent, "E-posta gönder..."))
        } catch (e: Exception) {
            Toast.makeText(context, "E-posta uygulaması bulunamadı.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWebsite() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(web))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Web sayfası açılamadı.", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(text: String, label: String) {
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, "$label kopyalandı!", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))

            // Developer Avatar/Logo Visual Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Visual Initials Badge
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(HighlightBlue, BorderMedium)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SG",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = BackgroundDark
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "SG",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Bay Serhat 04",
                        color = HighlightBlue,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Yazılım Geliştirici & Tasarımcı",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Official requested Contact channels
            Text(
                text = "İletişim & Destek",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Phone Row Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { callSerhat() },
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BorderDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = null,
                                tint = HighlightBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Telefon Numarası", color = TextMuted, fontSize = 10.sp)
                            Text(phone, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    IconButton(onClick = { copyToClipboard(phone, "Telefon") }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = HighlightBlue, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Email Row Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { emailSerhat() },
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BorderDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = null,
                                tint = HighlightBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("E-posta Adresi", color = TextMuted, fontSize = 10.sp)
                            Text(email, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    IconButton(onClick = { copyToClipboard(email, "E-posta") }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = HighlightBlue, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Web URL Row Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openWebsite() },
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BorderDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = null,
                                tint = HighlightBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Modlu APK Portalı", color = TextMuted, fontSize = 10.sp)
                            Text("www.modlu-1apk.gt.tc", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    IconButton(onClick = { copyToClipboard(web, "Web adresi") }) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = null, tint = HighlightBlue, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Versiyon 4.0 © 2026",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ----------------------------------------------------
// FULLSCREEN MODE OVERLAYS
// ----------------------------------------------------
@Composable
fun FullscreenScreenLight(
    color: Color,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .padding(bottom = 48.dp)
                .padding(horizontal = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Kapatmak için ekrana dokunun",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 20.dp)
            )
        }
    }
}

@Composable
fun FullscreenPoliceLight(
    onClose: () -> Unit
) {
    var isRed by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            isRed = !isRed
            delay(140L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isRed) Color(0xFFD32F2F) else Color(0xFF1976D2))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.LocalPolice,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "POLİS SİNYALİ - KAPATMAK İÇİN DOKUNUN",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun FullscreenDiscoLight(
    speedMs: Long,
    onClose: () -> Unit
) {
    val colors = listOf(
        Color(0xFFFF3B30), // Red
        Color(0xFFFFCC00), // Yellow
        Color(0xFF34C759), // Green
        Color(0xFF5AC8FA), // Cyan
        Color(0xFF5856D6), // Indigo
        Color(0xFFFF2D55), // Pink
        Color(0xFFAF52DE), // Purple
        Color(0xFFFF9500)  // Orange
    )
    var colorIndex by remember { mutableStateOf(0) }

    LaunchedEffect(speedMs) {
        while (true) {
            colorIndex = (colorIndex + 1) % colors.size
            delay(speedMs)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors[colorIndex])
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.size(110.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "DİSKO MODU - KAPATMAK İÇİN DOKUNUN",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 20.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ----------------------------------------------------
// BOTTOM BAR NAVIGATION
// ----------------------------------------------------
@Composable
fun FlasorBottomBar(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    val items = listOf(
        NavigationItem("Flaşör", Icons.Filled.FlashlightOn, Icons.Outlined.FlashlightOn),
        NavigationItem("Stroboskop", Icons.Filled.FilterTiltShift, Icons.Outlined.FilterTiltShift),
        NavigationItem("Mors", Icons.Filled.RecordVoiceOver, Icons.Outlined.RecordVoiceOver),
        NavigationItem("Ekran Işığı", Icons.Filled.Tv, Icons.Outlined.Tv),
        NavigationItem("Eğlence", Icons.Filled.MusicVideo, Icons.Outlined.MusicVideo),
        NavigationItem("Hakkında", Icons.Filled.Info, Icons.Outlined.Info)
    )

    NavigationBar(
        containerColor = SurfaceDark,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        items.forEach { item ->
            val isSelected = activeTab == item.label
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.label) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = if (isSelected) HighlightBlue else TextMuted
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = if (isSelected) Color.White else TextMuted,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = HighlightBlue.copy(alpha = 0.15f)
                ),
                modifier = Modifier.testTag("nav_item_${item.label.lowercase()}")
            )
        }
    }
}

data class NavigationItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
