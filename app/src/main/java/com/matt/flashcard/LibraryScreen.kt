package com.matt.flashcard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The Library of ready-made English categories for one Deck. Three pages in one screen: the levels,
 * the categories of a level, and the Cards of a category with the Copy button. Back steps out one page.
 */
@Composable
fun LibraryScreen(ui: LibraryUi?, onBack: () -> Unit, onCopy: (LibraryCategory) -> Unit, onDismissNote: () -> Unit) {
    val c = LocalFlashColors.current
    var levelNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    val level = libraryLevels.firstOrNull { it.number == levelNumber }
    val category = level?.categories?.firstOrNull { it.id == categoryId }

    fun back() {
        when {
            category != null -> { categoryId = null; onDismissNote() }
            level != null -> levelNumber = null
            else -> onBack()
        }
    }
    BackHandler(enabled = level != null) { back() }

    Box(Modifier.fillMaxSize().background(c.canvas).safeDrawingPadding()) {
        if (ui == null) return@Box
        Column(Modifier.fillMaxSize()) {
            TopBar(::back, level?.let { "Level ${it.number}" } ?: "Library")
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) { DeckPill(ui.deckName) }
            when {
                category != null -> CategoryPage(category, ui, onCopy)
                level != null -> CategoriesPage(level, ui) { categoryId = it.id }
                else -> LevelsPage { levelNumber = it.number }
            }
        }
    }
}

@Composable
private fun ListRow(onClick: (() -> Unit)?, content: @Composable () -> Unit) {
    val c = LocalFlashColors.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.divider, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun ColumnScope.LevelsPage(onOpen: (LibraryLevel) -> Unit) {
    val c = LocalFlashColors.current
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        libraryLevels.forEach { level ->
            ListRow(if (level.ready) ({ onOpen(level) }) else null) {
                Column(Modifier.weight(1f)) {
                    Text("LEVEL ${level.number}", style = FlashType.labelMd, color = c.inkSecondary)
                    Text(level.title, style = FlashType.headlineSm, color = if (level.ready) c.ink else c.inkSecondary)
                }
                Text(
                    if (level.ready) "${level.categories.size} categories" else "Coming soon",
                    style = FlashType.labelLg, color = if (level.ready) c.accent else c.inkSecondary,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.CategoriesPage(level: LibraryLevel, ui: LibraryUi, onOpen: (LibraryCategory) -> Unit) {
    val c = LocalFlashColors.current
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(level.title.uppercase(), style = FlashType.labelMd, color = c.inkSecondary)
        level.categories.forEach { category ->
            ListRow({ onOpen(category) }) {
                Text(category.emoji, fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(category.name, style = FlashType.headlineSm, color = c.ink)
                    Text("${category.cards.size} cards", style = FlashType.labelMd, color = c.inkSecondary)
                }
                if (isAdded(category, ui.addedIds)) Text("Added ✓", style = FlashType.labelLg, color = c.accent)
            }
        }
    }
}

@Composable
private fun ColumnScope.CategoryPage(category: LibraryCategory, ui: LibraryUi, onCopy: (LibraryCategory) -> Unit) {
    val c = LocalFlashColors.current
    val missing = missingEntries(category, ui.addedIds).size
    val copying = ui.copyingCategoryId == category.id
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(category.emoji, fontSize = 28.sp)
            Spacer(Modifier.width(10.dp))
            Text(category.name, style = FlashType.headlineMd, color = c.ink)
        }
        Text(
            "Copied as ${ui.sourceFlag} ${ui.sourceLabel.lowercase().replaceFirstChar { it.uppercase() }} + ${ui.targetFlag} ${ui.targetLabel.lowercase().replaceFirstChar { it.uppercase() }}",
            style = FlashType.labelMd, color = c.inkSecondary,
        )
        Spacer(Modifier.padding(2.dp))
        category.cards.forEachIndexed { i, english ->
            val added = category.libraryId(i) in ui.addedIds
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(english, style = FlashType.bodyMd, color = c.ink, modifier = Modifier.weight(1f))
                if (added) Text("✓", style = FlashType.labelLg, color = c.accent)
            }
        }
        if (ui.note != null) {
            Spacer(Modifier.padding(4.dp))
            Text(ui.note, style = FlashType.labelLg, color = if (ui.noteIsError) c.danger else c.accent)
        }
    }
    ImportButton(
        when {
            copying -> "Copying…"
            missing == 0 -> "Added ✓"
            missing == category.cards.size -> "Copy ${category.cards.size} cards"
            else -> "Copy $missing missing card${if (missing == 1) "" else "s"}"
        },
        enabled = missing > 0 && ui.copyingCategoryId == null,
        onClick = { onCopy(category) },
    )
}
