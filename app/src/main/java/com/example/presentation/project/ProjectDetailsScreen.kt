package com.example.presentation.project

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Project
import com.example.presentation.MainViewModel
import com.example.util.JsonUtil
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsScreen(
    viewModel: MainViewModel,
    projectId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditProject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val context = LocalContext.current
    
    val project = remember(projectId, projects) {
        projects.find { it.projectId == projectId }
    }

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Node not found", fontWeight = FontWeight.Bold)
        }
        return
    }

    // Modal popup states
    var showPaymentDialog by remember { mutableStateOf(false) }
    var payAmount by remember { mutableStateOf("") }
    var payNotes by remember { mutableStateOf("") }

    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewVer by remember { mutableStateOf("vScale_1") }
    var previewApprovedState by remember { mutableStateOf(true) }
    var previewNotes by remember { mutableStateOf("") }

    var showRevisionDialog by remember { mutableStateOf(false) }
    var revisionVer by remember { mutableStateOf("vScale_1") }
    var revisionDetails by remember { mutableStateOf("") }
    var revisionEditorNotes by remember { mutableStateOf("") }

    val payments = JsonUtil.fromPaymentHistoryJson(project.paymentHistory)
    val previews = JsonUtil.fromPreviewHistoryJson(project.previewHistory)
    val revisions = JsonUtil.fromRevisionHistoryJson(project.revisionHistory)
    val activityLogsList = JsonUtil.fromActivityHistoryJson(project.activityLogs)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(project.projectTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("details_back") ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEditProject(project.projectId) }, modifier = Modifier.testTag("edit_project_icon")) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(
                        onClick = {
                            viewModel.deleteProject(project.projectId)
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("delete_project_icon")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                    }
                },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // Client Contact Card with Call & Email integration trigger
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CLIENT CONTACT & DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(project.clientName, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    
                    if (project.clientPhone.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = "Phone", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(project.clientPhone, fontSize = 13.sp, modifier = Modifier.clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${project.clientPhone}"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {}
                            }, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    if (project.clientEmail.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(imageVector = Icons.Default.Email, contentDescription = "Email", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(project.clientEmail, fontSize = 13.sp, modifier = Modifier.clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${project.clientEmail}"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {}
                            }, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Visual Workflow Timeline block
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PRODUCTION PIPELINE VISUAL TIMELINE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    VisualPipelineTimeline(activeStatus = project.status)
                }
            }

            // Financial Summary & Milestone actions
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("FINANCIAL BALANCE SHEET", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                        Button(
                            onClick = { showPaymentDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), contentColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("record_payment_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Record Pay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FinancialColumn(label = "Total Deal", value = "₹${project.totalAmount}", modifier = Modifier.weight(1f))
                        FinancialColumn(label = "Advance Paid", value = "₹${project.advanceAmount}", valueColor = Color(0xFF10B981), modifier = Modifier.weight(1f))
                        FinancialColumn(label = "Outstanding Balance", value = "₹${project.remainingAmount}", valueColor = if (project.remainingAmount > 0) Color(0xFFF59E0B) else Color(0xFF10B981), modifier = Modifier.weight(1f))
                    }

                    if (payments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Receipt Ledger history:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        payments.forEach { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("₹${p.amount} (${p.notes})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                Text(p.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }

            // Preview & revisions tracking box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CLIENT PREVIEW & SCREENINGS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { showRevisionDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Revision", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { showPreviewDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Add Preview", fontSize = 11.sp)
                            }
                        }
                    }

                    if (previews.isEmpty()) {
                        Text("No screening reviews initiated yet.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    } else {
                        previews.forEach { pr ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Version ${pr.version}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(text = pr.notes.ifEmpty { "N/A" }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (pr.status == "Approved") Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = pr.status, fontSize = 10.sp, color = if (pr.status == "Approved") Color(0xFF10B981) else Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Custom Shared notes box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SHARED CREATIVE BRIEF & NOTES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(project.notes.ifEmpty { "No specific creative brief provided." }, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), lineHeight = 18.sp)
                }
            }

            // Activity Log history list
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SYSTEM AUDIT REVISION ACTIVITY LOGS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                    
                    if (activityLogsList.isEmpty()) {
                        Text("No logs compiled yet.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    } else {
                        val formatter = SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault())
                        activityLogsList.forEach { log ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(log.message, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text("By: ${log.author}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                Text(
                                    text = formatter.format(Date(log.timestamp)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // Modal dialog pops
    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Log Payment Milestone Receipt") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = { payAmount = it },
                        label = { Text("Amount Received (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("payment_amt_input")
                    )
                    OutlinedTextField(
                        value = payNotes,
                        onValueChange = { payNotes = it },
                        label = { Text("Receipt details (e.g. UPI, GPay, Bank)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = payAmount.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.addPaymentHistory(project.projectId, amt, payNotes.ifEmpty { "Advance Received" })
                            showPaymentDialog = false
                            payAmount = ""
                            payNotes = ""
                        }
                    },
                    modifier = Modifier.testTag("record_pay_confirm_btn")
                ) {
                    Text("Record payment")
                }
            }
        )
    }

    if (showPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = { Text("Submit Screening Draft") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = previewVer,
                        onValueChange = { previewVer = it },
                        label = { Text("Video Draft Version") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Has Client Approved Draft?", fontSize = 14.sp)
                        Switch(checked = previewApprovedState, onCheckedChange = { previewApprovedState = it })
                    }
                    OutlinedTextField(
                        value = previewNotes,
                        onValueChange = { previewNotes = it },
                        label = { Text("Reviewers notes / screen feedback") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addPreviewHistory(project.projectId, previewVer, previewApprovedState, previewNotes)
                        showPreviewDialog = false
                    }
                ) {
                    Text("Register Preview Status")
                }
            }
        )
    }

    if (showRevisionDialog) {
        AlertDialog(
            onDismissRequest = { showRevisionDialog = false },
            title = { Text("Log Client Revision Request") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = revisionVer,
                        onValueChange = { revisionVer = it },
                        label = { Text("Version demanding correction") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = revisionDetails,
                        onValueChange = { revisionDetails = it },
                        label = { Text("Correction points (e.g. Cut outro, Fix audio)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = revisionEditorNotes,
                        onValueChange = { revisionEditorNotes = it },
                        label = { Text("Instructions to Editor") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addRevisionHistory(project.projectId, revisionVer, revisionDetails, revisionEditorNotes)
                        showRevisionDialog = false
                    }
                ) {
                    Text("Dispatch Correction Ticket")
                }
            }
        )
    }
}

@Composable
fun FinancialColumn(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

// Custom Horizontal Visual Nodes pipeline timeline renderer
@Composable
fun VisualPipelineTimeline(activeStatus: String) {
    val statuses = listOf(
        Pair("Created", "New"),
        Pair("Assigned", "Assigned"),
        Pair("Editing", "Editing"),
        Pair("Preview", "Preview Sent"),
        Pair("Revision", "Revision"),
        Pair("Delivery", "Final Delivery"),
        Pair("Completed", "Completed")
    )

    val activeIndex = statuses.indexOfFirst { it.second == activeStatus }.coerceAtLeast(0)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        statuses.forEachIndexed { idx, (label, value) ->
            val isPassed = idx <= activeIndex
            val nodeColor = if (isPassed) {
                if (activeStatus == "On Hold") Color(0xFFEF4444) 
                else MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Vertical connecting node dot
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(nodeColor, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPassed) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = label,
                        fontWeight = if (idx == activeIndex) FontWeight.ExtraBold else FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (idx == activeIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (idx == activeIndex) {
                        Text(
                            text = "ACTIVE STAGE", 
                            fontSize = 9.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
