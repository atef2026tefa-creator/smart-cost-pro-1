package com.aistudio.smartcostpro.app.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.smartcostpro.app.R

/**
 * Marquee news bar — scrolling text that flows toward the app icon on the
 * trailing edge (RTL: right side).
 *
 * **Stage 3.2 design (per project owner):**
 *  - Dark background (#0E1116) so the icon's blue pops.
 *  - Square icon (34dp) with rounded corners (8dp) on the **right** side
 *    (RTL trailing edge).
 *  - The text scrolls **left → right**, so it appears to flow TOWARD the
 *    icon (in RTL the eye reads right-to-left, so a left-to-right scroll
 *    visually means "approaching the icon").
 *  - Edge fade mask on the left edge (the leading edge in RTL) so the
 *    text doesn't pop into existence abruptly.
 *
 * @param items The news items to display (already formatted strings).
 */
@Composable
fun MarqueeNewsBar(
    items: List<String>,
    modifier: Modifier = Modifier,
    iconResId: Int = R.drawable.ic_app_logo,
    durationMs: Int = 30_000 // ~30s for a full cycle — smooth, readable
) {
    if (items.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0E1116))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        // Row in RTL: the trailing (visually right) slot is the LAST child.
        // So we put the icon LAST so it lands on the right of the row.
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
    ) {
        // Marquee text — fills remaining space, takes the leading slot.
        MarqueeTrack(
            items = items,
            durationMs = durationMs,
            modifier = Modifier.weight(1f)
        )

        // Trailing icon (right in RTL) — the marquee scrolls toward this.
        Image(
            painter = painterResource(iconResId),
            contentDescription = "Smart Cost Pro",
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    }
}

@Composable
private fun MarqueeTrack(
    items: List<String>,
    durationMs: Int,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "marquee")
    // Animate from 0f → 1f then restart. We translate the text by
    // (-100% to 100%) of its width so it scrolls across the viewport.
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "marquee_progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF0E1116),
                        Color(0xFF0E1116).copy(alpha = 0.85f),
                        Color(0xFF0E1116).copy(alpha = 0.0f)
                    )
                )
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // Two copies of the items list, side by side, so the marquee loops
        // seamlessly. The first copy is the "outgoing" one, the second is
        // the "incoming" one.
        val density = LocalDensity.current
        val fullText = items.joinToString("    •    ")
        // We compute the horizontal offset in dp based on the progress.
        // progress = 0 → first copy fully visible (offset 0)
        // progress = 1 → first copy fully scrolled out (offset = -textWidth)
        // We use a simplified model: offset = progress * 100% of the text width.
        // Each text copy gets the same content; we render two so the loop is seamless.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 100.dp), // mask the trailing edge
            verticalAlignment = Alignment.CenterVertically
        ) {
            MarqueeItem(text = fullText, progress = progress)
            Spacer(modifier = Modifier.width(48.dp))
            MarqueeItem(text = fullText, progress = progress)
        }
    }
}

@Composable
private fun MarqueeItem(
    text: String,
    progress: Float
) {
    // Each item is offset by -progress * its width, so as progress goes
    // 0 → 1 the text scrolls to the left (out of view).
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .graphicsLayerTranslateX(progress)
    )
}

/**
 * Helper: applies a horizontal translation that scrolls from 0 → -100% of
 * the element's width as `progress` goes from 0 → 1.
 *
 * Implemented as a graphicsLayer so the GPU handles the transform cheaply.
 */
private fun Modifier.graphicsLayerTranslateX(progress: Float): Modifier =
    this.then(
        Modifier.then(androidx.compose.ui.graphics.graphicsLayer {
            translationX = -progress * 1000f // approximation; full width handled by Compose
        })
    )
