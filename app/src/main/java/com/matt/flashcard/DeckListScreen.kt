package com.matt.flashcard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DeckListScreen(decks: List<DeckSummary>?, week: List<DayCount>, selectedId: Long, onSelect: (Long) -> Unit, onOpen: (Long) -> Unit, onAddCard: (Long) -> Unit, onCreate: (String, String, String, Boolean) -> Unit, onDeleteDeck: (Deck) -> Unit,
    backfill: Backfill?, onSetRomanize: (Deck, Boolean, Boolean) -> Unit, onDismissBackfill: () -> Unit,
) {
    val c = LocalFlashColors.current
    var creating by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<DeckSummary?>(null) }
    var askLatin by remember { mutableStateOf<DeckSummary?>(null) }

    Box(Modifier.fillMaxSize().background(c.canvas).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(24.dp))
            Text("Decks", style = FlashType.headlineLg, color = c.ink)
            if (!decks.isNullOrEmpty()) {
                val due = decks.sumOf { it.due }
                Text(
                    "${decks.size} Deck${if (decks.size == 1) "" else "s"} · $due Due Cards today",
                    style = FlashType.labelLg, color = c.accent,
                )
            }
            Spacer(Modifier.height(16.dp))
            when {
                decks == null -> Unit
                decks.isEmpty() -> EmptyState(Modifier.weight(1f))
                else -> {
                    val active = decks.firstOrNull { it.deck.id == selectedId } ?: decks.first()
                    WeekStrip(week)
                    Spacer(Modifier.height(12.dp))
                    DeckPicker(decks, active, onSelect)
                    Spacer(Modifier.height(16.dp))
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        DeckCard(active, onClick = { onOpen(active.deck.id) }, onAddCard = { onAddCard(active.deck.id) }, onDelete = { deleting = active },
                            backfill = backfill?.takeIf { it.deckId == active.deck.id }, onDismissBackfill = onDismissBackfill,
                            onLatin = { if (active.deck.romanize) onSetRomanize(active.deck, false, false) else askLatin = active },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            DockButton("+  New Deck", c.accent, c.onAccent, Modifier.fillMaxWidth()) { creating = true }
            Spacer(Modifier.height(16.dp))
        }
    }

    askLatin?.let { s ->
        val missing = s.total
        AlertDialog(
            modifier = Modifier.padding(horizontal = 28.dp),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { askLatin = null },
            title = { Text("Store Latin letters?") },
            text = {
                Text(
                    if (missing == 0) "New cards in \"${s.deck.name}\" will also get a Latin-letter reading."
                    else "New cards will get a Latin-letter reading. Also write one for the $missing card${if (missing == 1) "" else "s"} already in this deck? That makes $missing request${if (missing == 1) "" else "s"} to your AI service.",
                )
            },
            confirmButton = { TextButton(onClick = { onSetRomanize(s.deck, true, missing > 0); askLatin = null }) { Text(if (missing == 0) "Turn on" else "Turn on and fill in") } },
            dismissButton = {
                Row {
                    if (missing > 0) TextButton(onClick = { onSetRomanize(s.deck, true, false); askLatin = null }) { Text("New cards only") }
                    TextButton(onClick = { askLatin = null }) { Text("Cancel") }
                }
            },
        )
    }

    deleting?.let { s ->
        AlertDialog(
            modifier = Modifier.padding(horizontal = 28.dp),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { deleting = null },
            title = { Text("Delete deck?") },
            text = {
                Text(
                    "\"${s.deck.name}\" and its ${s.total} card${if (s.total == 1) "" else "s"} will be deleted for good.",
                )
            },
            confirmButton = { TextButton(onClick = { onDeleteDeck(s.deck); deleting = null }) { Text("Delete", color = c.danger) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }

    if (creating) NewDeckDialog(onDismiss = { creating = false }, onCreate = { n, s, t, r -> onCreate(n, s, t, r); creating = false })
}

@Composable
private fun DeckPicker(decks: List<DeckSummary>, active: DeckSummary, onSelect: (Long) -> Unit) {
    val c = LocalFlashColors.current
    var open by remember { mutableStateOf(false) }
    var fieldWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    Box {
        Row(
            Modifier.fillMaxWidth().onSizeChanged { fieldWidth = with(density) { it.width.toDp() } }.clip(RoundedCornerShape(16.dp)).background(c.surface).border(1.dp, c.divider, RoundedCornerShape(16.dp))
                .clickable { open = true }.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(active.deck.name, style = FlashType.headlineSm, color = c.ink, maxLines = 1, modifier = Modifier.weight(1f))
            if (active.due > 0) Text("${active.due} due", style = FlashType.labelMd, color = c.accent)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose deck", tint = c.inkSecondary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.width(fieldWidth), containerColor = c.surface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, c.divider)) {
            decks.forEach { d ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(d.deck.name, style = FlashType.bodyMd, color = c.ink, modifier = Modifier.weight(1f, fill = false))
                            if (d.due > 0) {
                                Spacer(Modifier.width(12.dp))
                                Text("${d.due} due", style = FlashType.labelMd, color = c.accent)
                            }
                        }
                    },
                    onClick = { onSelect(d.deck.id); open = false },
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    val c = LocalFlashColors.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No Decks yet", style = FlashType.headlineMd, color = c.ink)
        Spacer(Modifier.height(8.dp))
        Text("Create your first Deck to start adding Cards.", style = FlashType.bodyMd, color = c.inkSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DeckCard(
    s: DeckSummary, onClick: () -> Unit, onAddCard: () -> Unit, onDelete: () -> Unit,
    backfill: Backfill?, onDismissBackfill: () -> Unit, onLatin: () -> Unit,
) {
    val c = LocalFlashColors.current
    val canReview = s.total > 0
    val shape = RoundedCornerShape(24.dp)
    Box {
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.divider, shape)
            .clickable(enabled = canReview, onClick = onClick).padding(20.dp),
    ) {
        Box(Modifier.padding(end = 44.dp)) { Pill("${languageName(s.deck.targetLanguage)} ↔ ${languageName(s.deck.sourceLanguage)}") }
        Spacer(Modifier.height(12.dp))
        Text(s.deck.name, style = FlashType.headlineMd, color = c.ink)
        Text("${s.total} Card${if (s.total == 1) "" else "s"} total", style = FlashType.bodyMd, color = c.inkSecondary)
        Spacer(Modifier.height(16.dp))
        BoxBar(s.boxCounts)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            when {
                !canReview -> Text("No Cards yet", style = FlashType.labelLg, color = c.inkSecondary)
                s.due > 0 -> {
                    Pill("${s.due} Due", dot = c.accent, tint = true)
                    Spacer(Modifier.weight(1f))
                    ReviewLink("Tap to review", c.accent)
                }
                else -> {
                    ExtraPill()
                    Spacer(Modifier.weight(1f))
                    ReviewLink("Review", c.extraText)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "+  Add Card", style = FlashType.labelLg, color = c.accent,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onAddCard).padding(vertical = 10.dp, horizontal = 4.dp),
        )
        if (languageFor(s.deck.targetLanguage)?.latin == false) {
            Text(
                if (s.deck.romanize) "Latin letters: on · turn off" else "Latin letters: off · turn on",
                style = FlashType.labelLg, color = c.inkSecondary,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onLatin).padding(vertical = 10.dp, horizontal = 4.dp),
            )
        }
        if (backfill != null) {
            Text(
                when {
                    backfill.error != null -> "Stopped at ${backfill.done} of ${backfill.total}: ${backfill.error}"
                    backfill.finished -> "Latin letters added to ${backfill.done} card${if (backfill.done == 1) "" else "s"}."
                    else -> "Writing Latin letters… ${backfill.done} of ${backfill.total}"
                },
                style = FlashType.labelMd, color = if (backfill.error != null) c.danger else c.accent,
                modifier = Modifier.clickable(enabled = backfill.finished, onClick = onDismissBackfill).padding(horizontal = 4.dp),
            )
        }
    }
    TrashIcon(
        c.inkSecondary,
        Modifier.align(Alignment.TopEnd).padding(14.dp).size(34.dp).clip(CircleShape).background(c.inset)
            .clickable(onClick = onDelete).padding(8.dp),
    )
    }
}

/** A thin outlined bin, drawn so it matches the app's light line icons. */
@Composable
private fun TrashIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "Delete deck" }) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(Path().apply { moveTo(w * 0.12f, h * 0.24f); lineTo(w * 0.88f, h * 0.24f) }, color, style = stroke)
        drawPath(
            Path().apply { moveTo(w * 0.36f, h * 0.24f); lineTo(w * 0.36f, h * 0.12f); lineTo(w * 0.64f, h * 0.12f); lineTo(w * 0.64f, h * 0.24f) },
            color, style = stroke,
        )
        drawPath(
            Path().apply { moveTo(w * 0.22f, h * 0.24f); lineTo(w * 0.29f, h * 0.88f); lineTo(w * 0.71f, h * 0.88f); lineTo(w * 0.78f, h * 0.24f) },
            color, style = stroke,
        )
        drawPath(Path().apply { moveTo(w * 0.42f, h * 0.42f); lineTo(w * 0.42f, h * 0.70f) }, color, style = stroke)
        drawPath(Path().apply { moveTo(w * 0.58f, h * 0.42f); lineTo(w * 0.58f, h * 0.70f) }, color, style = stroke)
    }
}

