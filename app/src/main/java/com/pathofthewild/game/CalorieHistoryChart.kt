package com.pathofthewild.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.max

/** A size-independent chart: it derives every coordinate from its actual Compose canvas. */
@Composable
internal fun CalorieHistoryChart(
    points: List<DailyCalorieTotal>,
    targetCalories: Int,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val targetColor = MaterialTheme.colorScheme.secondary

    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        val left = size.width * 0.06f
        val right = size.width * 0.98f
        val top = size.height * 0.08f
        val bottom = size.height * 0.92f
        val chartWidth = (right - left).coerceAtLeast(1f)
        val chartHeight = (bottom - top).coerceAtLeast(1f)

        drawLine(axisColor, Offset(left, top), Offset(left, bottom), strokeWidth = 1.5f)
        drawLine(axisColor, Offset(left, bottom), Offset(right, bottom), strokeWidth = 1.5f)

        if (points.isEmpty()) return@Canvas

        val maximum = max(
            1,
            max(targetCalories.coerceAtLeast(0), points.maxOfOrNull { it.calories } ?: 0)
        ).toFloat()

        fun yFor(calories: Int): Float = bottom -
            (calories.coerceAtLeast(0).toFloat() / maximum).coerceIn(0f, 1f) * chartHeight

        if (targetCalories > 0) {
            val targetY = yFor(targetCalories)
            drawLine(
                targetColor,
                Offset(left, targetY),
                Offset(right, targetY),
                strokeWidth = 2f
            )
        }

        if (points.size == 1) {
            val point = Offset(left + chartWidth / 2f, yFor(points.first().calories))
            drawCircle(lineColor, radius = 5f, center = point)
            return@Canvas
        }

        val path = Path()
        points.forEachIndexed { index, point ->
            val x = left + chartWidth * index.toFloat() / (points.lastIndex).toFloat()
            val y = yFor(point.calories)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
    }
}
