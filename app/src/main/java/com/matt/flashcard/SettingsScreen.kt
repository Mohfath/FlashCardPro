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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(settings: AppSettings, onBack: () -> Unit) {
    val c = LocalFlashColors.current
    var provider by remember { mutableStateOf(settings.provider()) }
    Box(Modifier.fillMaxSize().background(c.canvas).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(onBack, "Settings")
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Keys are saved only on this phone and sent only to the service they belong to.",
                    style = FlashType.labelLg, color = c.inkSecondary,
                )

                Section("AI FOR LATIN LETTERS", "Writes how a card sounds in Latin letters. Pick the service you have a key for.") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AiProvider.entries.forEach { p ->
                            Choice(p.label, p == provider, true, { provider = p; settings.setProvider(p) }, Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    KeyField(
                        key = "ai_${provider.name}", label = "${provider.label} API key", help = provider.keyHelp,
                        initial = settings.own("ai_${provider.name}"), onSave = { settings.setAiKey(provider, it) },
                        hasFallback = settings.aiKey(provider).isNotBlank() && settings.own("ai_${provider.name}").isBlank(),
                    )
                }

                Section("VOICES", "ElevenLabs speaks each card out loud.") {
                    KeyField(
                        key = "elevenlabs", label = "ElevenLabs API key", help = "Get a key at elevenlabs.io",
                        initial = settings.own("elevenlabs"), onSave = settings::setElevenLabsKey,
                        hasFallback = settings.elevenLabsKey().isNotBlank() && settings.own("elevenlabs").isBlank(),
                    )
                }

                Section("TRANSLATION", "DeepL fills in the other language when you add cards.") {
                    KeyField(
                        key = "deepl", label = "DeepL API key", help = "Get a free key at deepl.com/pro-api",
                        initial = settings.own("deepl"), onSave = settings::setDeepLKey,
                        hasFallback = settings.deepLKey().isNotBlank() && settings.own("deepl").isBlank(),
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, subtitle: String, content: @Composable () -> Unit) {
    val c = LocalFlashColors.current
    val shape = RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.divider, shape).padding(16.dp)) {
        Text(title, style = FlashType.labelMd, color = c.inkSecondary)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, style = FlashType.bodyMd, color = c.inkSecondary)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/** A masked key field that saves as you type. Remembers per [key], so switching AI provider shows that provider's key. */
@Composable
private fun KeyField(key: String, label: String, help: String, initial: String, onSave: (String) -> Unit, hasFallback: Boolean) {
    val c = LocalFlashColors.current
    var value by remember(key) { mutableStateOf(initial) }
    var shown by remember(key) { mutableStateOf(false) }
    Column {
        Text(label, style = FlashType.labelMd, color = c.ink)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.inset).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = { value = it; onSave(it) },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = FlashType.bodyMd.fontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = c.ink),
                    cursorBrush = SolidColor(c.accent),
                    visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (value.isEmpty()) Text(if (hasFallback) "Using the built-in developer key" else "Paste your key", style = FlashType.bodyMd, color = c.inkSecondary)
            }
            Spacer(Modifier.padding(start = 8.dp))
            Text(
                if (shown) "Hide" else "Show", style = FlashType.labelMd, color = c.accent,
                modifier = Modifier.clip(CircleShape).clickable { shown = !shown }.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(if (value.isNotBlank()) "Saved on this phone" else help, style = FlashType.labelSm, color = c.inkSecondary)
    }
}
