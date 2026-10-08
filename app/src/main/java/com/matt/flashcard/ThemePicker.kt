package com.matt.flashcard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** A small round button for the top corner. */
@Composable
fun CornerButton(symbol: String, description: String, onClick: () -> Unit) {
    val c = LocalFlashColors.current
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(c.surface).border(1.dp, c.divider, CircleShape)
            .semantics { contentDescription = description }.clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(symbol, style = FlashType.labelLg) }
}

/** Small corner button that opens a list of the app's themes. */
@Composable
fun ThemePicker(current: AppTheme, onSelect: (AppTheme) -> Unit, modifier: Modifier = Modifier) {
    val c = LocalFlashColors.current
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(c.surface).border(1.dp, c.divider, CircleShape).clickable { open = true },
            contentAlignment = Alignment.Center,
        ) { Text("🎨", style = FlashType.labelLg) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = c.surface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, c.divider)) {
            AppTheme.entries.forEach { t ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(18.dp).clip(CircleShape).background(t.colors.accent).border(2.dp, t.colors.canvas, CircleShape))
                            Spacer(Modifier.width(12.dp))
                            Text(t.label, style = if (t == current) FlashType.labelLg else FlashType.bodyMd, color = c.ink)
                        }
                    },
                    onClick = { onSelect(t); open = false },
                )
            }
        }
    }
}
