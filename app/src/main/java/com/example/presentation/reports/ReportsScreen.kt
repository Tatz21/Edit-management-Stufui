package com.example.presentation.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Project
import com.example.presentation.MainViewModel
import com.example.presentation.project.CustomDropdownSelector
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showExportSheet by remember { mutableStateOf(false) }
    var exportFilterSelection by remember { mutableStateOf("All") }

    // 1. Compute Monthly Stats
    val monthlyStats = remember(projects) {
        val today = Calendar.getInstance()
        val currentMonth = today.get(Calendar.MONTH)
        val currentYear = today.get(Calendar.YEAR)
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val monthlyProjects = projects.filter { p ->
            try {
                val date = format.parse(p.receivedDate)
                if (date != null) {
                    val cal = Calendar.getInstance().apply { time = date }
                    cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
                } else false
            } catch (e: Exception) { false }
        }

        val receivedCount = monthlyProjects.size
        val completedCount = monthlyProjects.count { it.status == "Completed" }
        val revenueGenerated = monthlyProjects.sumOf { it.totalAmount }

        MonthlyReportSummary(receivedCount, completedCount, revenueGenerated)
    }

    // 2. Compute Editor Metrics
    val editors = Project.EDITORS
    val editorPerformanceList = remember(projects) {
        editors.map { e ->
            val assigned = projects.filter { it.assignedEditor.equals(e, ignoreCase = true) }
            val totalAssignedCount = assigned.size
            val completedCount = assigned.count { it.status == "Completed" }
            val rate = if (totalAssignedCount > 0) (completedCount.toFloat() / totalAssignedCount.toFloat() * 100).toInt() else 0
            val contribution = assigned.filter { it.status == "Completed" }.sumOf { it.totalAmount }
            
            EditorPerformanceReportItem(e, totalAssignedCount, rate, contribution)
        }
    }

    // 3. Compute Invoice Payment Stats
    val paymentReport = remember(projects) {
        val fullyPaid = projects.count { it.paymentStatus == "Fully Paid" }
        val partiallyPaid = projects.count { it.paymentStatus == "Partially Paid" }
        val pendingCount = projects.count { it.paymentStatus == "Pending" || it.paymentStatus.isEmpty() }
        
        val outstandingSum = projects.sumOf { it.remainingAmount }
        val collectedSum = projects.sumOf { it.advanceAmount }

        PaymentReportSummary(fullyPaid, partiallyPaid, pendingCount, collectedSum, outstandingSum)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Agency Reports", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Large Action Excel POI Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showExportSheet = true }
                    .testTag("reports_export_xlsx_card")
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Export Spreadsheet Documents", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Compile spreadsheet documents (.csv) of client accounts and project statuses dynamically, and share or open in Excel/Sheets immediately.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                    }
                }
            }

            // Monthly Performance Summary
            Text("CURRENT MONTH PERFORMANCE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReportRow(label = "New Projects Received", value = monthlyStats.received.toString())
                    ReportRow(label = "Projects Completed", value = monthlyStats.completed.toString())
                    ReportRow(label = "Deal Volume Generated", value = "₹${formatAmt(monthlyStats.revenue)}")
                }
            }

            // Financial Payment audit ledger summary
            Text("ACCOUNTS RECEIVER SHEET", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReportRow(label = "Fully Settled Invoice nodes", value = paymentReport.fullyPaid.toString())
                    ReportRow(label = "Partially Paid pipelines", value = paymentReport.partiallyPaid.toString())
                    ReportRow(label = "Unpaid Remaining nodes", value = paymentReport.pending.toString())
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ReportRow(label = "Total Collected Capital", value = "₹${formatAmt(paymentReport.totalCollected)}", color = Color(0xFF10B981))
                    ReportRow(label = "Outstanding Booking Dues", value = "₹${formatAmt(paymentReport.outstandingAmount)}", color = Color(0xFFF59E0B))
                }
            }

            // Editor Performances lists
            Text("EDITORS PERFORMANCE MATRIX", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    editorPerformanceList.forEach { ep ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ep.editorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("ROI: ₹${formatAmt(ep.revenueContribution)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Assigned: ${ep.assignedCount} projects", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text("Completion rate: ${ep.completionRate}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            if (ep != editorPerformanceList.last()) {
                                HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // Sheet popup or dialogue for Excel Export specifications
    if (showExportSheet) {
        AlertDialog(
            onDismissRequest = { showExportSheet = false },
            title = { Text("Spreadsheet Report (.csv)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Select projects scope to export compiled spreadsheet directly:", fontSize = 13.sp)
                    
                    CustomDropdownSelector(
                        label = "Include Projects",
                        selected = exportFilterSelection,
                        options = listOf("All", "Completed", "Overdue", "Payments Pending"),
                        onSelected = { exportFilterSelection = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val simpleFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                        val title = "EditFlowPro_Report_$simpleFormat"
                        viewModel.shareExcelReport(context, title, exportFilterSelection)
                        showExportSheet = false
                    },
                    modifier = Modifier.testTag("export_xlsx_confirm_btn")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Spreadsheet")
                }
            }
        )
    }
}

@Composable
fun ReportRow(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// Stats models
data class MonthlyReportSummary(val received: Int, val completed: Int, val revenue: Double)
data class EditorPerformanceReportItem(val editorName: String, val assignedCount: Int, val completionRate: Int, val revenueContribution: Double)
data class PaymentReportSummary(val fullyPaid: Int, val partiallyPaid: Int, val pending: Int, val totalCollected: Double, val outstandingAmount: Double)

private fun formatAmt(v: Double): String {
    return String.format(Locale.getDefault(), "%,.0f", v)
}
