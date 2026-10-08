package com.matt.flashcard

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val EnterTint = Color(0xFF16A34A)
private val TranslateTint = Color(0xFF6366F1)
private val DiacriticKeys = listOf("č", "ć", "đ", "š", "ž")

@Composable
fun AddCardScreen(
    ui: AddCardUi?,
    onBack: () -> Unit,
    onSourceChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onExampleChange: (String) -> Unit,
    onRomanizationChange: (String) -> Unit,
    onClear: () -> Unit,
    onImport: () -> Unit,
    onSave: () -> Unit,
) {
    val c = LocalFlashColors.current
    Box(Modifier.fillMaxSize().background(c.canvas).safeDrawingPadding()) {
        if (ui == null) return@Box
        Column(Modifier.fillMaxSize()) {
            TopBar(onBack)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DeckPill(ui.deckName)
                Spacer(Modifier.weight(1f))
                BoxTag()
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FieldCard(
                    flag = ui.targetFlag, label = "LANGUAGE TO LEARN · ${ui.targetLabel}",
                    value = ui.target, onChange = onTargetChange, color = c.ink, size = 20.sp, lines = 3,
                    diacritics = ui.targetCode == "hr",
                )
                if (ui.romanize) {
                    FieldCard(
                        flag = "🔤", label = "LATIN LETTERS · HOW IT SOUNDS",
                        value = ui.romanization, onChange = onRomanizationChange, color = c.ink, size = 20.sp, lines = 2,
                        badge = if (ui.romanizing) "Romanizing…" else "Auto",
                    )
                }
                FieldCard(
                    flag = ui.sourceFlag, label = "MY LANGUAGE · ${ui.sourceLabel}",
                    value = ui.source, onChange = onSourceChange, color = c.ink, size = 20.sp, lines = 3,
                    badge = if (ui.translatingInto != null) "Translating…" else "Auto-translate",
                )
                FieldCard(
                    flag = "", label = "EXAMPLE · ${ui.targetLabel} (optional)",
                    value = ui.example, onChange = onExampleChange, color = c.inkSecondary, size = 15.sp, lines = 2,
                    subtle = true,
                )
                if (ui.error != null) Text(ui.error, style = FlashType.labelLg, color = c.danger)
                Text(
                    "Import a list instead", style = FlashType.labelLg, color = c.accent, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, c.divider, RoundedCornerShape(16.dp))
                        .clickable(onClick = onImport).padding(vertical = 14.dp),
                )
                if (ui.source.isNotEmpty() || ui.target.isNotEmpty() || ui.example.isNotEmpty()) {
                    Text(
                        "Clear", style = FlashType.labelLg, color = c.inkSecondary, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClear).padding(vertical = 10.dp),
                    )
                }
            }
            AddButton(
                when {
                    ui.savingAudio -> "Generating audio…"
                    ui.saveWithoutAudio -> "Add without audio"
                    else -> "Add Card"
                },
                ui.canSave, onSave,
            )
        }
    }
}

@Composable
internal fun TopBar(onBack: () -> Unit, title: String = "Add Card") {
    val c = LocalFlashColors.current
    Box(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.align(Alignment.CenterStart).size(40.dp).clip(CircleShape).background(c.surface)
                .border(1.dp, c.divider, CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = c.ink, modifier = Modifier.size(22.dp)) }
        Text(title, style = FlashType.headlineSm, color = c.ink)
    }
}

@Composable
internal fun DeckPill(name: String) {
    val c = LocalFlashColors.current
    Row(
        Modifier.clip(CircleShape).background(c.accent.copy(alpha = 0.1f)).border(1.dp, c.accent.copy(alpha = 0.25f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(c.accent))
        Spacer(Modifier.width(8.dp))
        Text("Deck: $name", style = FlashType.labelMd, color = c.accent, maxLines = 1)
    }
}

@Composable
private fun BoxTag() {
    Text(
        "Enters Box 1", style = FlashType.labelMd, color = EnterTint,
        modifier = Modifier.clip(CircleShape).background(EnterTint.copy(alpha = 0.12f))
            .border(1.dp, EnterTint.copy(alpha = 0.3f), CircleShape).padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** A labelled input card that lights up its border while focused, as in the Add Card design. */
@Composable
private fun FieldCard(
    flag: String,
    label: String,
    value: String,
    onChange: (String) -> Unit,
    color: Color,
    size: TextUnit,
    lines: Int,
    diacritics: Boolean = false,
    badge: String? = null,
    hint: String? = null,
    subtle: Boolean = false,
) {
    val c = LocalFlashColors.current
    var focused by remember { mutableStateOf(false) }
    var field by remember { mutableStateOf(TextFieldValue("")) }
    val focus = remember { FocusRequester() }
    // Text changed from outside (a translation arriving) wins; otherwise keep the cursor the user has.
    val shown = if (field.text == value) field else TextFieldValue(value, TextRange(value.length))
    val shape = RoundedCornerShape(24.dp)

    Column(
        Modifier.fillMaxWidth()
            .then(if (focused) Modifier.shadow(6.dp, shape, ambientColor = c.accent, spotColor = c.accent) else Modifier)
            .clip(shape).background(if (subtle && !focused) c.surface.copy(alpha = 0.7f) else c.surface)
            .border(if (focused) 2.dp else 1.dp, if (focused) c.accent else c.divider, shape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (flag.isNotEmpty()) {
                Text(flag, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
            }
            Text(label, style = FlashType.labelMd, color = c.inkSecondary, modifier = Modifier.weight(1f))
            if (badge != null) {
                Text(
                    badge, style = FlashType.labelMd, color = TranslateTint,
                    modifier = Modifier.clip(CircleShape).background(TranslateTint.copy(alpha = 0.1f))
                        .border(1.dp, TranslateTint.copy(alpha = 0.3f), CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            if (hint != null) Text(hint, style = FlashType.labelSm, color = c.inkSecondary)
        }
        Spacer(Modifier.height(10.dp))
        BasicTextField(
            value = shown,
            onValueChange = { field = it; if (it.text != value) onChange(it.text) },
            textStyle = TextStyle(
                fontFamily = FlashType.bodyMd.fontFamily, fontWeight = FontWeight.SemiBold,
                fontSize = size, lineHeight = size * 1.4f, color = color,
            ),
            cursorBrush = SolidColor(c.accent),
            minLines = lines,
            modifier = Modifier.fillMaxWidth().focusRequester(focus).onFocusChanged { focused = it.isFocused },
        )
        if (diacritics) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Diacritics", style = FlashType.labelMd, color = c.inkSecondary, maxLines = 1, softWrap = false, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DiacriticKeys.forEach { ch ->
                        Box(
                            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(c.inset)
                                .clickable {
                                    val sel = shown.selection
                                    val text = shown.text.replaceRange(sel.min, sel.max, ch)
                                    field = TextFieldValue(text, TextRange(sel.min + ch.length))
                                    onChange(text)
                                    focus.requestFocus()
                                },
                            contentAlignment = Alignment.Center,
                        ) { Text(ch, style = FlashType.labelLg, color = c.ink) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = LocalFlashColors.current
    val shape = RoundedCornerShape(16.dp)
    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().height(56.dp)
                .then(if (enabled) Modifier.shadow(10.dp, shape, ambientColor = c.accent, spotColor = c.accent) else Modifier)
                .clip(shape).background(if (enabled) c.accent else c.inset).clickable(enabled = enabled, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
        ) {
            if (enabled) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(label, style = FlashType.headlineSm, color = if (enabled) c.onAccent else c.inkSecondary)
        }
    }
}
