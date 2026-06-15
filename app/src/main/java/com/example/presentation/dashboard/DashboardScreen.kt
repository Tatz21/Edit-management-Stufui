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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Project
import com.example.domain.model.Invoice
import com.example.domain.model.Client
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
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
                            PaymentDashboardComponent(
                                viewModel = viewModel,
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
                        PaymentDashboardComponent(
                            viewModel = viewModel,
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
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val statusFilters = listOf("All", "In Progress", "Editing", "Review", "Finalized")

    val counts = remember(projects) {
        val nonOnHold = projects.filter { it.status != "On Hold" }
        mapOf(
            "All" to nonOnHold.size,
            "In Progress" to nonOnHold.count { getDisplayStatus(it.status) == "In Progress" },
            "Editing" to nonOnHold.count { getDisplayStatus(it.status) == "Editing" },
            "Review" to nonOnHold.count { getDisplayStatus(it.status) == "Review" },
            "Finalized" to nonOnHold.count { getDisplayStatus(it.status) == "Finalized" }
        )
    }

    val filteredProjects = remember(projects, selectedStatusFilter, searchQuery) {
        val nonOnHold = projects.filter { it.status != "On Hold" }
        val byStatus = if (selectedStatusFilter == "All") {
            nonOnHold
        } else {
            nonOnHold.filter { getDisplayStatus(it.status) == selectedStatusFilter }
        }

        (if (searchQuery.isBlank()) {
            byStatus
        } else {
            byStatus.filter {
                it.projectTitle.contains(searchQuery, ignoreCase = true) ||
                it.clientName.contains(searchQuery, ignoreCase = true) ||
                it.assignedEditor.contains(searchQuery, ignoreCase = true)
            }
        }).sortedBy { p ->
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
                    text = "${filteredProjects.size} Projects",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onNavigateToProjects() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Central status filter switcher chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                statusFilters.forEach { statusFilter ->
                    val isSelected = selectedStatusFilter == statusFilter
                    val count = counts[statusFilter] ?: 0
                    val baseColors = getDisplayStatusThemeColors(statusFilter, isDark)
                    val activeBgColor = if (isSelected) {
                        baseColors.bg
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    }
                    val activeTextColor = if (isSelected) {
                        baseColors.text
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    
                    Surface(
                        onClick = { selectedStatusFilter = statusFilter },
                        shape = RoundedCornerShape(100.dp),
                        color = activeBgColor,
                        border = if (isSelected) {
                            BorderStroke(1.5.dp, baseColors.text)
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        },
                        modifier = Modifier.testTag("dashboard_filter_$statusFilter")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (statusFilter != "All") {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(baseColors.text, RoundedCornerShape(50.dp))
                                )
                            }
                            Text(
                                text = statusFilter,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeTextColor
                            )
                            Surface(
                                color = if (isSelected) baseColors.text.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Text(
                                    text = count.toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = activeTextColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Compact search input line
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search projects, client name, editor...", fontSize = 12.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("dashboard_search_input"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(18.dp)) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
            )

            if (filteredProjects.isEmpty()) {
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
                        text = if (searchQuery.isNotEmpty()) "No matching video projects found" else "No projects in category '${selectedStatusFilter}'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filteredProjects.take(6).forEach { project ->
                        ActiveProjectRowItem(
                            project = project,
                            isDark = isDark,
                            onClick = { onProjectClick(project.projectId) }
                        )
                    }

                    if (filteredProjects.size > 6) {
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
                                    text = "View remaining ${filteredProjects.size - 6} active projects...",
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

    val displayStatus = remember(project.status) {
        getDisplayStatus(project.status)
    }

    val statusColors = remember(displayStatus, isDark) {
        getDisplayStatusThemeColors(displayStatus, isDark)
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

                Spacer(modifier = Modifier.height(4.dp))

                // Professional progress stage indicator
                val progressPercent = when (displayStatus) {
                    "In Progress" -> 0.25f
                    "Editing" -> 0.50f
                    "Review" -> 0.75f
                    "Finalized" -> 1.00f
                    else -> 0.00f
                }
                val progressTrackColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFECECEC)
                val progressFillColor = statusColors.text

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(progressTrackColor, RoundedCornerShape(100.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressPercent)
                                .background(progressFillColor, RoundedCornerShape(100.dp))
                        )
                    }
                    Text(
                        text = "${(progressPercent * 100).toInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = progressFillColor
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(statusColors.bg, RoundedCornerShape(100.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(statusColors.text, RoundedCornerShape(50.dp))
                    )
                    Text(
                        text = displayStatus,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
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
                        text = "Est: ${formatDateLabel(project.deadlineDate)}",
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

private fun getDisplayStatus(status: String): String {
    return when (status) {
        "New", "Assigned" -> "In Progress"
        "Editing" -> "Editing"
        "Preview Sent", "Revision" -> "Review"
        "Final Delivery", "Completed" -> "Finalized"
        else -> "On Hold"
    }
}

private fun getDisplayStatusThemeColors(displayStatus: String, isDark: Boolean): StatusTheme {
    return when (displayStatus) {
        "In Progress" -> StatusTheme(
            bg = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFFFEF3C7),
            text = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
        )
        "Editing" -> StatusTheme(
            bg = if (isDark) Color(0xFFE28743).copy(alpha = 0.15f) else Color(0xFFFBE9E7),
            text = if (isDark) Color(0xFFF4511E) else Color(0xFFBF360C)
        )
        "Review" -> StatusTheme(
            bg = if (isDark) Color(0xFF0EA5E9).copy(alpha = 0.15f) else Color(0xFFE0F2FE),
            text = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)
        )
        "Finalized" -> StatusTheme(
            bg = if (isDark) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFD1FAE5),
            text = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
        )
        else -> StatusTheme(
            bg = if (isDark) Color(0xFF94A3B8).copy(alpha = 0.15f) else Color(0xFFF1F5F9),
            text = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
        )
    }
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDashboardComponent(
    viewModel: MainViewModel,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val invoices = remember { viewModel.invoices }.collectAsState(initial = emptyList()).value
    val clients = remember { viewModel.clients }.collectAsState(initial = emptyList()).value
    val projects = remember { viewModel.projects }.collectAsState(initial = emptyList()).value
    val context = LocalContext.current

    var selectedStatusFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showLogDialog by remember { mutableStateOf(false) }

    // Dialog state
    var selectedClient by remember { mutableStateOf<Client?>(null) }
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var customInvoiceNumber by remember { mutableStateOf("") }
    var invoiceAmount by remember { mutableStateOf("") }
    var invoiceStatus by remember { mutableStateOf("Pending") }
    var issueDate by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var invoiceNotes by remember { mutableStateOf("") }

    var clientDropdownExpanded by remember { mutableStateOf(false) }
    var projectDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(showLogDialog) {
        if (showLogDialog) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            issueDate = sdf.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 14)
            dueDate = sdf.format(calendar.time)
            
            val num = 1001 + invoices.size
            customInvoiceNumber = "INV-${Calendar.getInstance().get(Calendar.YEAR)}-$num"
            
            selectedClient = null
            selectedProject = null
            invoiceAmount = ""
            invoiceStatus = "Pending"
            invoiceNotes = ""
        }
    }

    LaunchedEffect(selectedProject) {
        selectedProject?.let { p ->
            if (invoiceAmount.isEmpty()) {
                invoiceAmount = p.remainingAmount.toString()
            }
        }
    }

    val filteredInvoices = remember(invoices, selectedStatusFilter, searchQuery) {
        invoices.filter { inv ->
            val matchesFilter = when (selectedStatusFilter) {
                "All" -> true
                "Paid" -> inv.status == "Paid"
                "Pending" -> inv.status == "Pending" || inv.status == "Overdue"
                else -> true
            }

            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                inv.clientName.contains(searchQuery, ignoreCase = true) ||
                (inv.projectName?.contains(searchQuery, ignoreCase = true) ?: false)
            }

            matchesFilter && matchesQuery
        }.sortedBy { inv ->
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.parse(inv.dueDate)?.time ?: Long.MAX_VALUE
            } catch (e: Exception) {
                Long.MAX_VALUE
            }
        }
    }

    val upcomingCount = remember(invoices) {
        invoices.count { it.status == "Pending" || it.status == "Overdue" }
    }
    val upcomingAmount = remember(invoices) {
        invoices.filter { it.status == "Pending" || it.status == "Overdue" }.sumOf { it.amount }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_payment_card"),
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
                        imageVector = Icons.Default.Payments,
                        contentDescription = "Payments Dashboard Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Payments & Receivables",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = { showLogDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("btn_log_invoice")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Invoice",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Invoice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Outstanding Invoices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$upcomingCount Pending",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (upcomingCount > 0) Color(0xFFFBBF24) else MaterialTheme.colorScheme.primary
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                )

                Column(modifier = Modifier.weight(1.1f)) {
                    Text(
                        text = "Amount Due",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$${String.format(Locale.getDefault(), "%,.0f", upcomingAmount)} USD",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (upcomingCount > 0) Color(0xFFFBBF24) else Color(0xFF10B981)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filters = listOf("All", "Pending", "Paid")
                filters.forEach { status ->
                    val isSelected = selectedStatusFilter == status
                    val activeBg = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    }
                    val activeText = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        onClick = { selectedStatusFilter = status },
                        shape = RoundedCornerShape(100.dp),
                        color = activeBg,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.testTag("dashboard_invoice_filter_chip_$status")
                    ) {
                        Text(
                            text = status,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeText,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by serial #, client name...", fontSize = 11.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("dashboard_payment_search"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                ),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
            )

            if (filteredInvoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching invoices found" else "No invoices recorded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredInvoices.take(5).forEach { invoice ->
                        DashboardInvoiceRowItem(
                            invoice = invoice,
                            isDark = isDark,
                            onStatusChange = { newStatus ->
                                viewModel.addOrUpdateInvoice(invoice.copy(status = newStatus))
                            }
                        )
                    }

                    if (filteredInvoices.size > 5) {
                        Text(
                            text = "+ ${filteredInvoices.size - 5} more invoices",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PostAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Log Invoice Record", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedClient?.name ?: "Choose Client Profile *",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Associated Client") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                IconButton(onClick = { clientDropdownExpanded = !clientDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { clientDropdownExpanded = true },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        DropdownMenu(
                            expanded = clientDropdownExpanded,
                            onDismissRequest = { clientDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            clients.forEach { client ->
                                DropdownMenuItem(
                                    text = { Text(client.name) },
                                    onClick = {
                                        selectedClient = client
                                        selectedProject = null
                                        clientDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedClient != null) {
                        val clientProjList = remember(projects, selectedClient) {
                            projects.filter {
                                (selectedClient?.email?.isNotEmpty() == true && it.clientEmail.equals(selectedClient?.email, ignoreCase = true)) ||
                                it.clientName.equals(selectedClient?.name, ignoreCase = true)
                            }
                        }

                        if (clientProjList.isNotEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = selectedProject?.projectTitle ?: "General billing (No linked project)",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Link to specific Project") },
                                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                                    trailingIcon = {
                                        IconButton(onClick = { projectDropdownExpanded = !projectDropdownExpanded }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { projectDropdownExpanded = true },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )

                                DropdownMenu(
                                    expanded = projectDropdownExpanded,
                                    onDismissRequest = { projectDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("General billing (No Link)") },
                                        onClick = {
                                            selectedProject = null
                                            projectDropdownExpanded = false
                                        }
                                    )
                                    clientProjList.forEach { proj ->
                                        DropdownMenuItem(
                                            text = { Text("${proj.projectTitle} ($${proj.remainingAmount} left)") },
                                            onClick = {
                                                selectedProject = proj
                                                projectDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customInvoiceNumber,
                        onValueChange = { customInvoiceNumber = it },
                        label = { Text("Invoice Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    OutlinedTextField(
                        value = invoiceAmount,
                        onValueChange = { invoiceAmount = it },
                        label = { Text("Billable Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        prefix = { Text("$ ") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = invoiceStatus,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Invoice Status") },
                            modifier = Modifier.fillMaxWidth().clickable { statusDropdownExpanded = true },
                            trailingIcon = {
                                IconButton(onClick = { statusDropdownExpanded = !statusDropdownExpanded }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "status dropdown")
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )

                        DropdownMenu(
                            expanded = statusDropdownExpanded,
                            onDismissRequest = { statusDropdownExpanded = false }
                        ) {
                            Invoice.STATUS_OPTIONS.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        invoiceStatus = option
                                        statusDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = issueDate,
                            onValueChange = { issueDate = it },
                            label = { Text("Issue Date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            label = { Text("Due Date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = invoiceNotes,
                        onValueChange = { invoiceNotes = it },
                        label = { Text("Notes / Descriptions") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val client = selectedClient
                        val amountParsed = invoiceAmount.trim().toDoubleOrNull()
                        
                        if (client == null) {
                            Toast.makeText(context, "Please select an existing client", Toast.LENGTH_SHORT).show()
                        } else if (customInvoiceNumber.trim().isEmpty()) {
                            Toast.makeText(context, "Please supply an invoice number", Toast.LENGTH_SHORT).show()
                        } else if (amountParsed == null || amountParsed <= 0.0) {
                            Toast.makeText(context, "Please enter an amount greater than 0.0", Toast.LENGTH_SHORT).show()
                        } else {
                            val newInvoice = Invoice(
                                invoiceId = UUID.randomUUID().toString(),
                                clientId = client.clientId,
                                clientName = client.name,
                                projectId = selectedProject?.projectId,
                                projectName = selectedProject?.projectTitle,
                                invoiceNumber = customInvoiceNumber.trim(),
                                amount = amountParsed,
                                status = invoiceStatus,
                                issueDate = issueDate.trim(),
                                dueDate = dueDate.trim(),
                                notes = invoiceNotes.trim()
                            )
                            viewModel.addOrUpdateInvoice(newInvoice)
                            showLogDialog = false
                            Toast.makeText(context, "Logged ${newInvoice.invoiceNumber} successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("dialog_invoice_confirm")
                ) {
                    Text("Issue Invoice")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogDialog = false },
                    modifier = Modifier.testTag("dialog_dismiss_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DashboardInvoiceRowItem(
    invoice: Invoice,
    isDark: Boolean,
    onStatusChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    val statusColor = when (invoice.status) {
        "Paid" -> Color(0xFF10B981)
        "Pending" -> Color(0xFFFBBF24)
        "Overdue" -> Color(0xFFEF4444)
        else -> Color(0xFF6B7280)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("invoice_row_${invoice.invoiceId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = invoice.invoiceNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = invoice.clientName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Due Date",
                        tint = if (invoice.status == "Overdue") Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "Due: ${invoice.dueDate}",
                        fontSize = 11.sp,
                        color = if (invoice.status == "Overdue") Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$${String.format(Locale.getDefault(), "%,.0f", invoice.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Box {
                    Row(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
                            .clickable { dropdownExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(statusColor, RoundedCornerShape(50))
                        )
                        Text(
                            text = invoice.status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Change Status",
                            tint = statusColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        Invoice.STATUS_OPTIONS.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    onStatusChange(opt)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
