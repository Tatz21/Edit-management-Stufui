package com.example.presentation.clients

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Client
import com.example.domain.model.Invoice
import com.example.domain.model.Project
import com.example.presentation.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentManagementScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark by viewModel.isDarkTheme.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val invoices by viewModel.invoices.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterStatus by remember { mutableStateOf("All") }
    var currentSubTab by remember { mutableStateOf(0) } // 0 = Invoices list, 1 = Client Earnings
    var showAddDialog by remember { mutableStateOf(false) }

    // Dialog form state
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

    // Set default invoice number and dates when dialog is opened
    LaunchedEffect(showAddDialog) {
        if (showAddDialog) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            
            issueDate = sdf.format(calendar.time)
            
            calendar.add(Calendar.DAY_OF_YEAR, 14) // Default 14-day term
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

    // Interactive helper: Auto-fill amount if a project is picked
    LaunchedEffect(selectedProject) {
        selectedProject?.let { p ->
            if (invoiceAmount.isEmpty()) {
                invoiceAmount = p.remainingAmount.toString()
            }
        }
    }

    // Computed states
    val filteredInvoices = remember(invoices, searchQuery, selectedFilterStatus) {
        invoices.filter { invoice ->
            val matchesQuery = invoice.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    invoice.clientName.contains(searchQuery, ignoreCase = true) ||
                    (invoice.projectName?.contains(searchQuery, ignoreCase = true) ?: false)
            
            val matchesStatus = selectedFilterStatus == "All" || invoice.status == selectedFilterStatus
            
            matchesQuery && matchesStatus
        }
    }

    // Client summaries computed reactively
    val clientSummaries = remember(clients, invoices) {
        clients.map { client ->
            val clientInvoices = invoices.filter { it.clientId == client.clientId }
            val totalBilled = clientInvoices.sumOf { it.amount }
            val totalPaid = clientInvoices.filter { it.status == "Paid" }.sumOf { it.amount }
            val totalPending = clientInvoices.filter { it.status == "Pending" }.sumOf { it.amount }
            val totalOverdue = clientInvoices.filter { it.status == "Overdue" }.sumOf { it.amount }
            
            ClientPaymentSummary(
                client = client,
                totalBilled = totalBilled,
                totalPaid = totalPaid,
                totalPending = totalPending,
                totalOverdue = totalOverdue,
                invoiceCount = clientInvoices.size
            )
        }.sortedByDescending { it.totalBilled }
    }

    // Top Header Stats counters (Only Invoice-specific summaries!)
    val totalBilledValue = invoices.sumOf { it.amount }
    val totalPaidValue = invoices.filter { it.status == "Paid" }.sumOf { it.amount }
    val totalPendingValue = invoices.filter { it.status == "Pending" }.sumOf { it.amount }
    val totalOverdueValue = invoices.filter { it.status == "Overdue" }.sumOf { it.amount }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Payment & Billing", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("payments_add_invoice_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCard,
                            contentDescription = "New Invoice Node",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentSubTab == 0 && clients.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.PostAdd, contentDescription = null) },
                    text = { Text("New Invoice") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("payments_fab")
                )
            }
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
            // 1. KPI Stats Summary Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Earned (Paid)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1B4332).copy(alpha = 0.15f) else Color(0xFFE8F5E9)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Earnings", style = MaterialTheme.typography.labelSmall, color = if (isDark) Color(0xFF52B788) else Color(0xFF1B4332))
                        Text(
                            text = String.format(Locale.getDefault(), "$%.0f", totalPaidValue),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFF52B788) else Color(0xFF1B4332)
                        )
                    }
                }

                // Total Outstanding (Pending)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF782E00).copy(alpha = 0.12f) else Color(0xFFFFF3E0)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Pending", style = MaterialTheme.typography.labelSmall, color = if (isDark) Color(0xFFFFA200) else Color(0xFFE65100))
                        Text(
                            text = String.format(Locale.getDefault(), "$%.0f", totalPendingValue),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFFFFA200) else Color(0xFFE65100)
                        )
                    }
                }

                // Total Overdue
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF9B2226).copy(alpha = 0.15f) else Color(0xFFFFEBEE)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Overdue", style = MaterialTheme.typography.labelSmall, color = if (isDark) Color(0xFFE63946) else Color(0xFF9B2226))
                        Text(
                            text = String.format(Locale.getDefault(), "$%.0f", totalOverdueValue),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFFE63946) else Color(0xFF9B2226)
                        )
                    }
                }
            }

            // 2. Custom Segments Subtabs control
            PrimaryTabRow(
                selectedTabIndex = currentSubTab,
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = currentSubTab == 0,
                    onClick = { currentSubTab = 0 },
                    text = { Text("Invoice Ledger", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = currentSubTab == 1,
                    onClick = { currentSubTab = 1 },
                    text = { Text("Client Earnings", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // 3. Dynamic Sub-views matching active Subtab
            AnimatedContent(
                targetState = currentSubTab,
                transitionSpec = {
                    fadeIn(animationSpec = spring()) + scaleIn(initialScale = 0.95f) togetherWith
                            fadeOut(animationSpec = spring())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "Subtab transition"
            ) { activeTab ->
                if (activeTab == 0) {
                    // TAB 0: Invoices List
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Search Outlined Box
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search invoice ID, client or project...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        // Status filter row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val filters = listOf("All", "Paid", "Pending", "Overdue")
                            filters.forEach { status ->
                                val isSelected = selectedFilterStatus == status
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFilterStatus = status },
                                    label = { Text(status, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("invoice_filter_chip_$status")
                                )
                            }
                        }

                        // Listing invoices
                        if (filteredInvoices.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                    )
                                    Text(
                                        text = if (invoices.isEmpty()) "No billing records found" else "No matching invoices",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (invoices.isEmpty()) {
                                        Text(
                                            text = if (clients.isEmpty()) {
                                                "You must register a Client Profile first in the Clients tab."
                                            } else {
                                                "Generate invoices linked to your editing projects."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        if (clients.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Button(
                                                onClick = { showAddDialog = true },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Create Bill Entry")
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 64.dp)
                            ) {
                                items(filteredInvoices, key = { it.invoiceId }) { invoice ->
                                    InvoiceRowCard(
                                        invoice = invoice,
                                        isDark = isDark,
                                        onDelete = { viewModel.deleteInvoice(invoice.invoiceId) },
                                        onStatusChange = { newStatus ->
                                            viewModel.addOrUpdateInvoice(invoice.copy(status = newStatus))
                                            Toast.makeText(context, "${invoice.invoiceNumber} status updated to $newStatus!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // TAB 1: Client Earnings summaries list
                    if (clientSummaries.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                                Text(
                                    text = "Your client registry is currently empty.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 64.dp)
                        ) {
                            items(clientSummaries, key = { it.client.clientId }) { summary ->
                                ClientEarningsRowCard(
                                    summary = summary,
                                    isDark = isDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. Create Invoice Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PostAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Issue Invoice Record", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Client Selection Dropdown
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
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )

                        DropdownMenu(
                            expanded = clientDropdownExpanded,
                            onDismissRequest = { clientDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            clients.forEach { client ->
                                DropdownMenuItem(
                                    text = { Text(client.name) },
                                    onClick = {
                                        selectedClient = client
                                        selectedProject = null // Reset project selection
                                        clientDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Optional Project Connection Dropdown
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
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                DropdownMenu(
                                    expanded = projectDropdownExpanded,
                                    onDismissRequest = { projectDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.85f)
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

                    // Invoice Number Input
                    OutlinedTextField(
                        value = customInvoiceNumber,
                        onValueChange = { customInvoiceNumber = it },
                        label = { Text("Invoice Serial Number *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    // Billable Amount Input
                    OutlinedTextField(
                        value = invoiceAmount,
                        onValueChange = { invoiceAmount = it },
                        label = { Text("Invoiced Billable Amount ($) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        prefix = { Text("$ ") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    // Payment Status dropdown spinner representation
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = invoiceStatus,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Current Invoice Status") },
                            modifier = Modifier.fillMaxWidth().clickable { statusDropdownExpanded = true },
                            trailingIcon = {
                                IconButton(onClick = { statusDropdownExpanded = !statusDropdownExpanded }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Toggle status options")
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

                    // Dates: Issue Date & Due Date Fields
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
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )

                        OutlinedTextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            label = { Text("Due Date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    // Receipt Notes
                    OutlinedTextField(
                        value = invoiceNotes,
                        onValueChange = { invoiceNotes = it },
                        label = { Text("Notes / Descriptions") },
                        placeholder = { Text("e.g., Billing for 2nd milestone draft") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val client = selectedClient
                        val amountParsed = invoiceAmount.trim().toDoubleOrNull()
                        
                        if (client == null) {
                            Toast.makeText(context, "Please select an existing registered client", Toast.LENGTH_SHORT).show()
                        } else if (customInvoiceNumber.trim().isEmpty()) {
                            Toast.makeText(context, "Please configure a sequential Invoice Number", Toast.LENGTH_SHORT).show()
                        } else if (amountParsed == null || amountParsed <= 0.0) {
                            Toast.makeText(context, "Please specify a numeric billable amount > 0.0", Toast.LENGTH_SHORT).show()
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
                            showAddDialog = false
                            Toast.makeText(context, "Successfully created ${newInvoice.invoiceNumber}!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("dialog_invoice_confirm")
                ) {
                    Text("Issue Invoice")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun InvoiceRowCard(
    invoice: Invoice,
    isDark: Boolean,
    onDelete: () -> Unit,
    onStatusChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpandedMenu by remember { mutableStateOf(false) }
    
    val statusColor = when (invoice.status) {
        "Paid" -> Color(0xFF10B981)
        "Pending" -> Color(0xFFF59E0B)
        "Overdue" -> Color(0xFFEF4444)
        else -> Color(0xFF6B7280)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("invoice_row_${invoice.invoiceId}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: INV number, amount, status pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = statusColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = invoice.invoiceNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Billed: $${String.format(Locale.getDefault(), "%,.2f", invoice.amount)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Interactive Status Badge Dropdown trigger
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
                            .clickable { isExpandedMenu = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(statusColor, RoundedCornerShape(50.dp))
                        )
                        Text(
                            text = invoice.status,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "change status",
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isExpandedMenu,
                        onDismissRequest = { isExpandedMenu = false }
                    ) {
                        Invoice.STATUS_OPTIONS.forEach { state ->
                            DropdownMenuItem(
                                text = { Text(state) },
                                onClick = {
                                    onStatusChange(state)
                                    isExpandedMenu = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))

            // Body info
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Client Link
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Client: ${invoice.clientName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Project Link
                invoice.projectName?.let { pName ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Project: $pName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (invoice.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = invoice.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Footer: Due Date & Delete trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (invoice.status == "Overdue") Icons.Default.Warning else Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (invoice.status == "Overdue") Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Due: ${invoice.dueDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (invoice.status == "Overdue") Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp).testTag("delete_invoice_${invoice.invoiceId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete record",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ClientEarningsRowCard(
    summary: ClientPaymentSummary,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val totalBilled = summary.totalBilled
    val collectedPercent = if (totalBilled > 0.0) summary.totalPaid / totalBilled else 0.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.client.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Total Billing Transacted: $${String.format(Locale.getDefault(), "%,.0f", summary.totalBilled)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "$%,.0f", summary.totalPaid),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = "Total Earnings",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Ratio metric Progression Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { collectedPercent.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Color(0xFF10B981),
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.0f%% collected", collectedPercent * 100),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${summary.invoiceCount} invoices",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Expandable segment detailing outstanding splits
            if (isExpanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                
                Column(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ClientBalanceLine(
                        label = "Fully Settled", 
                        value = summary.totalPaid, 
                        color = Color(0xFF10B981),
                        icon = Icons.Default.CheckCircle
                    )
                    ClientBalanceLine(
                        label = "Pending Process", 
                        value = summary.totalPending, 
                        color = Color(0xFFF59E0B),
                        icon = Icons.Default.HourglassEmpty
                    )
                    ClientBalanceLine(
                        label = "Overdue Unpaid", 
                        value = summary.totalOverdue, 
                        color = Color(0xFFEF4444),
                        icon = Icons.Default.Warning
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientBalanceLine(
    label: String,
    value: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = String.format(Locale.getDefault(), "$%,.2f", value),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (value > 0.0) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

// Data holder
data class ClientPaymentSummary(
    val client: Client,
    val totalBilled: Double,
    val totalPaid: Double,
    val totalPending: Double,
    val totalOverdue: Double,
    val invoiceCount: Int
)
