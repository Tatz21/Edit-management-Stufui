package com.example.presentation.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Project
import com.example.presentation.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: MainViewModel,
    onNavigateToProjectDetails: (String) -> Unit,
    onNavigateToAddProject: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.filteredProjects.collectAsState()
    val dynamicEditors by viewModel.editorsState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val selectedEditor by viewModel.filterEditor.collectAsState()
    val selectedStatus by viewModel.filterStatus.collectAsState()
    val selectedPayment by viewModel.filterPaymentStatus.collectAsState()
    val selectedPriority by viewModel.filterPriority.collectAsState()
    val selectedSortValue by viewModel.sortType.collectAsState()

    var showFiltersDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Agency Projects", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { showFiltersDialog = true },
                        modifier = Modifier.testTag("filter_icon_button")
                    ) {
                        BadgedBox(badge = {
                            val activeFilters = listOf(selectedEditor, selectedStatus, selectedPayment, selectedPriority).count { it != "All" }
                            if (activeFilters > 0) {
                                Badge { Text(activeFilters.toString()) }
                            }
                        }) {
                            Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filters")
                        }
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("list_settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddProject,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("quick_add_project_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Project")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search Input Row
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search by Client or Project Title...", fontSize = 14.sp) },
                prefix = { Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp).padding(end = 4.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_text_field"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Quick display of active tags
            if (projects.isEmpty()) {
                // Empty state page
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ResizedIcon(imageVector = Icons.Default.FolderOpen, contentDescription = null, size = 64.dp, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No projects match filters", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        Text("Create one using the FAB (+ button) below.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(projects, key = { it.projectId }) { proj ->
                        ProjectItemCard(
                            project = proj,
                            onClick = { onNavigateToProjectDetails(proj.projectId) }
                        )
                    }
                }
            }
        }
    }

    if (showFiltersDialog) {
        AlertDialog(
            onDismissRequest = { showFiltersDialog = false },
            title = { Text("Filter & Sort Workspace", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Sorting options
                    Text("Sort Order", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    CustomDropdownSelector(
                        label = "Sort By",
                        selected = selectedSortValue,
                        options = listOf("Latest", "Oldest", "Deadline", "Revenue"),
                        onSelected = { viewModel.updateSortType(it) }
                    )

                    // Filters
                    Text("Filters", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    
                    CustomDropdownSelector(
                        label = "Assigned Editor",
                        selected = selectedEditor,
                        options = listOf("All") + dynamicEditors,
                        onSelected = { viewModel.updateFilterEditor(it) }
                    )

                    CustomDropdownSelector(
                        label = "Workflow Status",
                        selected = selectedStatus,
                        options = listOf("All") + Project.STATUS_OPTIONS,
                        onSelected = { viewModel.updateFilterStatus(it) }
                    )

                    CustomDropdownSelector(
                        label = "Payment Status",
                        selected = selectedPayment,
                        options = listOf("All") + Project.PAYMENT_STATUS_OPTIONS,
                        onSelected = { viewModel.updateFilterPaymentStatus(it) }
                    )

                    CustomDropdownSelector(
                        label = "Priority Level",
                        selected = selectedPriority,
                        options = listOf("All") + Project.PRIORITY_OPTIONS,
                        onSelected = { viewModel.updateFilterPriority(it) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFiltersDialog = false }) {
                    Text("Apply Filter")
                }
            }
        )
    }
}

// Reuseable custom dropdown style for pristine form selects
@Composable
fun CustomDropdownSelector(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { IconButton(onClick = { expanded = !expanded }) { Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null) } },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(10.dp)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.85f)) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelected(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

// Elegant List Item Project card
@Composable
fun ProjectItemCard(
    project: Project,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (project.totalAmount > 0) (project.advanceAmount / project.totalAmount).toFloat().coerceIn(0f, 1f) else 0f
    
    // Status to colors mappings
    val statusColor = when (project.status) {
        "New" -> StatusNew
        "Assigned" -> StatusAssigned
        "Editing" -> StatusEditing
        "Preview Sent" -> StatusPreviewSent
        "Revision" -> StatusRevision
        "Final Delivery" -> StatusFinalDelivery
        "Completed" -> StatusCompleted
        else -> StatusOnHold
    }

    val priorityColor = when (project.priority) {
        "Low" -> PriorityLow
        "Medium" -> PriorityMedium
        "High" -> PriorityHigh
        else -> PriorityUrgent
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("project_item_${project.projectId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Client Name heading
                Text(
                    text = project.clientName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
                
                // Priority Pill
                Box(
                    modifier = Modifier
                        .background(priorityColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = project.priority, color = priorityColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))

            // Project Title text
            Text(
                text = project.projectTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Assigned Editor
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                ResizedIcon(imageVector = Icons.Default.Person, contentDescription = null, size = 12.dp, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Editor: ${project.assignedEditor}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Deadline and Days left
                Column {
                    Text(text = "Deadline", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ResizedIcon(imageVector = Icons.Default.CalendarToday, contentDescription = null, size = 12.dp, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = project.deadlineDate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    }
                }

                // Payment and progress status
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Payment Status", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (project.paymentStatus == "Fully Paid") Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = project.paymentStatus,
                            color = if (project.paymentStatus == "Fully Paid") Color(0xFF10B981) else Color(0xFFF59E0B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Pipeline workflow status
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Status", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = project.status, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            
            // Render Payment progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = if (project.paymentStatus == "Fully Paid") Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

// Icon Resizer extension helper
@Composable
fun ResizedIcon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: androidx.compose.ui.graphics.Color) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = Modifier.size(size),
        tint = tint
    )
}