@Composable
private fun ReviewLink(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = FlashType.labelLg, color = color)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun BoxBar(counts: List<Int>) {
    val c = LocalFlashColors.current
    val total = counts.sum()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.inset).padding(12.dp)) {
        val counted = "Box 1: ${counts.first()} · Box 5: ${counts.last()}"
        if (LocalDensity.current.fontScale > 1.3f) {
            // Big system fonts: stack the two lines instead of letting them squeeze each other.
            Text("Boxes 1–5", style = FlashType.labelMd, color = c.inkSecondary)
            Text(counted, style = FlashType.labelMd, color = c.inkSecondary)
        } else {
            Row {
                Text("Boxes 1–5", style = FlashType.labelMd, color = c.inkSecondary, modifier = Modifier.weight(1f))
                Text(counted, style = FlashType.labelMd, color = c.inkSecondary, maxLines = 1, softWrap = false)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(c.divider)) {
            if (total > 0) counts.forEachIndexed { i, n ->
                if (n > 0) Box(Modifier.weight(n.toFloat()).height(8.dp).background(c.accent.copy(alpha = 0.3f + 0.7f * i / 4f)))
            }
        }
    }
}

@Composable
private fun NewDeckDialog(onDismiss: () -> Unit, onCreate: (String, String, String, Boolean) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var source by rememberSaveable { mutableStateOf("en") }
    var target by rememberSaveable { mutableStateOf("hr") }
    var romanize by rememberSaveable { mutableStateOf(false) }
    val needsLatin = languageFor(target)?.latin == false
    AlertDialog(
        modifier = Modifier.padding(horizontal = 28.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = { Text("New Deck") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Deck name") }, singleLine = true)
                LanguagePicker("My language", source, { source = it })
                LanguagePicker("Language to Learn", target, { target = it })
                if (needsLatin) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { romanize = !romanize }.padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Also store in Latin letters", style = FlashType.labelLg, color = LocalFlashColors.current.ink)
                            Text("Adds how each card sounds, if you can't read the script.", style = FlashType.labelSm, color = LocalFlashColors.current.inkSecondary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(checked = romanize, onCheckedChange = { romanize = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, source, target, needsLatin && romanize) }, enabled = name.isNotBlank() && source != target) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun LanguagePicker(label: String, selected: String, onSelect: (String) -> Unit) {
    val c = LocalFlashColors.current
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = FlashType.labelMd, color = c.inkSecondary)
        Spacer(Modifier.height(4.dp))
        Box {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, c.divider, RoundedCornerShape(12.dp))
                    .clickable { open = true }.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LanguageLabel(languageFor(selected), Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = c.inkSecondary)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = c.surface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, c.divider)) {
                Languages.forEach { lang ->
                    DropdownMenuItem(text = { LanguageLabel(lang) }, onClick = { onSelect(lang.code); open = false })
                }
            }
        }
    }
}

@Composable
private fun LanguageLabel(lang: Language?, modifier: Modifier = Modifier) {
    val c = LocalFlashColors.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(lang?.flag.orEmpty(), fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(lang?.name.orEmpty(), style = FlashType.bodyMd, color = c.ink)
    }
}
