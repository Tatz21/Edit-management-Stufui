package com.example.presentation.clients

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import com.example.domain.model.Project
import com.example.presentation.MainViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDirectoryScreen(
    viewModel: MainViewModel,
    onNavigateToProjectDetails: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clients by viewModel.clients.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterStatus by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedClientForDetail by remember { mutableStateOf<Client?>(null) }

    // Dialog state
    var clientName by remember { mutableStateOf("") }
    var clientEmail by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var clientPaymentStatus by remember { mutableStateOf("Pending") }
    var clientNotes by remember { mutableStateOf("") }

    var displayDropdownMenu by remember { mutableStateOf(false) }

    // Filtered clients list
    val filteredClients = remember(clients, searchQuery, selectedFilterStatus) {
        clients.filter { client ->
            val matchesSearch = client.name.contains(searchQuery, ignoreCase = true) ||
                    client.email.contains(searchQuery, ignoreCase = true) ||
                    client.phone.contains(searchQuery, ignoreCase = true) ||
                    client.notes.contains(searchQuery, ignoreCase = true)
            
            val matchesFilter = selectedFilterStatus == "All" || client.paymentStatus == selectedFilterStatus
            
            matchesSearch && matchesFilter
        }
    }

    // Counts
    val totalCount = clients.size
    val pendingCount = clients.count { it.paymentStatus == "Pending" }
    val partialCount = clients.count { it.paymentStatus == "Partially Paid" }
    val fullyPaidCount = clients.count { it.paymentStatus == "Fully Paid" }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Client Directory", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_client_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Client Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("New Profile") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_client_fab")
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
            // 1. KPI Stats Summary Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total clients
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$totalCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    }
                }

                // Pending Collection status
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
                        Text("Pending", style = MaterialTheme.typography.labelSmall, color = if (isDark) Color(0xFFE63946) else Color(0xFF9B2226))
                        Text("$pendingCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = if (isDark) Color(0xFFE63946) else Color(0xFF9B2226))
                    }
                }

                // Fully paid
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
                        Text("Fully Paid", style = MaterialTheme.typography.labelSmall, color = if (isDark) Color(0xFF52B788) else Color(0xFF1B4332))
                        Text("$fullyPaidCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = if (isDark) Color(0xFF52B788) else Color(0xFF1B4332))
                    }
                }
            }

            // 2. Search Outlined Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search client directory...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear text")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("client_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // 3. Horizontal Status Filter Scrollable Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filters = listOf("All", "Pending", "Partially Paid", "Fully Paid")
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
                        modifier = Modifier.testTag("filter_chip_$status")
                    )
                }
            }

            // 4. Clients Listing or Empty state
            if (filteredClients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = if (clients.isEmpty()) "Your Client Directory is empty" else "No matching clients found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (clients.isEmpty()) {
                            Text(
                                text = "Create client profiles with contact and payment tracking.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Your First Client")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("client_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredClients, key = { it.clientId }) { client ->
                        val clientProjects = remember(projects, client) {
                            projects.filter {
                                (client.email.isNotEmpty() && it.clientEmail.equals(client.email, ignoreCase = true)) ||
                                        it.clientName.equals(client.name, ignoreCase = true)
                            }
                        }
                        ClientRowItem(
                            client = client,
                            clientProjects = clientProjects,
                            isDark = isDark,
                            onDelete = { viewModel.deleteClient(client.clientId) },
                            onCall = { phone ->
                                try {
                                    val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    dial.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(dial)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot dial. No dialer available.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onEmail = { email ->
                                try {
                                    val mail = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                                    mail.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(mail)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No email client available.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onViewDetail = { selectedClientForDetail = client }
                        )
                    }
                }
            }
        }
    }

    // 5. Add Client dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Add Client Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Name
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client Name *") },
                        placeholder = { Text("John Doe") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("client_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    // Email
                    OutlinedTextField(
                        value = clientEmail,
                        onValueChange = { clientEmail = it },
                        label = { Text("Email Contact") },
                        placeholder = { Text("john@example.com") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("client_email_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    // Phone
                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("Phone Contact") },
                        placeholder = { Text("+91 9876543210") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("client_phone_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    // Payment Status dropdown spinner representation
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = clientPaymentStatus,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Global Payment Status") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("client_payment_status")
                                .clickable { displayDropdownMenu = true },
                            trailingIcon = {
                                IconButton(onClick = { displayDropdownMenu = !displayDropdownMenu }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Toggle status options")
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )

                        DropdownMenu(
                            expanded = displayDropdownMenu,
                            onDismissRequest = { displayDropdownMenu = false }
                        ) {
                            Client.PAYMENT_STATUS_OPTIONS.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        clientPaymentStatus = option
                                        displayDropdownMenu = false
                                    },
                                    modifier = Modifier.testTag("dropdown_item_$option")
                                )
                            }
                        }
                    }

                    // Notes
                    OutlinedTextField(
                        value = clientNotes,
                        onValueChange = { clientNotes = it },
                        label = { Text("Notes / Company Details") },
                        placeholder = { Text("e.g. Lead editor, premium branding corp") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("client_notes_input"),
                        minLines = 2,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (clientName.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter a valid client name", Toast.LENGTH_SHORT).show()
                        } else {
                            val profile = Client(
                                clientId = UUID.randomUUID().toString(),
                                name = clientName.trim(),
                                email = clientEmail.trim(),
                                phone = clientPhone.trim(),
                                notes = clientNotes.trim(),
                                paymentStatus = clientPaymentStatus
                            )
                            viewModel.addOrUpdateClient(profile)
                            
                            // Reset state
                            clientName = ""
                            clientEmail = ""
                            clientPhone = ""
                            clientPaymentStatus = "Pending"
                            clientNotes = ""
                            showAddDialog = false
                            
                            Toast.makeText(context, "Client Profile successfully verified and saved!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("dialog_confirm_button")
                ) {
                    Text("Save Record")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        clientName = ""
                        clientEmail = ""
                        clientPhone = ""
                        clientPaymentStatus = "Pending"
                        clientNotes = ""
                        showAddDialog = false
                    },
                    modifier = Modifier.testTag("dialog_dismiss_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (selectedClientForDetail != null) {
        val detailProjects = remember(projects, selectedClientForDetail) {
            projects.filter {
                (selectedClientForDetail!!.email.isNotEmpty() && it.clientEmail.equals(selectedClientForDetail!!.email, ignoreCase = true)) ||
                        it.clientName.equals(selectedClientForDetail!!.name, ignoreCase = true)
            }
        }
        ModalBottomSheet(
            onDismissRequest = { selectedClientForDetail = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            ClientDetailSheetContent(
                client = selectedClientForDetail!!,
                clientProjects = detailProjects,
                isDark = isDark,
                onDismiss = { selectedClientForDetail = null },
                onSaveProfile = { updatedClient ->
                    viewModel.addOrUpdateClient(updatedClient)
                    selectedClientForDetail = updatedClient
                },
                onNavigateToProjectDetails = { id ->
                    selectedClientForDetail = null
                    onNavigateToProjectDetails?.invoke(id)
                },
                onCall = { phone ->
                    try {
                        val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                        dial.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(dial)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot dial. No dialer available.", Toast.LENGTH_SHORT).show()
                    }
                },
                onEmail = { email ->
                    try {
                        val mail = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                        mail.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(mail)
                    } catch (e: Exception) {
                        Toast.makeText(context, "No email client available.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailSheetContent(
    client: Client,
    clientProjects: List<Project>,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSaveProfile: (Client) -> Unit,
    onNavigateToProjectDetails: (String) -> Unit,
    onCall: (String) -> Unit,
    onEmail: (String) -> Unit
) {
    var activeTab by remember { mutableStateOf("Overview") }
    val totalRevenue = remember(clientProjects) { clientProjects.sumOf { it.totalAmount } }
    val totalRemaining = remember(clientProjects) { clientProjects.sumOf { it.remainingAmount } }
    val totalPaid = remember(clientProjects, totalRevenue, totalRemaining) { (totalRevenue - totalRemaining).coerceAtLeast(0.0) }
    
    val activeCount = remember(clientProjects) {
        clientProjects.count { it.status in listOf("New", "Assigned", "Editing", "Preview Sent", "Revision") }
    }
    val completedCount = remember(clientProjects) {
        clientProjects.count { it.status in listOf("Final Delivery", "Completed") }
    }

    // Editing states for Overview Profile
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(client.name) }
    var editEmail by remember { mutableStateOf(client.email) }
    var editPhone by remember { mutableStateOf(client.phone) }
    var editNotes by remember { mutableStateOf(client.notes) }
    var editPaymentStatus by remember { mutableStateOf(client.paymentStatus) }
    var showEditStatusDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f) // Occupies up to 90% of screen height
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular initials avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = if (client.name.isNotBlank()) client.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase() else ""
                    Text(
                        text = initials.ifBlank { "CL" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        text = "Client Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.testTag("client_detail_close_btn")) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close details")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom High-End Tab/Switcher row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(100.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf("Overview", "Project Ledger")
            tabs.forEach { tabName ->
                val isSelected = activeTab == tabName
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(100.dp)
                        )
                        .clickable { activeTab = tabName }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab contents
        if (activeTab == "Overview") {
            // Scrollable Profile Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Toggle between View vs Edit profile
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Contact Information",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            // Edit toggle button
                            TextButton(
                                onClick = {
                                    if (isEditing) {
                                        // Revert changes
                                        editName = client.name
                                        editEmail = client.email
                                        editPhone = client.phone
                                        editNotes = client.notes
                                        editPaymentStatus = client.paymentStatus
                                    }
                                    isEditing = !isEditing
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEditing) "Cancel" else "Edit Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!isEditing) {
                            // Read-only beautiful presentation
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Name Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Full Name", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(client.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }
                                }

                                // Phone Row
                                if (client.phone.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { onCall(client.phone) },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Phone Details", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(client.phone, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        }
                                        IconButton(onClick = { onCall(client.phone) }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Call, contentDescription = "Dial", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }

                                // Email Row
                                if (client.email.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { onEmail(client.email) },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Email Contact", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(client.email, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        }
                                        IconButton(onClick = { onEmail(client.email) }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Mail, contentDescription = "Send Email", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }

                                // Payment Status Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Global Payment Status", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val statusColors = getClientStatusColors(client.paymentStatus, isDark)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier
                                                .background(statusColors.bg, RoundedCornerShape(100.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Box(modifier = Modifier.size(6.dp).background(statusColors.text, RoundedCornerShape(50.dp)))
                                            Text(client.paymentStatus, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColors.text)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Active edit forms
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    label = { Text("Client Name") },
                                    modifier = Modifier.fillMaxWidth().testTag("edit_client_name"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = editEmail,
                                    onValueChange = { editEmail = it },
                                    label = { Text("Email Contact") },
                                    modifier = Modifier.fillMaxWidth().testTag("edit_client_email"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = editPhone,
                                    onValueChange = { editPhone = it },
                                    label = { Text("Phone Contact") },
                                    modifier = Modifier.fillMaxWidth().testTag("edit_client_phone"),
                                    singleLine = true
                                )

                                // Dropdown selector for status
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = editPaymentStatus,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Payment Status") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("edit_client_status")
                                            .clickable { showEditStatusDropdown = true },
                                        trailingIcon = {
                                            IconButton(onClick = { showEditStatusDropdown = !showEditStatusDropdown }) {
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        }
                                    )

                                    DropdownMenu(
                                        expanded = showEditStatusDropdown,
                                        onDismissRequest = { showEditStatusDropdown = false }
                                    ) {
                                        Client.PAYMENT_STATUS_OPTIONS.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    editPaymentStatus = option
                                                    showEditStatusDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (editName.isBlank()) {
                                            // Handle error
                                        } else {
                                            val updated = client.copy(
                                                name = editName.trim(),
                                                email = editEmail.trim(),
                                                phone = editPhone.trim(),
                                                notes = editNotes.trim(),
                                                paymentStatus = editPaymentStatus
                                            )
                                            onSaveProfile(updated)
                                            isEditing = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_client_edit_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Save Registry Changes", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Meeting Notes & Internal feedback Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Jot Client Notes & Feedback",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        placeholder = { Text("Type here to save internal details regarding this client...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(120.dp).testTag("edit_client_notes_field"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        )
                    )

                    // Quick Appending Suggestion Tags
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val feedbackTags = listOf(
                            "VIP Client - High Priority",
                            "Awaiting Asset Delivery",
                            "Prefers WhatsApp Updates",
                            "Requested Revision",
                            "Awaiting Final Invoice"
                        )
                        feedbackTags.forEach { tagText ->
                            SuggestionChip(
                                onClick = {
                                    val formattedTag = if (editNotes.isBlank()) tagText else if (editNotes.endsWith("\n") || editNotes.endsWith(" ")) tagText else "\n• $tagText"
                                    editNotes += formattedTag
                                },
                                label = { Text(tagText, fontSize = 10.sp) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val updated = client.copy(notes = editNotes.trim())
                            onSaveProfile(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.align(Alignment.End).testTag("save_client_notes_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Memo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Tab 2: Project Ledger & Pipeline History
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Statistics Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Total Value
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Total Paid", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$%,.0f".format(Locale.US, totalPaid), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Remaining / Outstanding
                    val outstandingColor = if (totalRemaining > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = outstandingColor.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, outstandingColor.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Pending Bills", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$%,.0f".format(Locale.US, totalRemaining), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = outstandingColor)
                        }
                    }

                    // Projects pipeline count
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Projects Active", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$activeCount / ${clientProjects.size}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                    }
                }

                // Project History List
                Text(
                    text = "Historical Pipeline & History (${clientProjects.size})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (clientProjects.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Text("No projects recorded under this client profile.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(clientProjects, key = { it.projectId }) { proj ->
                            val statusColor = when (proj.status) {
                                "New", "Assigned", "Editing" -> Color(0xFFF59E0B)
                                "Preview Sent", "Revision" -> Color(0xFF0EA5E9)
                                "Final Delivery", "Completed" -> Color(0xFF10B981)
                                else -> Color(0xFF94A3B8)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToProjectDetails(proj.projectId) }
                                    .testTag("ledger_proj_item_${proj.projectId}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(proj.projectTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${proj.projectType} • Deadline: ${proj.deadlineDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        
                                        // Simple Progress Indicators
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                color = statusColor.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = proj.status,
                                                    color = statusColor,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Text(
                                                text = "Unpaid: $%,.0f".format(Locale.US, proj.remainingAmount),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (proj.remainingAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Details",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClientRowItem(
    client: Client,
    clientProjects: List<Project>,
    isDark: Boolean,
    onDelete: () -> Unit,
    onCall: (String) -> Unit,
    onEmail: (String) -> Unit,
    onViewDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = remember(client.paymentStatus, isDark) {
        getClientStatusColors(client.paymentStatus, isDark)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onViewDetail() }
            .testTag("client_item_${client.clientId}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Name / Status & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = client.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (client.notes.isNotEmpty()) {
                            Text(
                                text = client.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Vision details action
                    IconButton(
                        onClick = onViewDetail,
                        modifier = Modifier.size(32.dp).testTag("client_view_btn_${client.clientId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "View Profile info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Payment Status Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(statusColors.bg, RoundedCornerShape(100.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(statusColors.text, RoundedCornerShape(50.dp))
                        )
                        Text(
                            text = client.paymentStatus,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColors.text
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("client_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete client",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Body: Email & Phone contacts
            if (client.email.isNotEmpty() || client.phone.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (client.phone.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.clickable { onCall(client.phone) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = client.phone,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (client.email.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.clickable { onEmail(client.email) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = client.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Floating Communication Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (client.phone.isNotEmpty()) {
                            IconButton(
                                onClick = { onCall(client.phone) },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Place call",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (client.email.isNotEmpty()) {
                            IconButton(
                                onClick = { onEmail(client.email) },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mail,
                                    contentDescription = "Send email",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (clientProjects.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                
                var isExpanded by remember { mutableStateOf(false) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Linked Projects (${clientProjects.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle project details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    if (isExpanded) {
                        clientProjects.forEach { project ->
                            val displayStatus = when (project.status) {
                                "New", "Assigned", "Editing" -> "In Progress"
                                "Preview Sent", "Revision" -> "Review"
                                "Final Delivery", "Completed" -> "Finalized"
                                else -> "On Hold"
                            }
                            val statusColor = when (displayStatus) {
                                "In Progress" -> Color(0xFFF59E0B)
                                "Review" -> Color(0xFF0EA5E9)
                                "Finalized" -> Color(0xFF10B981)
                                else -> Color(0xFF94A3B8)
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = project.projectTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Est. Completion: ${project.deadlineDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(statusColor, RoundedCornerShape(50.dp))
                                    )
                                    Text(
                                        text = displayStatus,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = statusColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ClientTheme(val bg: Color, val text: Color)

private fun getClientStatusColors(status: String, isDark: Boolean): ClientTheme {
    return when (status) {
        "Pending" -> ClientTheme(
            bg = if (isDark) Color(0xFF9B2226).copy(alpha = 0.15f) else Color(0xFFFFEBEE),
            text = if (isDark) Color(0xFFE63946) else Color(0xFF9B2226)
        )
        "Partially Paid" -> ClientTheme(
            bg = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFFFEF3C7),
            text = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
        )
        "Fully Paid" -> ClientTheme(
            bg = if (isDark) Color(0xFF1B4332).copy(alpha = 0.15f) else Color(0xFFE8F5E9),
            text = if (isDark) Color(0xFF52B788) else Color(0xFF1B4332)
        )
        else -> ClientTheme(
            bg = if (isDark) Color(0xFF3F37C9).copy(alpha = 0.15f) else Color(0xFFE8EAF6),
            text = if (isDark) Color(0xFF4895EF) else Color(0xFF3F37C9)
        )
    }
}
