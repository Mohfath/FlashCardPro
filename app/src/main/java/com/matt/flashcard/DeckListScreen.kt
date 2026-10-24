package com.matt.flashcard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.material3.RadioButton
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
fun DeckListScreen(decks: List<DeckSummary>?, week: List<DayCount>, selectedId: Long, onSelect: (Long) -> Unit, onOpen: (Long) -> Unit, onAddCard: (Long) -> Unit, onLibrary: (Long) -> Unit, onCreate: (String, String, String, ReadingMode, Formality) -> Unit, onDeleteDeck: (Deck) -> Unit,
    onSetFormality: (Deck, Formality) -> Unit,
    missingAudio: MissingAudio?, onRedoMissingAudio: (Deck) -> Unit, onDismissMissingAudio: () -> Unit,
    headerActions: @Composable RowScope.() -> Unit,
) {
    val c = LocalFlashColors.current
    var creating by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<DeckSummary?>(null) }
    var choosingTone by remember { mutableStateOf<DeckSummary?>(null) }
    var confirmingAudio by remember { mutableStateOf<DeckSummary?>(null) }

    Box(Modifier.fillMaxSize().background(c.canvas).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Flashcards", style = FlashType.headlineLg.copy(fontSize = 28.sp, lineHeight = 36.sp), color = c.ink, maxLines = 1, softWrap = false, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = headerActions)
            }
            Spacer(Modifier.height(10.dp))
            when {
                decks == null -> Unit
                decks.isEmpty() -> EmptyState(Modifier.weight(1f))
                else -> {
                    Box(Modifier.padding(horizontal = 20.dp)) { WeekStrip(week) }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("YOUR DECKS", style = FlashType.labelMd, color = c.inkSecondary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${decks.size} Deck${if (decks.size == 1) "" else "s"}", style = FlashType.labelMd, color = c.accent,
                            modifier = Modifier.clip(CircleShape).background(c.accent.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                        Spacer(Modifier.weight(1f))
                        if (decks.size > 1) Text("Swipe ↔", style = FlashType.labelMd, color = c.inkSecondary, maxLines = 1, softWrap = false)
                    }
                    Spacer(Modifier.height(10.dp))
                    DeckCarousel(
                        decks, selectedId, onSelect,
                        onOpen = onOpen, onAddCard = onAddCard, onLibrary = onLibrary,
                        onDelete = { deleting = it }, onTone = { choosingTone = it },
                        onRedoAudio = { confirmingAudio = it },
                        missingAudio = missingAudio, onDismissMissingAudio = onDismissMissingAudio,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            DockButton("+  New Deck", c.accent, c.onAccent, Modifier.fillMaxWidth().padding(horizontal = 20.dp)) { creating = true }
            Spacer(Modifier.height(16.dp))
        }
    }

    confirmingAudio?.let { s ->
        AlertDialog(
            modifier = Modifier.padding(horizontal = 28.dp),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { confirmingAudio = null },
            title = { Text("Redo missing audio") },
            text = {
                Text(
                    if (s.missingAudio == 0) "Every card in \"${s.deck.name}\" already has audio."
                    else "Make audio for the ${s.missingAudio} card${if (s.missingAudio == 1) "" else "s"} in \"${s.deck.name}\" that have none? That uses ${s.missingAudio} ElevenLabs request${if (s.missingAudio == 1) "" else "s"}.",
                )
            },
            confirmButton = {
                TextButton(onClick = { if (s.missingAudio > 0) onRedoMissingAudio(s.deck); confirmingAudio = null }) { Text(if (s.missingAudio > 0) "Make audio" else "OK") }
            },
            dismissButton = { if (s.missingAudio > 0) TextButton(onClick = { confirmingAudio = null }) { Text("Cancel") } },
        )
    }

    choosingTone?.let { s ->
        var tone by remember(s.deck.id) { mutableStateOf(s.deck.formality) }
        AlertDialog(
            modifier = Modifier.padding(horizontal = 28.dp),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { choosingTone = null },
            title = { Text("Tone of translations") },
            text = {
                Column {
                    Formality.entries.forEach { option ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { tone = option }.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = tone == option, onClick = { tone = option })
                            Text(option.label, style = FlashType.bodyMd, color = c.ink)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Applies to new translations. Cards you already have stay as they are.", style = FlashType.labelMd, color = c.inkSecondary)
                }
            },
            confirmButton = { TextButton(onClick = { onSetFormality(s.deck, tone); choosingTone = null }) { Text("Apply") } },
            dismissButton = { TextButton(onClick = { choosingTone = null }) { Text("Cancel") } },
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

    if (creating) NewDeckDialog(onDismiss = { creating = false }, onCreate = { n, s, t, m, f -> onCreate(n, s, t, m, f); creating = false })
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

/** The Decks as a row of cards: swipe sideways to switch, the next card peeks in at the edge. */
@Composable
private fun DeckCarousel(
    decks: List<DeckSummary>, selectedId: Long, onSelect: (Long) -> Unit,
    onOpen: (Long) -> Unit, onAddCard: (Long) -> Unit, onLibrary: (Long) -> Unit,
    onDelete: (DeckSummary) -> Unit, onTone: (DeckSummary) -> Unit, onRedoAudio: (DeckSummary) -> Unit,
    missingAudio: MissingAudio?, onDismissMissingAudio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalFlashColors.current
    val pager = rememberPagerState(initialPage = pageIndexOf(decks, selectedId)) { decks.size }
    val latestDecks by rememberUpdatedState(decks)
    val latestSelected by rememberUpdatedState(selectedId)
    // Swiping to a card selects that Deck...
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            latestDecks.getOrNull(page)?.let { if (it.deck.id != latestSelected) onSelect(it.deck.id) }
        }
    }
    // ...and a Deck selected some other way (new Deck, deleted Deck) brings its card into view.
    LaunchedEffect(selectedId, decks.size) {
        val target = pageIndexOf(decks, selectedId).coerceAtMost(decks.lastIndex)
        if (target != pager.currentPage) pager.animateScrollToPage(target)
    }
    Column(modifier) {
        HorizontalPager(
            pager, Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 20.dp, end = 36.dp), pageSpacing = 12.dp,
            verticalAlignment = Alignment.Top,
        ) { page ->
            val s = decks[page]
            DeckCard(
                s,
                onOpen = { onOpen(s.deck.id) }, onAddCard = { onAddCard(s.deck.id) }, onLibrary = { onLibrary(s.deck.id) },
                onDelete = { onDelete(s) }, onTone = { onTone(s) }, onRedoAudio = { onRedoAudio(s) },
                missingAudio = missingAudio?.takeIf { it.deckId == s.deck.id }, onDismissMissingAudio = onDismissMissingAudio,
            )
        }
        if (decks.size > 1) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                decks.forEachIndexed { i, _ ->
                    val active = i == pager.currentPage
                    Box(
                        Modifier.padding(horizontal = 3.dp).height(8.dp).width(if (active) 24.dp else 8.dp).clip(CircleShape)
                            .background(if (active) c.accent else c.accent.copy(alpha = 0.25f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun DeckCard(
    s: DeckSummary, onOpen: () -> Unit, onAddCard: () -> Unit, onLibrary: () -> Unit, onDelete: () -> Unit,
    onTone: () -> Unit, onRedoAudio: () -> Unit,
    missingAudio: MissingAudio?, onDismissMissingAudio: () -> Unit,
) {
    val c = LocalFlashColors.current
    val shape = RoundedCornerShape(32.dp)
    Column(
        Modifier.fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = c.accent.copy(alpha = 0.18f), spotColor = c.accent.copy(alpha = 0.18f))
            .clip(shape).background(c.surface).border(1.dp, c.divider, shape)
            .clickable(enabled = s.total > 0, onClick = onOpen).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LanguageBadge(s.deck)
            Spacer(Modifier.weight(1f))
            DeckMenu(s, onAddCard, onLibrary, onTone, onRedoAudio, onDelete)
        }
        Spacer(Modifier.height(16.dp))
        Text(s.deck.name, style = FlashType.headlineMd.copy(fontSize = 26.sp, lineHeight = 32.sp), color = c.ink, maxLines = 2)
        Text("${s.total} Card${if (s.total == 1) "" else "s"} total", style = FlashType.bodyMd, color = c.inkSecondary)
        Spacer(Modifier.height(18.dp))
        BoxBar(s.boxCounts)
        Spacer(Modifier.height(20.dp))
        ReviewButton(reviewAction(s.total, s.due), onOpen)
        if (missingAudio != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    missingAudio.error != null -> "Stopped at ${missingAudio.done} of ${missingAudio.total}: ${missingAudio.error}"
                    missingAudio.finished -> "Audio made for ${missingAudio.done} card${if (missingAudio.done == 1) "" else "s"}."
                    else -> "Making audio… ${missingAudio.done} of ${missingAudio.total}"
                },
                style = FlashType.labelMd, color = if (missingAudio.error != null) c.danger else c.accent,
                modifier = Modifier.clickable(enabled = missingAudio.finished, onClick = onDismissMissingAudio),
            )
        }
    }
}

/** "🇭🇷 HR → 🇬🇧 EN": the language being learned, then the learner's own. */
@Composable
private fun LanguageBadge(deck: Deck) {
    val c = LocalFlashColors.current
    fun part(code: String) = "${languageFor(code)?.flag.orEmpty()} ${code.uppercase()}".trim()
    Row(
        Modifier.clip(CircleShape).background(c.accent.copy(alpha = 0.1f)).border(1.dp, c.accent.copy(alpha = 0.25f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${part(deck.targetLanguage)}  →  ${part(deck.sourceLanguage)}", style = FlashType.labelLg, color = c.accent, maxLines = 1)
    }
}

@Composable
private fun ReviewButton(action: ReviewAction, onClick: () -> Unit) {
    val c = LocalFlashColors.current
    val shape = RoundedCornerShape(16.dp)
    val bg = when {
        !action.enabled -> c.inset
        action.extraPractice -> c.extraBg
        else -> c.accent
    }
    val fg = when {
        !action.enabled -> c.inkSecondary
        action.extraPractice -> c.extraText
        else -> c.onAccent
    }
    Box(
        Modifier.fillMaxWidth().height(56.dp).clip(shape).background(bg)
            .border(if (action.extraPractice) 1.dp else 0.dp, if (action.extraPractice) c.extraBorder else Color.Transparent, shape)
            .clickable(enabled = action.enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(action.label, style = FlashType.headlineSm, color = fg)
            if (action.enabled) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** The three-dot menu of a Deck card. */
@Composable
private fun DeckMenu(
    s: DeckSummary, onAddCard: () -> Unit, onLibrary: () -> Unit, onTone: () -> Unit, onRedoAudio: () -> Unit, onDelete: () -> Unit,
) {
    val c = LocalFlashColors.current
    var open by remember { mutableStateOf(false) }
    @Composable
    fun item(chip: String, label: String, color: Color = c.ink, chipBg: Color = c.accent.copy(alpha = 0.1f), onClick: () -> Unit) {
        DropdownMenuItem(
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(chipBg), contentAlignment = Alignment.Center) {
                        Text(chip, fontSize = 14.sp, color = color, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(label, style = FlashType.labelLg, color = color)
                }
            },
            onClick = { open = false; onClick() },
        )
    }
    Box {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(c.surface).border(1.dp, c.divider, CircleShape)
                .semantics { contentDescription = "Deck options" }.clickable { open = true },
            contentAlignment = Alignment.Center,
        ) { Text("⋮", style = FlashType.headlineSm, color = c.inkSecondary) }
        DropdownMenu(open, { open = false }, containerColor = c.surface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, c.divider)) {
            item("+", "Add Card", onClick = onAddCard)
            item("📚", "Deck Library", onClick = onLibrary)
            if (supportsFormality(s.deck.targetLanguage)) item("💬", "Tone…", onClick = onTone)
            item("🔊", "Redo Missing Audio", onClick = onRedoAudio)
            HorizontalDivider(color = c.divider)
            item("🗑", "Delete Deck", color = c.danger, chipBg = c.danger.copy(alpha = 0.1f), onClick = onDelete)
        }
    }
}

@Composable
private fun BoxBar(counts: List<Int>) {
    val c = LocalFlashColors.current
    val total = counts.sum()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.inset).padding(horizontal = 10.dp, vertical = 12.dp)) {
        val counted = "Box 1: ${counts.first()} · Box 5: ${counts.last()}"
        if (LocalDensity.current.fontScale > 1.3f) {
            // Big system fonts: stack the two lines instead of letting them squeeze each other.
            Text("Boxes 1–5", style = FlashType.labelMd, color = c.inkSecondary)
            Text(counted, style = FlashType.labelMd, color = c.inkSecondary)
        } else {
            Row {
                Text("Boxes 1–5", style = FlashType.labelMd, color = c.inkSecondary, maxLines = 1, softWrap = false)
                Spacer(Modifier.width(8.dp))
                Text(counted, style = FlashType.labelMd.copy(fontSize = 11.sp), color = c.inkSecondary, textAlign = TextAlign.End, maxLines = 1, softWrap = false, modifier = Modifier.weight(1f))
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
private fun NewDeckDialog(onDismiss: () -> Unit, onCreate: (String, String, String, ReadingMode, Formality) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var source by rememberSaveable { mutableStateOf("en") }
    var target by rememberSaveable { mutableStateOf("hr") }
    var mode by rememberSaveable { mutableStateOf(ReadingMode.SCRIPT) }
    var tone by rememberSaveable { mutableStateOf(Formality.INFORMAL) }
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
                if (needsLatin) ReadingModePicker(mode, { mode = it })
                if (supportsFormality(target)) TonePicker(tone, { tone = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, source, target, if (needsLatin) mode else ReadingMode.SCRIPT, if (supportsFormality(target)) tone else Formality.INFORMAL) }, enabled = name.isNotBlank() && source != target) { Text("Create") }
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

/** Dropdown for the tone DeepL uses when translating into the language being learned. */
@Composable
private fun TonePicker(selected: Formality, onSelect: (Formality) -> Unit) {
    val c = LocalFlashColors.current
    var open by remember { mutableStateOf(false) }
    Column {
        Text("Tone of translations", style = FlashType.labelMd, color = c.inkSecondary)
        Spacer(Modifier.height(4.dp))
        Box {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, c.divider, RoundedCornerShape(12.dp))
                    .clickable { open = true }.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(selected.label, style = FlashType.bodyMd, color = c.ink, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = c.inkSecondary)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = c.surface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, c.divider)) {
                Formality.entries.forEach { option ->
                    DropdownMenuItem(text = { Text(option.label, style = FlashType.bodyMd, color = c.ink) }, onClick = { onSelect(option); open = false })
                }
            }
        }
    }
}

/** Dropdown for how a new Deck shows the language being learned: script, Latin letters or both. */
@Composable
private fun ReadingModePicker(selected: ReadingMode, onSelect: (ReadingMode) -> Unit) {
    val c = LocalFlashColors.current
    var open by remember { mutableStateOf(false) }
    Column {
        Text("Alphabet to learn with", style = FlashType.labelMd, color = c.inkSecondary)
        Spacer(Modifier.height(4.dp))
        Box {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, c.divider, RoundedCornerShape(12.dp))
                    .clickable { open = true }.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(selected.label, style = FlashType.bodyMd, color = c.ink, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = c.inkSecondary)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = c.surface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, c.divider)) {
                ReadingMode.entries.forEach { option ->
                    DropdownMenuItem(text = { Text(option.label, style = FlashType.bodyMd, color = c.ink) }, onClick = { onSelect(option); open = false })
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
