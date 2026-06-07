package com.example.presentation.clients

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import com.example.presentation.MainViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDirectoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val clients by viewModel.clients.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterStatus by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }

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
                        ClientRowItem(
                            client = client,
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
                            }
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
}

@Composable
fun ClientRowItem(
    client: Client,
    isDark: Boolean,
    onDelete: () -> Unit,
    onCall: (String) -> Unit,
    onEmail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = remember(client.paymentStatus, isDark) {
        getClientStatusColors(client.paymentStatus, isDark)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
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
