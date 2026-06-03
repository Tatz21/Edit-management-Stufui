package com.example.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Project
import com.example.presentation.MainViewModel
import com.example.presentation.components.CompletedProjectsLineChart
import com.example.presentation.components.MonthlyRevenueBarChart
import com.example.presentation.components.StatusDistributionPieChart
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToProjects: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProjectDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val projects = remember { viewModel.projects }.collectAsState(initial = emptyList()).value
    val dynamicEditors by viewModel.editorsState.collectAsState()

    // 1. Reactive KPI Computations
    val kpis = remember(projects) {
        val total = projects.size
        val active = projects.count { it.status != "Completed" && it.status != "On Hold" }
        val pending = projects.count { it.status == "New" || it.status == "Assigned" || it.status == "Editing" }
        val completed = projects.count { it.status == "Completed" }
        
        // Overdue calculation
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Date()
        val overdue = projects.count { p ->
            if (p.status == "Completed" || p.status == "On Hold" || p.status == "Final Delivery") false
            else {
                try {
                    val dl = formatter.parse(p.deadlineDate)
                    dl != null && dl.before(today)
                } catch (e: Exception) {
                    false
                }
            }
        }

        val totalRev = projects.sumOf { it.totalAmount }
        val advanceCollected = projects.sumOf { it.advanceAmount }
        val remainingCollection = projects.sumOf { it.remainingAmount }

        DashboardKPIs(total, active, pending, completed, overdue, totalRev, advanceCollected, remainingCollection)
    }

    // 2. Monthly Revenue Data extraction (last 6 months)
    val monthlyRevenueData = remember(projects) {
        val map = mutableMapOf<String, Double>()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val monthOutFormat = SimpleDateFormat("MMM yy", Locale.getDefault())
        
        projects.forEach { p ->
            try {
                val date = format.parse(p.receivedDate)
                if (date != null) {
                    val monthLabel = monthOutFormat.format(date)
                    map[monthLabel] = (map[monthLabel] ?: 0.0) + p.totalAmount
                }
            } catch (e: Exception) { /* Date parse ignored */ }
        }
        
        map
    }

    // 3. Status Distributions Map
    val statusData = remember(projects) {
        val map = mutableMapOf<String, Int>()
        projects.forEach { p ->
            map[p.status] = (map[p.status] ?: 0) + 1
        }
        map
    }

    // 4. Monthly completions line data
    val completionsData = remember(projects) {
        val map = mutableMapOf<String, Int>()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val monthOutFormat = SimpleDateFormat("MMM yy", Locale.getDefault())
        
        projects.filter { it.status == "Completed" }.forEach { p ->
            try {
                val date = format.parse(p.deliveryDate.ifEmpty { p.receivedDate })
                if (date != null) {
                    val monthLabel = monthOutFormat.format(date)
                    map[monthLabel] = (map[monthLabel] ?: 0) + 1
                }
            } catch (e: Exception) { /* Date parse ignored */ }
        }
        map
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Admin Dashboard",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "EditFlow Pro Workspace | UTC Live Tracking",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.padding(end = 8.dp).testTag("dashboard_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val isDark = viewModel.isDarkTheme.collectAsState().value

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isTablet = maxWidth > 600.dp

            if (isTablet) {
                // Responsive Split Layout for Tablets
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left half: KPIs Bento Grid
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Bento Metrics Pipeline",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        BentoActiveProjectsCard(
                            count = kpis.activeProjects,
                            isDark = isDark,
                            onClick = onNavigateToProjects
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BentoOverdueCard(
                                count = kpis.overdueProjects,
                                isDark = isDark,
                                onClick = onNavigateToProjects,
                                modifier = Modifier.weight(1f)
                            )
                            BentoCompletedCard(
                                count = kpis.completedProjects,
                                isDark = isDark,
                                onClick = onNavigateToProjects,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        BentoRevenueCard(
                            totalRevenue = kpis.totalRevenue,
                            remainingCollection = kpis.remainingCollection,
                            isDark = isDark
                        )
                        
                        BentoEditorWorkloadCard(
                            projects = projects,
                            editors = dynamicEditors,
                            isDark = isDark,
                            onNavigate = onNavigateToProjects
                        )
                    }

                    // Right half: Vertical Scrollable charts/analytics section
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        item {
                            ActiveProjectsSection(
                                projects = projects,
                                onProjectClick = onNavigateToProjectDetails,
                                onNavigateToProjects = onNavigateToProjects,
                                isDark = isDark
                            )
                        }
                        item {
                            Text(
                                text = "Visual Growth Analytics",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        item {
                            MonthlyRevenueBarChart(data = monthlyRevenueData)
                        }
                        item {
                            StatusDistributionPieChart(data = statusData)
                        }
                        item {
                            CompletedProjectsLineChart(data = completionsData)
                        }
                    }
                }
            } else {
                // Mobile Portrait Single Column Layout
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        Text(
                            text = "Active Pulse",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BentoActiveProjectsCard(
                            count = kpis.activeProjects,
                            isDark = isDark,
                            onClick = onNavigateToProjects
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BentoOverdueCard(
                                count = kpis.overdueProjects,
                                isDark = isDark,
                                onClick = onNavigateToProjects,
                                modifier = Modifier.weight(1f)
                            )
                            BentoCompletedCard(
                                count = kpis.completedProjects,
                                isDark = isDark,
                                onClick = onNavigateToProjects,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        ActiveProjectsSection(
                            projects = projects,
                            onProjectClick = onNavigateToProjectDetails,
                            onNavigateToProjects = onNavigateToProjects,
                            isDark = isDark
                        )
                    }

                    item {
                        Text(
                            text = "Revenue Totals",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BentoRevenueCard(
                            totalRevenue = kpis.totalRevenue,
                            remainingCollection = kpis.remainingCollection,
                            isDark = isDark
                        )
                    }

                    item {
                        Text(
                            text = "Editor Workloads",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BentoEditorWorkloadCard(
                            projects = projects,
                            editors = dynamicEditors,
                            isDark = isDark,
                            onNavigate = onNavigateToProjects
                        )
                    }

                    item {
                        Text(
                            text = "Workloads & Earnings Analytics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    item {
                        MonthlyRevenueBarChart(data = monthlyRevenueData)
                    }

                    item {
                        StatusDistributionPieChart(data = statusData)
                    }

                    item {
                        CompletedProjectsLineChart(data = completionsData)
                    }
                }
            }
        }
    }
}

// Custom Data class to organize metrics
data class DashboardKPIs(
    val totalProjects: Int,
    val activeProjects: Int,
    val pendingProjects: Int,
    val completedProjects: Int,
    val overdueProjects: Int,
    val totalRevenue: Double,
    val advanceCollected: Double,
    val remainingCollection: Double
)

@Composable
fun BentoActiveProjectsCard(
    count: Int,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isDark) Color(0xFF21005D) else Color(0xFFD0BCFF)
    val contentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D)
    val pillBg = if (isDark) Color(0xFF381E72) else Color(0xFF21005D)
    val pillText = Color.White

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Bubble
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isDark) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Active Projects Icon",
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                // Live Pulse Pill
                Card(
                    colors = CardDefaults.cardColors(containerColor = pillBg),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = "LIVE PULSE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = pillText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        letterSpacing = 1.sp
                    )
                }
            }
            
            Column {
                Text(
                    text = count.toString(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light,
                    color = contentColor,
                    lineHeight = 48.sp
                )
                Text(
                    text = "Active Projects",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun BentoOverdueCard(
    count: Int,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isDark) Color(0xFF601410) else Color(0xFFF2B8B5)
    val contentColor = if (isDark) Color(0xFFF2B8B5) else Color(0xFF601410)
    val bubbleBg = if (isDark) Color(0xFF8C1D18) else Color(0xFF601410)
    val bubbleText = Color.White

    Card(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(bubbleBg, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    color = bubbleText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Text(
                text = "OVERDUE",
                color = contentColor,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun BentoCompletedCard(
    count: Int,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isDark) Color(0xFF00391C) else Color(0xFFC4E7CB)
    val contentColor = if (isDark) Color(0xFFC4E7CB) else Color(0xFF00391C)
    val bubbleBg = if (isDark) Color(0xFF0F522B) else Color(0xFF00391C)
    val bubbleText = Color.White

    Card(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(bubbleBg, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    color = bubbleText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Text(
                text = "DONE",
                color = contentColor,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun BentoRevenueCard(
    totalRevenue: Double,
    remainingCollection: Double,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isDark) Color(0xFF4A4458) else Color(0xFFE8DEF8)
    val contentColor = if (isDark) Color(0xFFE8DEF8) else Color(0xFF1D192B)
    val subTextCol = if (isDark) Color(0xFFCCC2DC) else Color(0xFF49454F)
    val pendingTextCol = if (isDark) Color(0xFFF2B8B5) else Color(0xFFB3261E)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "TOTAL REVENUE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = subTextCol,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "₹${formatAmount(totalRevenue)}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "PENDING UNPAID",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = subTextCol,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "₹${formatAmount(remainingCollection)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = pendingTextCol
                )
            }
        }
    }
}

@Composable
fun BentoEditorWorkloadCard(
    projects: List<Project>,
    editors: List<String>,
    isDark: Boolean,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDark) Color(0xFF1C1B1F) else Color(0xFFFFFFFF)
    val borderCol = if (isDark) Color(0xFF49454F) else Color(0xFFCAC4D0)
    val headingColor = if (isDark) Color(0xFFCCC2DC) else Color(0xFF1D192B)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, RoundedCornerShape(28.dp))
            .clickable(onClick = onNavigate),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
        ) {
            Text(
                text = "EDITOR WORKLOADS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = headingColor,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            val displayEditors = editors.take(5)
            if (displayEditors.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("No editors configured.", fontSize = 13.sp, color = if (isDark) Color.LightGray else Color.DarkGray)
                }
            } else {
                val colorsList = listOf(Color(0xFF6750A4), Color(0xFF006A6A), Color(0xFF7D5260), Color(0xFF10B981), Color(0xFFE28743))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    displayEditors.forEachIndexed { index, editorName ->
                        val activeCount = projects.count { it.assignedEditor.equals(editorName, ignoreCase = true) && it.status != "Completed" && it.status != "On Hold" }
                        val percent = if (projects.isEmpty()) 0.40f else ((activeCount * 0.20f) + 0.15f).coerceIn(0.10f, 0.95f)
                        val color = colorsList[index % colorsList.size]
                        val avatar = if (editorName.isNotEmpty()) editorName.take(1).uppercase() else "E"

                        EditorWorkloadRow(
                            avatarName = avatar,
                            name = editorName,
                            percent = percent,
                            color = color,
                            isActive = activeCount > 0,
                            activeCount = activeCount,
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditorWorkloadRow(
    avatarName: String,
    name: String,
    percent: Float,
    color: Color,
    isActive: Boolean,
    activeCount: Int,
    isDark: Boolean
) {
    val trackBg = if (isDark) Color(0xFF313033) else Color(0xFFE6E1E5)
    val nameColor = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)
    val subtitleColor = if (isDark) Color(0xFFCCC2DC) else Color(0xFF49454F)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = avatarName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = nameColor
                )
                Text(
                    text = "${(percent * 100).toInt()}%" + if (isActive) " ($activeCount active)" else "",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = subtitleColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(trackBg, RoundedCornerShape(100.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(percent)
                        .background(color, RoundedCornerShape(100.dp))
                )
            }
        }
    }
}

private fun formatAmount(v: Double): String {
    return String.format(Locale.getDefault(), "%,.0f", v)
}

@Composable
fun ActiveProjectsSection(
    projects: List<Project>,
    onProjectClick: (String) -> Unit,
    onNavigateToProjects: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val activeProjects = remember(projects) {
        projects.filter { it.status != "Completed" && it.status != "On Hold" }
            .sortedBy { p ->
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    sdf.parse(p.deadlineDate)?.time ?: Long.MAX_VALUE
                } catch (e: Exception) {
                    Long.MAX_VALUE
                }
            }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Active Video Pipeline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Text(
                    text = "${activeProjects.size} Projects",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onNavigateToProjects() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (activeProjects.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoCameraBack,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "No active video editing projects",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    activeProjects.take(6).forEach { project ->
                        ActiveProjectRowItem(
                            project = project,
                            isDark = isDark,
                            onClick = { onProjectClick(project.projectId) }
                        )
                    }

                    if (activeProjects.size > 6) {
                        Surface(
                            onClick = onNavigateToProjects,
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "View remaining ${activeProjects.size - 6} active projects...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveProjectRowItem(
    project: Project,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = Date()
    val isOverdue = remember(project.deadlineDate) {
        try {
            val dl = formatter.parse(project.deadlineDate)
            dl != null && dl.before(today)
        } catch (e: Exception) {
            false
        }
    }

    val statusColors = remember(project.status, isDark) {
        getStatusThemeColors(project.status, isDark)
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_project_item_${project.projectId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isOverdue) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (isOverdue) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (project.projectType.contains("Short", ignoreCase = true) || project.projectType.contains("Reel", ignoreCase = true)) {
                        Icons.Default.Movie
                    } else if (project.projectType.contains("Ad", ignoreCase = true) || project.projectType.contains("Promo", ignoreCase = true)) {
                        Icons.Default.Videocam
                    } else {
                        Icons.Default.VideoCameraBack
                    },
                    contentDescription = null,
                    tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = project.projectTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = project.clientName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(statusColors.bg, RoundedCornerShape(100.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = project.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColors.text
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isOverdue) Icons.Default.Warning else Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = formatDateLabel(project.deadlineDate),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatDateLabel(dateStr: String): String {
    if (dateStr.isEmpty()) return "No deadline"
    try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = parser.parse(dateStr)
        if (date != null) {
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return formatter.format(date)
        }
    } catch (e: Exception) {
        // Fallback
    }
    return dateStr
}

private data class StatusTheme(val bg: Color, val text: Color)

private fun getStatusThemeColors(status: String, isDark: Boolean): StatusTheme {
    return when (status) {
        "New" -> StatusTheme(
            bg = if (isDark) Color(0xFF1D3557).copy(alpha = 0.4f) else Color(0xFFE8F1F5),
            text = if (isDark) Color(0xFF4EA8DE) else Color(0xFF1D3557)
        )
        "Assigned" -> StatusTheme(
            bg = if (isDark) Color(0xFF005F73).copy(alpha = 0.4f) else Color(0xFFE0F2F1),
            text = if (isDark) Color(0xFF94D2BD) else Color(0xFF005F73)
        )
        "Editing" -> StatusTheme(
            bg = if (isDark) Color(0xFFCA6702).copy(alpha = 0.4f) else Color(0xFFFEF3C7),
            text = if (isDark) Color(0xFFEE9B00) else Color(0xFFB45309)
        )
        "Preview Sent" -> StatusTheme(
            bg = if (isDark) Color(0xFF7209B7).copy(alpha = 0.4f) else Color(0xFFF3E5F5),
            text = if (isDark) Color(0xFFB5179E) else Color(0xFF7209B7)
        )
        "Revision" -> StatusTheme(
            bg = if (isDark) Color(0xFF9B2226).copy(alpha = 0.4f) else Color(0xFFFFEBEE),
            text = if (isDark) Color(0xFFE63946) else Color(0xFF9B2226)
        )
        "Final Delivery" -> StatusTheme(
            bg = if (isDark) Color(0xFF1B4332).copy(alpha = 0.4f) else Color(0xFFE8F5E9),
            text = if (isDark) Color(0xFF52B788) else Color(0xFF1B4332)
        )
        "Completed" -> StatusTheme(
            bg = if (isDark) Color(0xFF1A5235).copy(alpha = 0.4f) else Color(0xFFE8F5E9),
            text = if (isDark) Color(0xFF2EC4B6) else Color(0xFF1A5235)
        )
        else -> StatusTheme(
            bg = if (isDark) Color(0xFF3F37C9).copy(alpha = 0.4f) else Color(0xFFE8EAF6),
            text = if (isDark) Color(0xFF4895EF) else Color(0xFF3F37C9)
        )
    }
}
