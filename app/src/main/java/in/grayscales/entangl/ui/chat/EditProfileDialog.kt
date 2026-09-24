package `in`.grayscales.entangl.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import `in`.grayscales.entangl.core.identity.NodeIdentityManager
import `in`.grayscales.entangl.ui.common.CyberColorPicker
import `in`.grayscales.entangl.ui.theme.ColorUtils
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.DarkMatterVariant
import `in`.grayscales.entangl.ui.theme.IsotopeMagenta
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray

@Composable
fun EditProfileDialog(
    currentUsername: String,
    currentColorHex: String,
    onDismissRequest: () -> Unit,
    onSave: (newUsername: String, newColorHex: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var usernameInput by remember { mutableStateOf(currentUsername) }
    var selectedColorHex by remember { mutableStateOf(currentColorHex.ifBlank { ColorUtils.DEFAULT_PROFILE_HEX }) }

    val maxChars = NodeIdentityManager.MAX_USERNAME_LENGTH
    val isNameValid = usernameInput.trim().isNotEmpty() && usernameInput.trim().length <= maxChars
    val isColorValid = ColorUtils.isValidHexColor(selectedColorHex)
    val isFormValid = isNameValid && isColorValid

    val parsedColor = remember(selectedColorHex) {
        ColorUtils.parseColorOrDefault(selectedColorHex)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkMatter)
                .border(1.dp, parsedColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = parsedColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "EDIT NODE PROFILE",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NeutronWhite,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = selectedColorHex,
                        fontFamily = QuantumMonospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = parsedColor
                    )
                }

                // Codename section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "CODENAME / HANDLE",
                        fontFamily = QuantumMonospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SubatomicGray,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { input ->
                            if (input.length <= maxChars) {
                                usernameInput = input
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "e.g. CipherNode",
                                fontFamily = QuantumMonospace,
                                fontSize = 13.sp,
                                color = SubatomicGray
                            )
                        },
                        singleLine = true,
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = parsedColor,
                            unfocusedBorderColor = ParticleBorder,
                            cursorColor = parsedColor,
                            focusedTextColor = NeutronWhite,
                            unfocusedTextColor = NeutronWhite,
                            focusedContainerColor = DarkMatterVariant,
                            unfocusedContainerColor = DarkMatterVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MAX $maxChars CHARS",
                            fontFamily = QuantumMonospace,
                            fontSize = 9.sp,
                            color = SubatomicGray
                        )
                        Text(
                            text = "${usernameInput.length}/$maxChars",
                            fontFamily = QuantumMonospace,
                            fontSize = 9.sp,
                            color = if (usernameInput.length == maxChars) IsotopeMagenta else parsedColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Color Picker Component
                CyberColorPicker(
                    selectedHex = selectedColorHex,
                    onColorChanged = { newHex ->
                        selectedColorHex = newHex
                    },
                    previewInitials = usernameInput.trim().ifBlank { "ID" }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SubatomicGray
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ParticleBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = " CANCEL",
                            fontFamily = QuantumMonospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            if (isFormValid) {
                                onSave(usernameInput.trim(), selectedColorHex)
                            }
                        },
                        enabled = isFormValid,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = parsedColor,
                            contentColor = if (selectedColorHex.equals("#FFE600", ignoreCase = true)) Color.Black else Color.Black,
                            disabledContainerColor = DarkMatterVariant,
                            disabledContentColor = SubatomicGray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = " APPLY",
                            fontFamily = QuantumMonospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
