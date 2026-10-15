package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.model.SubscriptionState
import com.example.ui.theme.*

@Composable
fun SecurityPinDialog(
    title: String = "Security PIN Required",
    description: String = "Enter Master PIN to unlock and proceed:",
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val cleanDescription = remember(description) {
        description
            .replace("($SECURITY_MASTER_PIN)", "")
            .replace(SECURITY_MASTER_PIN, "")
            .replace(Regex("""\s*\(\d+\)\s*"""), " ")
            .replace("  ", " ")
            .trim()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BossGold.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, BossGold),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = BossGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    color = BossGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = cleanDescription,
                    color = BossText,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )
                OutlinedTextField(
                    value = pinText,
                    onValueChange = { input ->
                        if (input.length <= 6 && input.all { it.isDigit() }) {
                            pinText = input
                            isError = false
                        }
                    },
                    label = { Text("Master PIN", fontSize = 11.sp) },
                    placeholder = { Text("Enter PIN code", fontSize = 11.sp) },
                    singleLine = true,
                    isError = isError,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BossGold,
                        unfocusedBorderColor = BossCarbonBorder,
                        focusedTextColor = BossText,
                        unfocusedTextColor = BossText,
                        errorBorderColor = Color(0xFFEF4444)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text(
                        text = "Incorrect PIN code! Reset authorization denied.",
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanPin = pinText.trim().uppercase()
                    val isValid = cleanPin == SECURITY_MASTER_PIN ||
                        cleanPin in SubscriptionState.MASTER_PASSCODES
                    if (isValid) {
                        onSuccess()
                        onDismiss()
                    } else {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BossGold, contentColor = Color(0xFF161208)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("Verify & Reset", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = BossTextDim, fontSize = 12.sp)
            }
        },
        containerColor = BossCarbonPanel2,
        shape = RoundedCornerShape(12.dp)
    )
}
