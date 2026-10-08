package com.matt.flashcard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ImportScreen(
    ui: ImportUi?,
    onBack: () -> Unit,
    onTextChange: (String) -> Unit,
    onFirstIsTarget: (Boolean) -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    val c = LocalFlashColors.current
    BackHandler(enabled = ui?.running == true) { onCancel() }
    Box(Modifier.fillMaxSize().background(c.canvas).safeDrawingPadding()) {
        if (ui == null) return@Box
        val lines = remember(ui.text) { parseImportLines(ui.text) }
        Column(Modifier.fillMaxSize()) {
            TopBar(onBack, "Import Cards")
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) { DeckPill(ui.deckName) }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (ui.running || ui.finished) Progress(ui)

                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.surface).border(1.dp, c.divider, RoundedCornerShape(24.dp)).padding(16.dp)) {
                    Text("PASTE YOUR LIST · ONE CARD PER LINE", style = FlashType.labelMd, color = c.inkSecondary)
                    Spacer(Modifier.height(10.dp))
                    BasicTextField(
                        value = ui.text,
                        onValueChange = onTextChange,
                        enabled = !ui.running,
                        textStyle = TextStyle(fontFamily = FlashType.bodyMd.fontFamily, fontSize = 16.sp, lineHeight = 24.sp, color = c.ink),
                        cursorBrush = SolidColor(c.accent),
                        minLines = 8,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "A line can be one word or sentence, or both sides split by a tab, \" - \", \";\" or \"|\".",
                        style = FlashType.labelSm, color = c.inkSecondary,
                    )
                }

                Text("EACH LINE STARTS WITH", style = FlashType.labelMd, color = c.inkSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choice("${ui.sourceFlag} My language", !ui.firstIsTarget, !ui.running, { onFirstIsTarget(false) }, Modifier.weight(1f))
                    Choice("${ui.targetFlag} Language to learn", ui.firstIsTarget, !ui.running, { onFirstIsTarget(true) }, Modifier.weight(1f))
                }
                Text(
                    "Missing ${(if (ui.firstIsTarget) ui.sourceLabel else ui.targetLabel).lowercase().replaceFirstChar { it.uppercase() }} sides are translated for you.",
                    style = FlashType.labelSm, color = c.inkSecondary,
                )
                if (ui.note != null) Text(ui.note, style = FlashType.labelLg, color = c.danger)
            }
            if (ui.running) {
                ImportButton("Stop", true, onCancel, filled = false)
            } else {
                ImportButton(
                    if (lines.isEmpty()) "Import" else "Import ${lines.size} card${if (lines.size == 1) "" else "s"}",
                    lines.isNotEmpty(), onStart,
                )
            }
        }
    }
}

@Composable
private fun Progress(ui: ImportUi) {
    val c = LocalFlashColors.current
    val left = (ui.total - ui.done - ui.failed).coerceAtLeast(0)
    val shape = RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.divider, shape).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (ui.running) "Importing…" else "Finished", style = FlashType.headlineSm, color = c.ink,
                modifier = Modifier.weight(1f),
            )
            Text("${ui.done} / ${ui.total}", style = FlashType.headlineSm, color = c.accent)
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(c.inset)) {
            val fraction = if (ui.total == 0) 0f else (ui.done + ui.failed).toFloat() / ui.total
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp).clip(CircleShape).background(c.accent))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            buildString {
                append("$left left")
                if (ui.failed > 0) append(" · ${ui.failed} failed")
                if (ui.withoutAudio > 0) append(" · ${ui.withoutAudio} without audio")
                if (ui.withoutLatin > 0) append(" · ${ui.withoutLatin} without Latin letters")
            },
            style = FlashType.labelLg, color = c.inkSecondary,
        )
        if (ui.running && ui.current.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(ui.current, style = FlashType.bodyMd, color = c.inkSecondary, maxLines = 1)
        }
        if (ui.finished && ui.text.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("The lines that did not go in are still in the box below.", style = FlashType.labelSm, color = c.inkSecondary)
        }
    }
}

@Composable
internal fun Choice(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val c = LocalFlashColors.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.height(48.dp).clip(shape).background(if (selected) c.accent.copy(alpha = 0.12f) else c.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) c.accent else c.divider, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = FlashType.labelLg, color = if (selected) c.accent else c.ink, textAlign = TextAlign.Center) }
}

@Composable
private fun ImportButton(label: String, enabled: Boolean, onClick: () -> Unit, filled: Boolean = true) {
    val c = LocalFlashColors.current
    val shape = RoundedCornerShape(16.dp)
    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Box(
            Modifier.fillMaxWidth().height(56.dp).clip(shape)
                .background(if (!enabled) c.inset else if (filled) c.accent else c.surface)
                .border(if (filled) 0.dp else 1.dp, if (filled) c.accent else c.danger, shape)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label, style = FlashType.headlineSm,
                color = if (!enabled) c.inkSecondary else if (filled) c.onAccent else c.danger,
            )
        }
    }
}
