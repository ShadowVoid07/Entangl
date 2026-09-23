package `in`.grayscales.entangl.ui.onboarding

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.core.identity.NodeIdentityManager
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.DarkMatterVariant
import `in`.grayscales.entangl.ui.theme.IsotopeMagenta
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray
import `in`.grayscales.entangl.ui.theme.VoidBackground

@Composable
fun UsernameSetupScreen(
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var usernameInput by remember { mutableStateOf("") }
    val maxChars = NodeIdentityManager.MAX_USERNAME_LENGTH
    val isValid = usernameInput.trim().isNotEmpty() && usernameInput.trim().length <= maxChars

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkMatter)
                .border(1.dp, ParticleBorder, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo header
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkMatterVariant)
                    .border(1.dp, QuantumCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = `in`.grayscales.entangl.R.drawable.ic_entangl_logo),
                    contentDescription = "Entangl Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Title
            Text(
                text = "CHOOSE YOUR CODENAME",
                fontFamily = QuantumMonospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 2.sp,
                color = QuantumCyan,
                textAlign = TextAlign.Center
            )

            // Description
            Text(
                text = "Set a handle to identify your node during mutual QR handshakes. Your codename is cryptographically signed and shared only with peers you scan.",
                fontFamily = QuantumMonospace,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = SubatomicGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Input field
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
                        text = "e.g. CipherShadow",
                        fontFamily = QuantumMonospace,
                        fontSize = 13.sp,
                        color = SubatomicGray
                    )
                },
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(
                    imeAction = if (isValid) ImeAction.Done else ImeAction.Default
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (isValid) {
                            onConfirm(usernameInput.trim())
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QuantumCyan,
                    unfocusedBorderColor = ParticleBorder,
                    cursorColor = QuantumCyan,
                    focusedTextColor = NeutronWhite,
                    unfocusedTextColor = NeutronWhite,
                    focusedContainerColor = DarkMatterVariant,
                    unfocusedContainerColor = DarkMatterVariant
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Character counter & info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "MAX $maxChars CHARACTERS",
                    fontFamily = QuantumMonospace,
                    fontSize = 9.sp,
                    color = SubatomicGray
                )
                Text(
                    text = "${usernameInput.length}/$maxChars",
                    fontFamily = QuantumMonospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (usernameInput.length == maxChars) IsotopeMagenta else QuantumCyan
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Confirm button
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(usernameInput.trim())
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = QuantumCyan,
                    contentColor = Color.Black,
                    disabledContainerColor = DarkMatterVariant,
                    disabledContentColor = SubatomicGray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "INITIALIZE IDENTITY",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
