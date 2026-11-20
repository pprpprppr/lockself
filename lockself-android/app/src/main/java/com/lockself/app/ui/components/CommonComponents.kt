package com.lockself.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockself.app.data.model.FullRecord
import com.lockself.app.data.model.RecordType
import com.lockself.app.ui.theme.ListAccent
import com.lockself.app.ui.theme.NoteAccent
import com.lockself.app.ui.theme.PasswordAccent
import com.lockself.app.ui.theme.StrengthGreen
import com.lockself.app.ui.theme.StrengthOrange
import com.lockself.app.ui.theme.StrengthRed
import com.lockself.app.ui.theme.StrengthYellow
import com.lockself.app.util.PasswordStrengthMeter

/**
 * BlackBerry Password Keeper Continuous Spectrum Gradient Strength Meter with Sliding Notch
 * Implements the exact visual behavior of `PasswordSecurityField` (res/layout/password_field.xml)
 */
@Composable
fun ContinuousStrengthMeter(
    score: Int,
    warning: PasswordStrengthMeter.WarningReason = PasswordStrengthMeter.WarningReason.NONE,
    modifier: Modifier = Modifier
) {
    val animatedScore by animateFloatAsState(
        targetValue = score.coerceIn(0, 100).toFloat(),
        animationSpec = tween(durationMillis = 250),
        label = "notchAnimation"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val totalWidth = maxWidth
            val notchSize = 12.dp
            val travelWidth = totalWidth - notchSize
            val notchOffset = travelWidth * (animatedScore / 100f)

            // Spectrum Track (Red -> Orange -> Yellow -> Green)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(StrengthRed, StrengthOrange, StrengthYellow, StrengthGreen)
                        )
                    )
            )

            // Animated Sliding Notch
            Box(
                modifier = Modifier
                    .offset(x = notchOffset)
                    .size(notchSize)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }

        if (warning != PasswordStrengthMeter.WarningReason.NONE) {
            val text = when (warning) {
                PasswordStrengthMeter.WarningReason.TOO_SHORT -> "Password is too short (min 6 characters)"
                PasswordStrengthMeter.WarningReason.TOO_COMMON -> "Password is too common"
                else -> ""
            }
            Text(
                text = text,
                color = StrengthRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * BlackBerry Password Keeper Signature Record Card
 * Features the signature 5dp vertical accent strip:
 * - CrimsonRed (#D12B58) for Passwords
 * - Cyan (#00B0D4) for Notes
 * - EmeraldGreen (#46A66C) for Checklists
 */
@Composable
fun RecordCardItem(
    record: FullRecord,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stripColor = when (record.type) {
        RecordType.PASSWORD -> PasswordAccent
        RecordType.NOTE -> NoteAccent
        RecordType.LIST -> ListAccent
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 5dp Signature Accent Strip
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(72.dp)
                    .background(stripColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val subtitle = record.computeSubtitle()
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (record.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (record.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun CategoryBadge(count: Int) {
    if (count > 0) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
