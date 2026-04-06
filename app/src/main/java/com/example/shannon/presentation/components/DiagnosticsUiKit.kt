package com.example.shannon.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class DiagnosticsChipTone {
    Positive,
    Caution,
    Negative,
    Neutral,
}

@Composable
fun DiagnosticsSectionSurface(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        tonalElevation = 2.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun DiagnosticsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun DiagnosticsStatusChip(
    text: String,
    tone: DiagnosticsChipTone,
    modifier: Modifier = Modifier,
) {
    val semanticStyle = tone.style()

    SuggestionChip(
        onClick = {},
        enabled = false,
        modifier = modifier,
        border = null,
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = semanticStyle.containerColor,
            labelColor = semanticStyle.contentColor,
            disabledContainerColor = semanticStyle.containerColor,
            disabledLabelColor = semanticStyle.contentColor,
        ),
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
            )
        },
    )
}

@Composable
fun DiagnosticPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
fun DiagnosticSecondaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        ),
    ) {
        Text(text = text)
    }
}

@Composable
private fun DiagnosticsChipTone.style(): DiagnosticsChipStyle = when (this) {
    DiagnosticsChipTone.Positive -> DiagnosticsChipStyle(
        containerColor = if (isSystemInDarkTheme()) Color(0xFF173526) else Color(0xFFE5F6EA),
        contentColor = if (isSystemInDarkTheme()) Color(0xFF8DD8A8) else Color(0xFF1D6B3B),
    )
    DiagnosticsChipTone.Caution -> DiagnosticsChipStyle(
        containerColor = if (isSystemInDarkTheme()) Color(0xFF3A2B17) else Color(0xFFFFF0D9),
        contentColor = if (isSystemInDarkTheme()) Color(0xFFF2C27B) else Color(0xFF9A5E00),
    )
    DiagnosticsChipTone.Negative -> DiagnosticsChipStyle(
        containerColor = if (isSystemInDarkTheme()) Color(0xFF3F1D1E) else Color(0xFFFDE8E6),
        contentColor = if (isSystemInDarkTheme()) Color(0xFFF2B8B5) else Color(0xFFB3261E),
    )
    DiagnosticsChipTone.Neutral -> DiagnosticsChipStyle(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private data class DiagnosticsChipStyle(
    val containerColor: Color,
    val contentColor: Color,
)
