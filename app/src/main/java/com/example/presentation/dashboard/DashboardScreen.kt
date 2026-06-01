package com.example.presentation.dashboard

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
import androidx.compose.runtime.remember
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
    modifier: Modifier = Modifier
) {
    val projects = remember { viewModel.projects }.collectAsState(initial = emptyList()).value
    val syncState = remember { viewModel.syncStatus }.collectAsState(initial = "Local Mode").value

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
        
        // Pad with standard months if empty to trigger clean view
        if (map.isEmpty()) {
            map["Mar 26"] = 75000.0
            map["Apr 26"] = 145000.0
            map["May 26"] = 210000.0
            map["Jun 26"] = 0.0
        }
        map
    }

    // 3. Status Distributions Map
    val statusData = remember(projects) {
        val map = mutableMapOf<String, Int>()
        projects.forEach { p ->
            map[p.status] = (map[p.status] ?: 0) + 1
        }
        // Pad with mock empty indicator fields if zero projects exist
        if (map.isEmpty()) {
            map["New"] = 2
            map["Editing"] = 3
            map["Preview Sent"] = 1
            map["Completed"] = 4
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
        if (map.isEmpty()) {
            map["Mar 26"] = 1
            map["Apr 26"] = 3
            map["May 26"] = 5
            map["Jun 26"] = 0
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
                    // Sync badge
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.triggerManualSync() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (syncState.contains("Sync", ignoreCase = true)) Color(0xFF10B981) else Color(0xFFF59E0B),
                                        RoundedCornerShape(50)
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = syncState, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
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
    isDark: Boolean,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDark) Color(0xFF1C1B1F) else Color(0xFFFFFFFF)
    val borderCol = if (isDark) Color(0xFF49454F) else Color(0xFFCAC4D0)
    val headingColor = if (isDark) Color(0xFFCCC2DC) else Color(0xFF1D192B)

    val mritunjayActive = projects.count { it.assignedEditor.equals("Mritunjay", ignoreCase = true) && it.status != "Completed" && it.status != "On Hold" }
    val rijhuActive = projects.count { it.assignedEditor.equals("Rijhu", ignoreCase = true) && it.status != "Completed" && it.status != "On Hold" }
    val didiActive = projects.count { it.assignedEditor.equals("Didi", ignoreCase = true) && it.status != "Completed" && it.status != "On Hold" }

    val mPercent = if (projects.isEmpty()) 0.85f else ((mritunjayActive * 0.20f) + 0.25f).coerceIn(0.10f, 0.95f)
    val rPercent = if (projects.isEmpty()) 0.40f else ((rijhuActive * 0.20f) + 0.15f).coerceIn(0.10f, 0.95f)
    val dPercent = if (projects.isEmpty()) 0.60f else ((didiActive * 0.20f) + 0.20f).coerceIn(0.10f, 0.95f)

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
            
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                EditorWorkloadRow(
                    avatarName = "M",
                    name = "Mritunjay",
                    percent = mPercent,
                    color = Color(0xFF6750A4),
                    isActive = mritunjayActive > 0,
                    activeCount = mritunjayActive,
                    isDark = isDark
                )
                EditorWorkloadRow(
                    avatarName = "R",
                    name = "Rijhu",
                    percent = rPercent,
                    color = Color(0xFF006A6A),
                    isActive = rijhuActive > 0,
                    activeCount = rijhuActive,
                    isDark = isDark
                )
                EditorWorkloadRow(
                    avatarName = "D",
                    name = "Didi",
                    percent = dPercent,
                    color = Color(0xFF7D5260),
                    isActive = didiActive > 0,
                    activeCount = didiActive,
                    isDark = isDark
                )
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
