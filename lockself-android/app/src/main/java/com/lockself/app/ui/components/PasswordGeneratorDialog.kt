package com.lockself.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.lockself.app.util.PasswordGenerator
import com.lockself.app.util.PasswordStrengthMeter

@Composable
fun PasswordGeneratorDialog(
    initialLength: Int = 12,
    onPasswordAccepted: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var lengthSlider by remember { mutableFloatStateOf(initialLength.toFloat().coerceIn(4f, 16f)) }
    var includeLetters by remember { mutableStateOf(true) }
    var includeNumbers by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }

    var generatedPassword by remember {
        mutableStateOf(
            PasswordGenerator.generatePassword(
                length = initialLength,
                includeLetters = true,
                includeNumbers = true,
                includeSymbols = true
            )
        )
    }

    val strength = remember(generatedPassword) {
        PasswordStrengthMeter.evaluate(generatedPassword)
    }

    fun regenerate() {
        generatedPassword = PasswordGenerator.generatePassword(
            length = lengthSlider.toInt(),
            includeLetters = includeLetters,
            includeNumbers = includeNumbers,
            includeSymbols = includeSymbols
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Password Generator") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = generatedPassword,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace)
                    )
                    IconButton(onClick = { regenerate() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Regenerate")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                ContinuousStrengthMeter(score = strength.score, warning = strength.warning)

                Spacer(modifier = Modifier.height(16.dp))
                Text("Length: ${lengthSlider.toInt()}")
                Slider(
                    value = lengthSlider,
                    onValueChange = {
                        lengthSlider = it
                        regenerate()
                    },
                    valueRange = 4f..16f,
                    steps = 11
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeLetters,
                        onCheckedChange = {
                            includeLetters = it
                            regenerate()
                        }
                    )
                    Text("Letters (A-Z, a-z)")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeNumbers,
                        onCheckedChange = {
                            includeNumbers = it
                            regenerate()
                        }
                    )
                    Text("Numbers (0-9)")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeSymbols,
                        onCheckedChange = {
                            includeSymbols = it
                            regenerate()
                        }
                    )
                    Text("Symbols (!@#$)")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onPasswordAccepted(generatedPassword)
                    onDismiss()
                }
            ) {
                Text("Use Password")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
