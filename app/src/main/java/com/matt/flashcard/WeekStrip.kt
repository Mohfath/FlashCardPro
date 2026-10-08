package com.matt.flashcard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The last seven days: a filled circle with the number of Cards practiced, or an ice cube when the day was skipped. */
@Composable
fun WeekStrip(week: List<DayCount>, modifier: Modifier = Modifier) {
    val c = LocalFlashColors.current
    val shape = RoundedCornerShape(24.dp)
    val total = week.sumOf { it.count }
    Column(modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.divider, shape).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LAST 7 DAYS", style = FlashType.labelMd, color = c.inkSecondary, modifier = Modifier.weight(1f))
            Text(
                if (total == 0) "No practice yet" else "$total card${if (total == 1) "" else "s"}",
                style = FlashType.labelMd, color = if (total == 0) c.inkSecondary else c.accent,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            week.forEach { DayCircle(it, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun DayCircle(day: DayCount, modifier: Modifier) {
    val c = LocalFlashColors.current
    val practiced = day.count > 0
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(36.dp).clip(CircleShape)
                .background(if (practiced) c.accent else c.inset)
                .then(if (day.isToday) Modifier.border(2.dp, c.accent, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (practiced) {
                Text(
                    day.count.toString(), style = FlashType.labelLg, color = c.onAccent, textAlign = TextAlign.Center,
                    maxLines = 1, softWrap = false,
                )
            } else {
                Text("🧊", fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            day.letter, style = FlashType.labelSm, color = if (day.isToday) c.accent else c.inkSecondary,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}
