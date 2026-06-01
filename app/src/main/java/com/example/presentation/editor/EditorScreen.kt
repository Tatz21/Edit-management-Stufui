package com.example.presentation.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Project
import com.example.presentation.MainViewModel
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityMedium
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val editors = Project.EDITORS
    
    var selectedEditorForWorkload by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Agency Editors", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            Text(
                text = "Editors Workload and Output Performance:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            // Editor list cards
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(editors) { editor ->
                    val editorProjects = remember(projects, editor) {
                        projects.filter { it.assignedEditor.equals(editor, ignoreCase = true) }
                    }
                    val activeCount = editorProjects.count { it.status != "Completed" && it.status != "On Hold" }
                    val completedCount = editorProjects.count { it.status == "Completed" }
                    val revContribution = editorProjects.filter { it.status == "Completed" }.sumOf { it.totalAmount }
                    
                    // Nearest deadline
                    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val nearestDLine = editorProjects
                        .filter { it.status != "Completed" && it.status != "On Hold" }
                        .mapNotNull {
                            try { formatter.parse(it.deadlineDate) } catch (e: Exception) { null }
                        }
                        .minOrNull()
                    
                    val nearestDLineStr = if (nearestDLine != null) {
                        SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(nearestDLine)
                    } else "None"

                    EditorPerformanceCard(
                        name = editor,
                        active = activeCount,
                        completed = completedCount,
                        revenue = revContribution,
                        nearestDeadline = nearestDLineStr,
                        onClick = { selectedEditorForWorkload = editor }
                    )
                }
            }
        }
    }

    // Workload dialog pop sheet
    if (selectedEditorForWorkload != null) {
        val editorName = selectedEditorForWorkload!!
        val editorWorks = remember(projects, editorName) {
            projects.filter { it.assignedEditor.equals(editorName, ignoreCase = true) }
        }

        AlertDialog(
            onDismissRequest = { selectedEditorForWorkload = null },
            title = { Text("$editorName Workload Pipeline", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    if (editorWorks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No assigned contracts", fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(editorWorks) { work ->
                                WorkloadItemTrace(work = work)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEditorForWorkload = null }) {
                    Text("Close Panel")
                }
            }
        )
    }
}

@Composable
fun EditorPerformanceCard(
    name: String,
    active: Int,
    completed: Int,
    revenue: Double,
    nearestDeadline: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .testTag("editor_performance_card_$name"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Work, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                EditorMetricItem(label = "Active Deals", value = active.toString())
                EditorMetricItem(label = "Completed Deliveries", value = completed.toString())
                EditorMetricItem(label = "Rev Generated", value = "₹${formatAsThousands(revenue)}")
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // Upcoming nearest deadline label info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Nearest Deadline: ",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = nearestDeadline,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (nearestDeadline == "None") MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun EditorMetricItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun WorkloadItemTrace(work: Project) {
    // Determine indicators
    // Green = On Track
    // Orange = Deadline soon (<= 2 days)
    // Red = Overdue (deadline < today e)
    
    val today = Date()
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val colorIndicator = remember(work.deadlineDate, work.status) {
        if (work.status == "Completed") PriorityLow
        else {
            try {
                val dl = formatter.parse(work.deadlineDate)
                if (dl != null) {
                    val diff = dl.time - today.time
                    val diffDays = diff / (1000 * 60 * 60 * 24)
                    
                    when {
                        diffDays < 0 -> PriorityHigh // Red
                        diffDays <= 2 -> PriorityMedium // Orange
                        else -> PriorityLow // Green
                    }
                } else PriorityLow
            } catch (e: Exception) {
                PriorityLow
            }
        }
    }

    val progressVal = if (work.totalAmount > 0) (work.advanceAmount / work.totalAmount).toFloat().coerceIn(0f, 1f) else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = work.projectTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Circular status indicator dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(colorIndicator, RoundedCornerShape(4.dp))
                )
                Text(
                    text = work.status,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorIndicator
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Client: ${work.clientName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(text = "DL: ${work.deadlineDate}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(6.dp))
        
        LinearProgressIndicator(
            progress = { progressVal },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    }
}

private fun formatAsThousands(v: Double): String {
    return String.format(Locale.getDefault(), "%,.0f", v)
}
