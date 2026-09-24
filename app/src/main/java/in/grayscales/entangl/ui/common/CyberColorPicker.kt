package `in`.grayscales.entangl.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.ui.theme.ColorUtils
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.DarkMatterVariant
import `in`.grayscales.entangl.ui.theme.IsotopeMagenta
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumGreen
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CyberColorPicker(
    selectedHex: String,
    onColorChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    previewInitials: String = "ID"
) {
    val focusManager = LocalFocusManager.current

    val initialColor = remember(selectedHex) {
        ColorUtils.parseColorOrDefault(selectedHex)
    }

    val (initHue, initSat, initVal) = remember(initialColor) {
        ColorUtils.colorToHsv(initialColor)
    }

    var hue by remember { mutableFloatStateOf(initHue) }
    var saturation by remember { mutableFloatStateOf(initSat.coerceIn(0.2f, 1f)) }
    var brightness by remember { mutableFloatStateOf(initVal.coerceIn(0.4f, 1f)) }

    var hexInputText by remember { mutableStateOf(selectedHex.removePrefix("#").uppercase()) }
    var isInputValid by remember { mutableStateOf(true) }

    // Keep internal sliders in sync when external selectedHex changes
    LaunchedEffect(selectedHex) {
        val color = ColorUtils.parseColorOrNull(selectedHex)
        if (color != null) {
            val (h, s, v) = ColorUtils.colorToHsv(color)
            hue = h
            saturation = s.coerceIn(0.2f, 1f)
            brightness = v.coerceIn(0.4f, 1f)
            val formatted = selectedHex.removePrefix("#").uppercase()
            if (hexInputText != formatted) {
                hexInputText = formatted
                isInputValid = true
            }
        }
    }

    val currentColor = remember(hue, saturation, brightness) {
        ColorUtils.hsvToColor(hue, saturation, brightness)
    }

    val currentHex = remember(currentColor) {
        ColorUtils.colorToHex(currentColor)
    }

    val rainbowBrush = remember {
        Brush.horizontalGradient(
            listOf(
                Color(0xFFFF0000),
                Color(0xFFFFFF00),
                Color(0xFF00FF00),
                Color(0xFF00FFFF),
                Color(0xFF0000FF),
                Color(0xFFFF00FF),
                Color(0xFFFF0000)
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkMatter)
            .border(1.dp, ParticleBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top section: Live Avatar & Hex Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular Preview Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(currentColor.copy(alpha = 0.18f))
                        .border(2.dp, currentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewInitials.isNotBlank() && previewInitials.length <= 3) {
                        Text(
                            text = previewInitials.take(2).uppercase(),
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = currentColor
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar Preview",
                            tint = currentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "AVATAR TINT",
                        fontFamily = QuantumMonospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SubatomicGray,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = currentHex,
                        fontFamily = QuantumMonospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentColor
                    )
                }
            }

            // Color Indicator Capsule
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkMatterVariant)
                    .border(1.dp, currentColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                    )
                    Text(
                        text = "SELECTED",
                        fontFamily = QuantumMonospace,
                        fontSize = 9.sp,
                        color = NeutronWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Direct HEX code input field
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "HEX COLOR CODE",
                fontFamily = QuantumMonospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SubatomicGray,
                letterSpacing = 1.sp
            )

            OutlinedTextField(
                value = hexInputText,
                onValueChange = { input ->
                    val clean = input.trim().removePrefix("#").take(6)
                    hexInputText = clean.uppercase()
                    if (clean.length == 6 && ColorUtils.isValidHexColor(clean)) {
                        isInputValid = true
                        val newColor = ColorUtils.parseColorOrDefault(clean)
                        val (h, s, v) = ColorUtils.colorToHsv(newColor)
                        hue = h
                        saturation = s.coerceIn(0.2f, 1f)
                        brightness = v.coerceIn(0.4f, 1f)
                        onColorChanged("#${clean.uppercase()}")
                    } else {
                        isInputValid = clean.length < 6 || ColorUtils.isValidHexColor(clean)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "00F0FF",
                        fontFamily = QuantumMonospace,
                        fontSize = 13.sp,
                        color = SubatomicGray
                    )
                },
                leadingIcon = {
                    Text(
                        text = "#",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isInputValid) currentColor else IsotopeMagenta,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                trailingIcon = {
                    if (isInputValid && hexInputText.length == 6) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Valid Hex",
                            tint = QuantumGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (ColorUtils.isValidHexColor(hexInputText)) {
                            onColorChanged("#${hexInputText.uppercase()}")
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isInputValid) currentColor else IsotopeMagenta,
                    unfocusedBorderColor = if (isInputValid) ParticleBorder else IsotopeMagenta.copy(alpha = 0.7f),
                    cursorColor = currentColor,
                    focusedTextColor = NeutronWhite,
                    unfocusedTextColor = NeutronWhite,
                    focusedContainerColor = DarkMatterVariant,
                    unfocusedContainerColor = DarkMatterVariant
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Text(
                text = if (isInputValid) {
                    if (hexInputText.length == 6) "Valid Quantum 24-bit RGB Hex" else "Enter 6 hexadecimal characters (0-9, A-F)"
                } else {
                    "Invalid Hex format — expected 6 hex digits"
                },
                fontFamily = QuantumMonospace,
                fontSize = 9.sp,
                color = if (!isInputValid) IsotopeMagenta else if (hexInputText.length == 6) QuantumGreen else SubatomicGray
            )
        }

        // Hue Slider
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "HUE SPECTRUM",
                    fontFamily = QuantumMonospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SubatomicGray,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${hue.toInt()}°",
                    fontFamily = QuantumMonospace,
                    fontSize = 10.sp,
                    color = currentColor,
                    fontWeight = FontWeight.Bold
                )
            }

            // Hue gradient bar track behind slider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(rainbowBrush)
            )

            Slider(
                value = hue,
                onValueChange = { newHue ->
                    hue = newHue
                    val updatedColor = ColorUtils.hsvToColor(hue, saturation, brightness)
                    val newHex = ColorUtils.colorToHex(updatedColor)
                    hexInputText = newHex.removePrefix("#")
                    isInputValid = true
                    onColorChanged(newHex)
                },
                valueRange = 0f..360f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = currentColor,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }

        // Saturation Slider
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SATURATION",
                    fontFamily = QuantumMonospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SubatomicGray,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${(saturation * 100).toInt()}%",
                    fontFamily = QuantumMonospace,
                    fontSize = 10.sp,
                    color = currentColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = saturation,
                onValueChange = { newSat ->
                    saturation = newSat
                    val updatedColor = ColorUtils.hsvToColor(hue, saturation, brightness)
                    val newHex = ColorUtils.colorToHex(updatedColor)
                    hexInputText = newHex.removePrefix("#")
                    isInputValid = true
                    onColorChanged(newHex)
                },
                valueRange = 0.2f..1f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = currentColor,
                    activeTrackColor = currentColor,
                    inactiveTrackColor = ParticleBorder
                )
            )
        }

        // Quick Quantum Presets
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = QuantumCyan,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "QUANTUM PRESETS",
                    fontFamily = QuantumMonospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SubatomicGray,
                    letterSpacing = 1.sp
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ColorUtils.PRESETS.forEach { preset ->
                    val isSelected = currentHex.equals(preset.hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(preset.color)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) NeutronWhite else ParticleBorder,
                                shape = CircleShape
                            )
                            .clickable {
                                val (h, s, v) = ColorUtils.colorToHsv(preset.color)
                                hue = h
                                saturation = s.coerceIn(0.2f, 1f)
                                brightness = v.coerceIn(0.4f, 1f)
                                hexInputText = preset.hex.removePrefix("#")
                                isInputValid = true
                                onColorChanged(preset.hex)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = if (preset.hex == "#FFE600") Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
