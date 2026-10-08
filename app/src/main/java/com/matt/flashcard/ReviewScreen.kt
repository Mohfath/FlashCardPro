package com.matt.flashcard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun ReviewScreen(ui: ReviewUi?, onBack: () -> Unit, onPlay: (String) -> Unit, onReveal: () -> Unit, onAnswer: (Verdict) -> Unit, onDelete: () -> Unit, onToggleNative: () -> Unit = {}, onRedoAudio: () -> Unit = {}) {
    val c = LocalFlashColors.current
    Box(Modifier.fillMaxSize().background(c.canvas).safeDrawingPadding()) {
        when {
            ui == null -> Unit
            ui.cardId == null -> FinishedState(ui.deckName, onBack)
            else -> {
                val scroll = rememberScrollState()
                val swipe = remember { CardSwipe() }
                val scope = rememberCoroutineScope()
                val answerAnimated: (Verdict) -> Unit = { v ->
                    swipe.flyOut(if (v == Verdict.KNOW) swipe.width * 1.3f else -swipe.width * 1.3f, 0f, scope) { onAnswer(v) }
                }
                val deleteAnimated: () -> Unit = {
                    swipe.flyOut(0f, swipe.height * 1.1f, scope) { repeat(if (ui.pendingDelete) 1 else 2) { onDelete() } }
                }
                Column(
                    Modifier.fillMaxSize().onSizeChanged { swipe.width = it.width.toFloat(); swipe.height = it.height.toFloat() }
                        .swipeGestures(swipe, scope, ui.revealed, { scroll.value == 0 }, answerAnimated, deleteAnimated)
                        .padding(horizontal = 20.dp),
                ) {
                    // The target-language side speaks as soon as it appears.
                    LaunchedEffect(ui.cardId, ui.promptAudio) { ui.promptAudio?.let(onPlay) }
                    LaunchedEffect(ui.cardId, ui.revealed) { if (ui.revealed) ui.answerAudio?.let(onPlay) }
                    CompactHeader(ui, onBack, onToggleNative)
                    Spacer(Modifier.height(8.dp))
                    CardFace(
                        ui, onPlay, onReveal, scroll, swipe,
                        Modifier.weight(1f).graphicsLayer {
                            translationX = swipe.x
                            translationY = swipe.y
                            rotationZ = if (swipe.width > 0f) swipe.x / swipe.width * 12f else 0f
                            scaleX = swipe.enter
                            scaleY = swipe.enter
                            alpha = swipe.enter * (1f - (abs(swipe.y) / swipe.height.coerceAtLeast(1f)).coerceAtMost(1f) * 0.6f)
                        },
                    )
                    Spacer(Modifier.height(16.dp))
                    Dock(ui, onReveal, answerAnimated, onDelete, onRedoAudio)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

private val KnowTint = Color(0xFF22C55E)
private val DidntKnowTint = Color(0xFFF472B6)
private val DeleteTint = Color(0xFF3B82F6)

/** Shades the Card green (right), pink (left) or blue with a diamond mesh (down) as it is dragged. */
private fun DrawScope.drawSwipeTint(swipe: CardSwipe) {
    val full = (swipe.width * 0.3f).coerceAtLeast(1f)
    if (abs(swipe.x) >= abs(swipe.y)) {
        val p = (abs(swipe.x) / full).coerceIn(0f, 1f)
        drawRect((if (swipe.x > 0) KnowTint else DidntKnowTint).copy(alpha = p * 0.4f))
    } else {
        val p = (swipe.y / full).coerceIn(0f, 1f)
        drawRect(DeleteTint.copy(alpha = p * 0.35f))
        val step = 28.dp.toPx()
        val line = Color.White.copy(alpha = p * 0.4f)
        var i = -size.height
        while (i < size.width + size.height) {
            drawLine(line, Offset(i, 0f), Offset(i + size.height, size.height), strokeWidth = 1.5.dp.toPx())
            drawLine(line, Offset(i + size.height, 0f), Offset(i, size.height), strokeWidth = 1.5.dp.toPx())
            i += step
        }
    }
}

/** Drag offset and fly-out/spring-back animation of the Card being swiped. */
private class CardSwipe {
    var x by mutableFloatStateOf(0f)
    var y by mutableFloatStateOf(0f)
    var enter by mutableFloatStateOf(1f)
    var width = 0f
    var height = 0f
    var flying by mutableStateOf(false)
    private var job: Job? = null

    /** Slides the Card off to (tx, ty), runs [done] (which swaps in the next Card), then eases the next one in. */
    fun flyOut(tx: Float, ty: Float, scope: CoroutineScope, done: () -> Unit) {
        if (flying) return
        flying = true
        job?.cancel()
        job = scope.launch {
            val sx = x
            val sy = y
            animate(0f, 1f, animationSpec = tween(220)) { p, _ -> x = sx + (tx - sx) * p; y = sy + (ty - sy) * p }
            done()
            x = 0f
            y = 0f
            animate(0.9f, 1f, animationSpec = tween(180)) { v, _ -> enter = v }
            flying = false
        }
    }

    fun springBack(scope: CoroutineScope) {
        if (flying || (x == 0f && y == 0f)) return
        job?.cancel()
        job = scope.launch {
            val sx = x
            val sy = y
            animate(0f, 1f, animationSpec = spring(stiffness = Spring.StiffnessMedium)) { p, _ -> x = sx * (1 - p); y = sy * (1 - p) }
        }
    }
}

/**
 * Drag the Card right = Know, left = Didn't know (once revealed); down deletes at once, but only while the
 * Card is scrolled to the top so a downward drag on a long Card still scrolls it. Watches without consuming.
 */
private fun Modifier.swipeGestures(
    swipe: CardSwipe,
    scope: CoroutineScope,
    revealed: Boolean,
    atTop: () -> Boolean,
    onAnswer: (Verdict) -> Unit,
    onDelete: () -> Unit,
): Modifier = composed {
    val revealedNow by rememberUpdatedState(revealed)
    val atTopNow by rememberUpdatedState(atTop)
    val answer by rememberUpdatedState(onAnswer)
    val delete by rememberUpdatedState(onDelete)
    pointerInput(Unit) {
        val threshold = 120.dp.toPx()
        val slop = 8.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (swipe.flying) return@awaitEachGesture
            val startedAtTop = atTopNow()
            var dx = 0f
            var dy = 0f
            var horizontal: Boolean? = null
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                dx += change.positionChange().x
                dy += change.positionChange().y
                if (horizontal == null && maxOf(abs(dx), abs(dy)) > slop) horizontal = abs(dx) > abs(dy)
                if (horizontal == true && revealedNow) {
                    swipe.x = dx
                    swipe.y = 0f
                } else if (horizontal == false && startedAtTop && atTopNow()) {
                    swipe.x = 0f
                    swipe.y = dy.coerceAtLeast(0f)
                }
                if (!change.pressed) break
            }
            when {
                horizontal == true && revealedNow && abs(dx) > threshold -> answer(if (dx > 0) Verdict.KNOW else Verdict.DIDNT_KNOW)
                horizontal == false && startedAtTop && atTopNow() && dy > threshold -> delete()
                else -> swipe.springBack(scope)
            }
        }
    }
}

@Composable
private fun CompactHeader(ui: ReviewUi, onBack: () -> Unit, onToggleNative: () -> Unit) {
    val c = LocalFlashColors.current
    Row(Modifier.fillMaxWidth().height(44.dp).padding(end = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = c.ink,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onBack).padding(8.dp).size(24.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(ui.deckName, style = FlashType.labelLg, color = c.ink, maxLines = 1, modifier = Modifier.weight(1f))
        if (ui.promptRomanization != null || ui.answerRomanization != null) {
            Text(
                if (ui.hideNative) "Aa  Latin only" else "Aa  + script", style = FlashType.labelMd, color = c.accent,
                modifier = Modifier.clip(CircleShape).background(c.accent.copy(alpha = 0.1f)).clickable(onClick = onToggleNative)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text("${ui.dueRemaining} due", style = FlashType.labelMd, color = c.accent)
    }
}

@Composable
private fun CardFace(ui: ReviewUi, onPlay: (String) -> Unit, onReveal: () -> Unit, scroll: ScrollState, swipe: CardSwipe, modifier: Modifier) {
    val c = LocalFlashColors.current
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier.fillMaxWidth().padding(horizontal = 4.dp)
            .shadow(12.dp, shape).clip(shape).background(c.surface).border(1.dp, c.divider, shape)
            .drawWithContent { drawContent(); drawSwipeTint(swipe) },
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("#${ui.cardId}")
            Spacer(Modifier.width(8.dp))
            Pill("Box ${ui.box}")
            Spacer(Modifier.weight(1f))
            if (ui.extraPractice) ExtraPill() else Pill("Due", dot = c.accent, tint = true)
        }
        BoxWithConstraints(
            Modifier.weight(1f).fillMaxWidth().clickable(
                enabled = !ui.revealed, indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onReveal,
            ),
        ) {
            val minHeight = maxHeight
            Column(Modifier.fillMaxWidth().verticalScroll(scroll)) {
                Column(
                    Modifier.fillMaxWidth().heightIn(min = minHeight).padding(horizontal = 28.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SideLabel("PROMPT · ${ui.promptLanguage}")
                    Spacer(Modifier.height(12.dp))
                    CardText(ui.promptText, c.ink, ui.promptAudio, onPlay, ui.promptRomanization, ui.hideNative)
                    if (ui.revealed) {
                        Spacer(Modifier.height(24.dp))
                        Box(Modifier.fillMaxWidth(0.4f).height(1.dp).background(c.divider))
                        Spacer(Modifier.height(24.dp))
                        SideLabel("ANSWER · ${ui.answerLanguage}")
                        Spacer(Modifier.height(12.dp))
                        CardText(ui.answerText, c.accent, ui.answerAudio, onPlay, ui.answerRomanization, ui.hideNative)
                        if (!ui.example.isNullOrBlank()) {
                            Spacer(Modifier.height(24.dp))
                            SideLabel("EXAMPLE")
                            Spacer(Modifier.height(8.dp))
                            Text(ui.example, style = FlashType.bodyMd, color = c.inkSecondary, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SideLabel(label: String) {
    val c = LocalFlashColors.current
    Text(label, style = FlashType.labelMd, color = c.inkSecondary)
}

/** The Card text, with a speaker beside it to replay the audio when this side is the language being learned. */
@Composable
private fun CardText(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    audio: String?,
    onPlay: (String) -> Unit,
    romanization: String? = null,
    hideNative: Boolean = false,
) {
    val c = LocalFlashColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Column(Modifier.weight(1f, fill = false), horizontalAlignment = Alignment.CenterHorizontally) {
            if (romanization != null && hideNative) {
                Text(romanization, style = FlashType.bodyXl, color = color, textAlign = TextAlign.Center)
            } else {
                Text(text, style = FlashType.bodyXl, color = color, textAlign = TextAlign.Center)
                if (romanization != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(romanization, style = FlashType.bodyMd, color = c.inkSecondary, textAlign = TextAlign.Center)
                }
            }
        }
        if (audio != null) {
            Text(
                "🔊", style = FlashType.headlineSm,
                modifier = Modifier.padding(start = 8.dp).clip(CircleShape).clickable { onPlay(audio) }.padding(8.dp),
            )
        }
    }
}

@Composable
private fun Dock(ui: ReviewUi, onReveal: () -> Unit, onAnswer: (Verdict) -> Unit, onDelete: () -> Unit, onRedoAudio: () -> Unit) {
    val c = LocalFlashColors.current
    if (!ui.revealed) {
        DockButton("Reveal", c.accent, c.onAccent, Modifier.fillMaxWidth(), onReveal)
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DockButton("Didn't know", c.inset, c.ink, Modifier.weight(1f)) { onAnswer(Verdict.DIDNT_KNOW) }
            DockButton("Know", c.accent, c.onAccent, Modifier.weight(1f)) { onAnswer(Verdict.KNOW) }
        }
    }
    if (ui.audioError != null) {
        Spacer(Modifier.height(6.dp))
        Text(ui.audioError, style = FlashType.labelMd, color = c.danger, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Text(
            if (ui.redoingAudio) "Making audio…" else "Redo audio",
            style = FlashType.labelLg, color = c.inkSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(enabled = !ui.redoingAudio, onClick = onRedoAudio)
                .padding(vertical = 12.dp),
        )
        Text(
            if (ui.pendingDelete) "Press again to delete" else "Delete",
            style = FlashType.labelLg, color = c.danger, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onDelete).padding(vertical = 12.dp),
        )
    }
}

@Composable
internal fun DockButton(label: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(56.dp).clip(RoundedCornerShape(16.dp)).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = FlashType.headlineSm, color = fg) }
}

@Composable
internal fun Pill(text: String, dot: androidx.compose.ui.graphics.Color? = null, tint: Boolean = false) {
    val c = LocalFlashColors.current
    Row(
        Modifier.clip(CircleShape).background(c.inset).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = FlashType.labelMd, color = if (tint) c.accent else c.inkSecondary)
    }
}

@Composable
internal fun ExtraPill() {
    val c = LocalFlashColors.current
    Text(
        "EXTRA PRACTICE", style = FlashType.labelMd, color = c.extraText, textAlign = TextAlign.Center, maxLines = 1, softWrap = false,
        modifier = Modifier.clip(CircleShape).background(c.extraBg)
            .border(BorderStroke(1.dp, c.extraBorder), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun FinishedState(deckName: String, onBack: () -> Unit) {
    val c = LocalFlashColors.current
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("All done for now", style = FlashType.headlineMd, color = c.ink)
        Spacer(Modifier.height(8.dp))
        Text("No Cards left in $deckName.", style = FlashType.bodyMd, color = c.inkSecondary)
        Spacer(Modifier.height(24.dp))
        DockButton("Back to Decks", c.accent, c.onAccent, Modifier.fillMaxWidth(), onBack)
    }
}
