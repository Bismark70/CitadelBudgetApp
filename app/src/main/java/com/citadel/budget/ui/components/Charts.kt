package com.citadel.budget.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Donut chart used for the 50/30/20 split and report breakdowns. Pure Compose — no chart library. */
@Composable
fun DonutChart(
    values: List<Float>,
    colors: List<Color>,
    centerTitle: String,
    centerValue: String,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 20.dp
) {
    val total = values.sum()
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2 + 2.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            if (total <= 0f) {
                drawArc(
                    color = Color.Gray.copy(alpha = 0.25f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
                return@Canvas
            }
            var start = -90f
            values.forEachIndexed { i, v ->
                val sweep = v / total * 360f
                if (sweep > 0.01f) {
                    drawArc(
                        color = colors[i % colors.size],
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt)
                    )
                }
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                centerTitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                centerValue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Circular gauge for the Citadel Score (0-100). */
@Composable
fun ScoreGauge(
    score: Int,
    ringColor: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = Color.Gray.copy(alpha = 0.2f)
) {
    val animated by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "scoreGauge"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val inset = stroke / 2 + 2.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(ringColor.copy(alpha = 0.35f), ringColor, ringColor)),
                startAngle = 135f,
                sweepAngle = 270f * animated.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$score",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                "of 100",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Two-line trend chart (income vs money committed) for the last months. */
@Composable
fun TrendChart(
    income: List<Float>,
    outflow: List<Float>,
    labels: List<String>,
    incomeColor: Color,
    outflowColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            if (income.size < 2 || outflow.size < 2) return@Canvas
            val maxY = (income + outflow).maxOrNull()?.coerceAtLeast(1f) ?: 1f

            fun pointsFor(values: List<Float>): List<Offset> = values.mapIndexed { i, v ->
                Offset(
                    x = i * size.width / (values.size - 1),
                    y = size.height * (1f - (v / maxY).coerceIn(0f, 1f))
                )
            }

            fun pathFor(points: List<Offset>): Path {
                val p = Path()
                points.forEachIndexed { i, pt ->
                    if (i == 0) p.moveTo(pt.x, pt.y) else p.lineTo(pt.x, pt.y)
                }
                return p
            }

            val inc = pathFor(pointsFor(income))
            val out = pathFor(pointsFor(outflow))
            val outArea = Path().apply {
                addPath(out)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                outArea,
                Brush.verticalGradient(
                    listOf(outflowColor.copy(alpha = 0.25f), Color.Transparent)
                )
            )
            drawPath(out, outflowColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            drawPath(inc, incomeColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
