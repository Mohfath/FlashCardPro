package com.matt.flashcard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private val deckList: DeckListViewModel by viewModels()
    private val settings by lazy { AppSettings(this) }
    private val review: ReviewViewModel by viewModels()
    private val addCard: AddCardViewModel by viewModels()
    private val importer: ImportViewModel by viewModels()
    private val library: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = remember { getSharedPreferences("ui", MODE_PRIVATE) }
            var theme by remember { mutableStateOf(AppTheme.from(prefs.getString("theme", null))) }
            FlashTheme(theme) {
                var reviewDeckId by rememberSaveable { mutableStateOf<Long?>(null) }
                var addCardDeckId by rememberSaveable { mutableStateOf<Long?>(null) }
                var settingsOpen by rememberSaveable { mutableStateOf(false) }
                var importDeckId by rememberSaveable { mutableStateOf<Long?>(null) }
                var libraryDeckId by rememberSaveable { mutableStateOf<Long?>(null) }
                var helpOpen by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(reviewDeckId) { reviewDeckId?.let(review::open) ?: review.close() }
                LaunchedEffect(addCardDeckId) { addCardDeckId?.let(addCard::open) ?: addCard.close() }
                LaunchedEffect(importDeckId) { importDeckId?.let(importer::open) ?: importer.close() }
                LaunchedEffect(libraryDeckId) { libraryDeckId?.let(library::open) ?: library.close() }
                BackHandler(enabled = reviewDeckId != null || addCardDeckId != null || libraryDeckId != null || settingsOpen || helpOpen) {
                    if (settingsOpen) { settingsOpen = false; return@BackHandler }
                    if (helpOpen) { helpOpen = false; return@BackHandler }
                    if (libraryDeckId != null) { libraryDeckId = null; return@BackHandler }
                    // From the import page, back goes to Add Card; from there, back to the deck list.
                    if (importDeckId != null) importDeckId = null
                    else {
                        reviewDeckId = null
                        addCardDeckId = null
                    }
                }

                Box(Modifier.fillMaxSize()) {
                when {
                    settingsOpen -> SettingsScreen(settings, onBack = { settingsOpen = false })
                    helpOpen -> HelpScreen(onBack = { helpOpen = false })
                    reviewDeckId != null -> ReviewScreen(
                        ui = review.ui.collectAsState().value,
                        onBack = { reviewDeckId = null },
                        onPlay = review::play,
                        onReveal = review::reveal,
                        onAnswer = review::answer,
                        onDelete = review::delete,
                        onRedoAudio = review::redoAudio,
                    )
                    libraryDeckId != null -> LibraryScreen(
                        ui = library.ui.collectAsState().value,
                        onBack = { libraryDeckId = null },
                        onCopy = library::copy,
                        onDismissNote = library::dismissNote,
                    )
                    importDeckId != null -> ImportScreen(
                        ui = importer.ui.collectAsState().value,
                        onBack = { importDeckId = null },
                        onTextChange = importer::setText,
                        onFirstIsTarget = importer::setFirstIsTarget,
                        onStart = importer::start,
                        onCancel = importer::cancel,
                    )
                    addCardDeckId != null -> AddCardScreen(
                        ui = addCard.ui.collectAsState().value,
                        onBack = { addCardDeckId = null },
                        onSourceChange = addCard::setSource,
                        onTargetChange = addCard::setTarget,
                        onClear = addCard::clear,
                        onImport = { importDeckId = addCardDeckId },
                        onExampleChange = addCard::setExample,
                        onRomanizationChange = addCard::setRomanization,
                        onSave = { addCard.save { addCardDeckId = null } },
                    )
                    else -> DeckListScreen(
                        decks = deckList.decks.collectAsState().value,
                        week = deckList.week.collectAsState().value,
                        selectedId = deckList.selectedDeckId.collectAsState().value,
                        onSelect = deckList::select,
                        onOpen = { reviewDeckId = it },
                        onAddCard = { addCardDeckId = it },
                        onLibrary = { libraryDeckId = it },
                        onCreate = deckList::createDeck,
                        onDeleteDeck = deckList::deleteDeck,
                        onSetFormality = deckList::setFormality,
                        missingAudio = deckList.missingAudio.collectAsState().value,
                        onRedoMissingAudio = deckList::redoMissingAudio,
                        onDismissMissingAudio = deckList::dismissMissingAudio,
                        headerActions = {
                            ThemePicker(theme, { theme = it; prefs.edit().putString("theme", it.name).apply() })
                            CornerButton("⚙️", "Settings") { settingsOpen = true }
                            CornerButton("?", "Help") { helpOpen = true }
                        },
                    )
                }
                val onDeckList = !settingsOpen && !helpOpen && reviewDeckId == null && addCardDeckId == null && importDeckId == null && libraryDeckId == null
                if (!onDeckList) {
                    Row(
                        Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(top = 4.dp, end = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThemePicker(theme, { theme = it; prefs.edit().putString("theme", it.name).apply() })
                    }
                }
                }
            }
        }
    }
}
