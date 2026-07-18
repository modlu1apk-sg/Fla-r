package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.db.AppDatabase
import com.example.db.PresetEntity
import com.example.db.PresetRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationMenu(
    onDismiss: () -> Unit,
    onApply: (color: Color, speedHz: Float, pattern: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Initialize Room Database repository
    val repository = remember {
        PresetRepository(AppDatabase.getDatabase(context).presetDao())
    }

    // Collect saved presets
    val presets by repository.allPresets.collectAsState(initial = emptyList())

    // Selection States
    var selectedPattern by remember { mutableStateOf("Sürekli") }
    var selectedColorHex by remember { mutableStateOf("#D1E4FF") }
    var selectedSpeed by remember { mutableStateOf(5.0f) }
    var presetName by remember { mutableStateOf("") }

    // Color Palette Presets
    val colorPalette = listOf(
        "#FFFFFF" to "Saf Beyaz",
        "#D1E4FF" to "Sophisticated",
        "#EF5350" to "Acil Kırmızı",
        "#FFB300" to "Amber Sarı",
        "#66BB6A" to "Neon Yeşil",
        "#29B6F6" to "Okyanus Mavi",
        "#AB47BC" to "Disko Moru",
        "#EC407A" to "Canlı Pembe",
        "#FF7043" to "Turuncu Volkan"
    )

    // Patterns list with icons
    val patternsList = listOf(
        Triple("Sürekli", Icons.Filled.LightMode, "Düz Işık"),
        Triple("Strobe", Icons.Filled.Speed, "Hızlı Flaş"),
        Triple("S.O.S", Icons.Filled.Warning, "Acil Yardım"),
        Triple("Darbe", Icons.Filled.Favorite, "Kalp Atışı"),
        Triple("Polis", Icons.Filled.LocalPolice, "Tepe Lambası"),
        Triple("Disko", Icons.Filled.MusicNote, "Renk Şöleni")
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark),
            color = BackgroundDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header of Menu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ÖZELLEŞTİRME MERKEZİ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Gelişmiş Sinyal ve Flaşör Editörü",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceDark)
                            .border(1.dp, BorderDark, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Kapat",
                            tint = Color.White
                        )
                    }
                }

                // Inner scrollable content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Section 1: Pattern/Desens Selector
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, BorderDark),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "1. Işık Deseni Seçin",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 4.dp)
                                ) {
                                    items(patternsList) { (patternName, icon, label) ->
                                        val isSelected = selectedPattern == patternName
                                        Column(
                                            modifier = Modifier
                                                .width(90.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(if (isSelected) HighlightBlue else BackgroundDark)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) HighlightBlue else BorderDark,
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .clickable { selectedPattern = patternName }
                                                .padding(vertical = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = if (isSelected) BackgroundDark else HighlightBlue,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = patternName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) BackgroundDark else TextLight,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = label,
                                                fontSize = 9.sp,
                                                color = if (isSelected) BackgroundDark.copy(alpha = 0.7f) else TextMuted,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Speed / Frequency Controller
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, BorderDark),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "2. Yanıp Sönme Hızı",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${String.format("%.1f", selectedSpeed)} Hz",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = HighlightBlue
                                    )
                                }
                                Text(
                                    text = "Flaşların tetiklenme saniye frekansı (Sürekli modda devre dışıdır).",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                Slider(
                                    value = selectedSpeed,
                                    onValueChange = { selectedSpeed = it },
                                    valueRange = 0.5f..30.0f,
                                    enabled = selectedPattern != "Sürekli",
                                    colors = SliderDefaults.colors(
                                        thumbColor = HighlightBlue,
                                        activeTrackColor = HighlightBlue,
                                        inactiveTrackColor = BorderDark
                                    ),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                val speedHint = when {
                                    selectedSpeed < 3f -> "Yavaş ve Ritmik Sinyal"
                                    selectedSpeed < 10f -> "Standart Görünür Flaş"
                                    selectedSpeed < 20f -> "Agresif Stroboskop"
                                    else -> "Titreşimli Ekstrem Hız"
                                }
                                Text(
                                    text = if (selectedPattern == "Sürekli") "Sürekli Işık Aktif" else speedHint,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedPattern == "Sürekli") TextMuted else HighlightBlue,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }

                    // Section 3: Color Palette Picker
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, BorderDark),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "3. Işık Rengi",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = "Ekran ışığı veya renk geçişli modlar için özel aydınlatma rengi.",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // Grid of colors
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val chunks = colorPalette.chunked(3)
                                    chunks.forEach { rowColors ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowColors.forEach { (hex, name) ->
                                                val isSelected = selectedColorHex.lowercase() == hex.lowercase()
                                                val parsedColor = try {
                                                    Color(android.graphics.Color.parseColor(hex))
                                                } catch (e: Exception) {
                                                    Color.White
                                                }

                                                Row(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(BackgroundDark)
                                                        .border(
                                                            width = if (isSelected) 2.dp else 1.dp,
                                                            color = if (isSelected) HighlightBlue else BorderDark,
                                                            shape = RoundedCornerShape(12.dp)
                                                        )
                                                        .clickable { selectedColorHex = hex }
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                            .background(parsedColor)
                                                            .border(1.dp, BorderMedium, CircleShape)
                                                    )
                                                    Text(
                                                        text = name,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) HighlightBlue else TextLight,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Custom HEX text input
                                OutlinedTextField(
                                    value = selectedColorHex,
                                    onValueChange = {
                                        if (it.startsWith("#") && it.length <= 9) {
                                            selectedColorHex = it
                                        } else if (!it.startsWith("#") && it.length <= 8) {
                                            selectedColorHex = "#$it"
                                        }
                                    },
                                    label = { Text("Özel Renk HEX Kodu", color = TextMuted) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = HighlightBlue,
                                        unfocusedBorderColor = BorderDark,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = TextLight
                                    ),
                                    maxLines = 1,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Section 4: Save Custom Preset
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, BorderDark),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "4. Bu Kombinasyonu Ön Ayar Olarak Kaydet",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Text(
                                    text = "Ayarlarınızı özel bir isimle veritabanına kaydedip dilediğiniz an tek dokunuşla çağırın.",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = presetName,
                                        onValueChange = { presetName = it },
                                        placeholder = { Text("Örn: Kamp Lambası, SOS Kırmızı", color = TextMuted, fontSize = 12.sp) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = HighlightBlue,
                                            unfocusedBorderColor = BorderDark,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = TextLight
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Button(
                                        onClick = {
                                            if (presetName.isNotBlank()) {
                                                scope.launch {
                                                    repository.insert(
                                                        PresetEntity(
                                                            name = presetName.trim(),
                                                            colorHex = selectedColorHex,
                                                            speedHz = selectedSpeed,
                                                            pattern = selectedPattern
                                                        )
                                                    )
                                                    presetName = ""
                                                    Toast.makeText(context, "Ön ayar başarıyla kaydedildi!", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Lütfen ön ayar için bir isim girin!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = HighlightBlue),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(54.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Save,
                                            contentDescription = "Kaydet",
                                            tint = BackgroundDark
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 5: Saved Presets List (Room Database representation)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, BorderDark),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Kayıtlı Özel Ön Ayarlar (${presets.size})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = "Kaydettiğiniz özel sinyal profilleriniz:",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                if (presets.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Filled.CloudOff,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Kayıtlı ön ayar bulunamadı.",
                                                color = TextMuted,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        presets.forEach { preset ->
                                            val presetColor = try {
                                                Color(android.graphics.Color.parseColor(preset.colorHex))
                                            } catch (e: Exception) {
                                                Color.White
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(BackgroundDark)
                                                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                                                    .clickable {
                                                        // Load values from preset
                                                        selectedPattern = preset.pattern
                                                        selectedColorHex = preset.colorHex
                                                        selectedSpeed = preset.speedHz
                                                        Toast.makeText(context, "${preset.name} ön ayarı yüklendi!", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    // Color dot
                                                    Box(
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .clip(CircleShape)
                                                            .background(presetColor)
                                                            .border(1.dp, BorderMedium, CircleShape)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            text = preset.name,
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            SuggestionChip(
                                                                onClick = {},
                                                                label = { Text(preset.pattern, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                                    containerColor = SurfaceDark,
                                                                    labelColor = HighlightBlue
                                                                ),
                                                                border = BorderStroke(0.5.dp, BorderDark)
                                                            )
                                                            if (preset.pattern != "Sürekli") {
                                                                Text(
                                                                    text = "${String.format("%.1f", preset.speedHz)} Hz",
                                                                    color = TextMuted,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    // Quick Run button
                                                    IconButton(
                                                        onClick = {
                                                            onApply(presetColor, preset.speedHz, preset.pattern)
                                                            onDismiss()
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.PlayArrow,
                                                            contentDescription = "Hemen Uygula",
                                                            tint = HighlightBlue
                                                        )
                                                    }

                                                    // Delete button
                                                    IconButton(
                                                        onClick = {
                                                            scope.launch {
                                                                repository.delete(preset)
                                                                Toast.makeText(context, "${preset.name} silindi.", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Delete,
                                                            contentDescription = "Sil",
                                                            tint = Color(0xFFEF5350)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Button to immediately run the current customized selection
                Button(
                    onClick = {
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(selectedColorHex))
                        } catch (e: Exception) {
                            Color.White
                        }
                        onApply(parsedColor, selectedSpeed, selectedPattern)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .height(58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HighlightBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = "Sinyali Başlat",
                        tint = BackgroundDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ÖZEL SİNYALİ BAŞLAT",
                        color = BackgroundDark,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
