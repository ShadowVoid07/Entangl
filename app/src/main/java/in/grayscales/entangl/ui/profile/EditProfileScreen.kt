package `in`.grayscales.entangl.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.core.identity.NodeIdentityManager
import `in`.grayscales.entangl.ui.common.CyberColorPicker
import `in`.grayscales.entangl.ui.theme.ColorUtils
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.DarkMatterVariant
import `in`.grayscales.entangl.ui.theme.IsotopeMagenta
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray
import `in`.grayscales.entangl.ui.theme.VoidBackground

/**
 * One-time Node Profile Initialization Screen.
 *
 * Serves as the mandatory first-launch onboarding step to establish the user's
 * cryptographic codename and quantum avatar color. Once confirmed, this profile
 * becomes permanently immutable.
 */
@Composable
fun EditProfileScreen(
    onConfirm: (username: String, profileColor: String) -> Unit,
    modifier: Modifier = Modifier,
    initialUsername: String = "",
    initialColorHex: String = ColorUtils.DEFAULT_PROFILE_HEX
) {
    var usernameInput by remember { mutableStateOf(initialUsername) }
    var selectedColorHex by remember { mutableStateOf(initialColorHex) }

    val focusManager = LocalFocusManager.current
    val maxChars = NodeIdentityManager.MAX_USERNAME_LENGTH
    val isNameValid = usernameInput.trim().isNotEmpty() && usernameInput.trim().length <= maxChars
    val isColorValid = ColorUtils.isValidHexColor(selectedColorHex)
    val isFormValid = isNameValid && isColorValid

    val parsedColor = remember(selectedColorHex) {
        ColorUtils.parseColorOrDefault(selectedColorHex, QuantumCyan)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBackground)
            .imePadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkMatter)
                .border(1.dp, ParticleBorder, RoundedCornerShape(16.dp))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkMatterVariant)
                    .border(1.5.dp, parsedColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = parsedColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Screen Title: INITIALIZE IDENTITY
            Text(
                text = "INITIALIZE IDENTITY",
                fontFamily = QuantumMonospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 2.sp,
                color = parsedColor,
                textAlign = TextAlign.Center
            )

            // Explanatory Subtext
            Text(
                text = "Establish your permanent peer identity. Your codename and avatar color are cryptographically bound to your hardware key and cannot be altered after initialization.",
                fontFamily = QuantumMonospace,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = SubatomicGray,
                textAlign = TextAlign.Center
            )

            // Codename Input Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CODENAME / HANDLE",
                        fontFamily = QuantumMonospace,
                        fontSize = 10.sp,
                        color = SubatomicGray
                    )
                    Text(
                        text = "${usernameInput.length}/$maxChars",
                        fontFamily = QuantumMonospace,
                        fontSize = 10.sp,
                        color = if (usernameInput.length > maxChars) IsotopeMagenta else SubatomicGray
                    )
                }

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
                            text = "Enter tactical codename...",
                            fontFamily = QuantumMonospace,
                            fontSize = 12.sp,
                            color = SubatomicGray.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = parsedColor,
                        unfocusedBorderColor = ParticleBorder,
                        focusedTextColor = NeutronWhite,
                        unfocusedTextColor = NeutronWhite,
                        cursorColor = parsedColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }

            // Avatar Color Palette Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "QUANTUM AVATAR COLOR",
                    fontFamily = QuantumMonospace,
                    fontSize = 10.sp,
                    color = SubatomicGray
                )

                CyberColorPicker(
                    selectedHex = selectedColorHex,
                    onColorChanged = { selectedColorHex = it },
                    previewInitials = usernameInput.take(2).uppercase().ifBlank { "ID" },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Primary Action: APPLY / SAVE
            Button(
                onClick = {
                    if (isFormValid) {
                        onConfirm(usernameInput.trim(), selectedColorHex)
                    }
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = DarkMatterVariant,
                    disabledContentColor = SubatomicGray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "APPLY",
                    fontFamily = QuantumMonospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
