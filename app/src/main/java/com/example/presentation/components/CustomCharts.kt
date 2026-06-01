package com.example.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import java.util.Locale
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun MonthlyRevenueBarChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val entries = data.entries.toList().takeLast(6) // Take last 6 months
    val maxVal = (entries.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0)
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        Text(
            text = "Monthly Revenue Growth",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Text("No data available", color = labelColor, fontSize = 14.sp)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                entries.forEach { (month, rawValue) ->
                    val progress = (rawValue / maxVal).toFloat().coerceIn(0.05f, 1f)
                    val animatedHeightState = animateFloatAsState(
                        targetValue = progress,
                        animationSpec = tween(durationMillis = 1000),
                        label = "BarHeight"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "₹${formatCompactAmount(rawValue)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Drawn custom bar with rounded tops and premium vertical gradient preview
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.8f)
                                .width(22.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(primaryColor, secondaryColor)
                                    ),
                                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                )
                                .fillMaxHeight(animatedHeightState.value)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = month,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = labelColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusDistributionPieChart(
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum().coerceAtLeast(1)
    
    // Status to Colors mappings
    val colorMap = mapOf(
        "New" to StatusNew,
        "Assigned" to StatusAssigned,
        "Editing" to StatusEditing,
        "Preview Sent" to StatusPreviewSent,
        "Revision" to StatusRevision,
        "Final Delivery" to StatusFinalDelivery,
        "Completed" to StatusCompleted,
        "On Hold" to StatusOnHold
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        Text(
            text = "Workflow Distribution",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (data.isEmpty() || total == 0) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Text("No projects registered", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 14.sp)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Drawing continuous ring arc slice
                Canvas(
                    modifier = Modifier
                        .size(120.dp)
                ) {
                    var startAngle = -90f
                    val strokeWidth = 32f

                    data.forEach { (status, count) ->
                        val sweepAngle = (count.toFloat() / total.toFloat()) * 360f
                        val color = colorMap[status] ?: Color.Gray
                        
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )
                        startAngle += sweepAngle
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Legends grid
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    data.forEach { (status, count) ->
                        val percent = ((count.toFloat() / total.toFloat()) * 100).toInt()
                        val color = colorMap[status] ?: Color.Gray
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(color, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$status - $count ($percent%)",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompletedProjectsLineChart(
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val entries = data.entries.toList().takeLast(6)
    val maxVal = (entries.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val fontColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        Text(
            text = "Completed Projects (Monthly)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Text("No data available", color = fontColor, fontSize = 14.sp)
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val paddingLeft = 40f
                val paddingBottom = 40f
                val chartWidth = size.width - paddingLeft
                val chartHeight = size.height - paddingBottom
                
                // Draw horizontal grids & baseline
                val gridSteps = 3
                for (i in 0..gridSteps) {
                    val y = chartHeight - (chartHeight / gridSteps) * i
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2f
                    )
                }

                // Smooth quadratic path points
                val sizeOfEntries = entries.size
                val stepX = if (sizeOfEntries > 1) chartWidth / (sizeOfEntries - 1) else chartWidth
                
                val pathPoints = mutableListOf<Offset>()
                entries.forEachIndexed { sIndex, (_, count) ->
                    val x = paddingLeft + (sIndex * stepX)
                    val progressY = count.toFloat() / maxVal.toFloat()
                    val y = chartHeight - (progressY * chartHeight * 0.85f) // Reserve some top room
                    pathPoints.add(Offset(x, y))
                }

                // Draw curve
                val curvePath = Path().apply {
                    if (pathPoints.isNotEmpty()) {
                        moveTo(pathPoints[0].x, pathPoints[0].y)
                        for (i in 1 until pathPoints.size) {
                            val previous = pathPoints[i - 1]
                            val current = pathPoints[i]
                            val controlX = (previous.x + current.x) / 2
                            cubicTo(controlX, previous.y, controlX, current.y, current.x, current.y)
                        }
                    }
                }
                
                drawPath(
                    path = curvePath,
                    color = primaryColor,
                    style = Stroke(width = 6f)
                )

                // Draw circles at key notes and text labels on Canvas
                pathPoints.forEachIndexed { sIndex, offset ->
                    drawCircle(
                        color = primaryColor,
                        center = offset,
                        radius = 10f
                    )
                    drawCircle(
                        color = Color.White,
                        center = offset,
                        radius = 5f
                    )
                }
            }
            
            // Render labels row
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                entries.forEach { entry ->
                    Text(
                        text = entry.key,
                        fontSize = 11.sp,
                        color = fontColor
                    )
                }
            }
        }
    }
}

private fun formatCompactAmount(value: Double): String {
    return when {
        value >= 100000 -> String.format(Locale.getDefault(), "%.1fL", value / 100000)
        value >= 1000 -> String.format(Locale.getDefault(), "%.1fK", value / 1000)
        else -> String.format(Locale.getDefault(), "%.0f", value)
    }
}
