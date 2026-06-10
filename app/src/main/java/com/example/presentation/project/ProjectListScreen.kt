package com.example.presentation.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
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
    var selectedProjectForNotes by remember { mutableStateOf<Project?>(null) }

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
        var viewMode by remember { mutableStateOf("table") } // Default to "table" tracker view mode
        var activeOnlyFilter by remember { mutableStateOf(true) } // Default to tracking active projects
        var selectedStatusFilter by remember { mutableStateOf("All Statuses") }

        val displayedProjects = remember(projects, activeOnlyFilter, selectedStatusFilter) {
            val baseList = if (activeOnlyFilter) {
                projects.filter { it.status != "Completed" && it.status != "Final Delivery" }
            } else {
                projects
            }
            if (selectedStatusFilter == "All Statuses") {
                baseList
            } else {
                baseList.filter { proj ->
                    val projDisplayStatus = when (proj.status) {
                        "New", "Assigned", "Editing" -> "In Progress"
                        "Preview Sent", "Revision" -> "Review"
                        "Final Delivery", "Completed" -> "Completed"
                        else -> "On Hold"
                    }
                    projDisplayStatus == selectedStatusFilter
                }
            }
        }

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
            
            Spacer(modifier = Modifier.height(12.dp))

            // Control & Toggles Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Active only filter chips with Material 3 polish
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = activeOnlyFilter,
                        onClick = { activeOnlyFilter = true },
                        label = { Text("Active Pipe", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = !activeOnlyFilter,
                        onClick = { activeOnlyFilter = false },
                        label = { Text("All Archive", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                // Right side: Custom Segmented switcher (List vs Table)
                Row(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val activeBg = MaterialTheme.colorScheme.primary
                    val activeContent = MaterialTheme.colorScheme.onPrimary
                    val inactiveBg = Color.Transparent
                    val inactiveContent = MaterialTheme.colorScheme.onSurfaceVariant

                    // Cards (List) Mode
                    Row(
                        modifier = Modifier
                            .background(
                                color = if (viewMode == "list") activeBg else inactiveBg,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewMode = "list" }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Card layout",
                            tint = if (viewMode == "list") activeContent else inactiveContent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Cards",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewMode == "list") activeContent else inactiveContent
                        )
                    }

                    // Table Mode
                    Row(
                        modifier = Modifier
                            .background(
                                color = if (viewMode == "table") activeBg else inactiveBg,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewMode = "table" }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Table tracking",
                            tint = if (viewMode == "table") activeContent else inactiveContent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Table",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewMode == "table") activeContent else inactiveContent
                        )
                    }
                }
            }
            
            // Dropdown filter for project statuses: In Progress, Review, Completed, On Hold
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var expanded by remember { mutableStateOf(false) }
                val statuses = listOf("All Statuses", "In Progress", "Review", "Completed", "On Hold")

                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        onClick = { expanded = !expanded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterAlt,
                                    contentDescription = "Status filter",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Status: $selectedStatusFilter",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.55f)
                    ) {
                        statuses.forEach { statusOption ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (statusOption != "All Statuses") {
                                            val statusColor = when (statusOption) {
                                                "In Progress" -> StatusEditing
                                                "Review" -> StatusPreviewSent
                                                "Completed" -> StatusCompleted
                                                else -> StatusOnHold
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(statusColor, RoundedCornerShape(50.dp))
                                            )
                                        }
                                        Text(statusOption, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                },
                                onClick = {
                                    selectedStatusFilter = statusOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedStatusFilter != "All Statuses") {
                    Surface(
                        onClick = { selectedStatusFilter = "All Statuses" },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Status Filter",
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Quick display of active tags / Empty states
            if (displayedProjects.isEmpty()) {
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
                        Text(
                            text = if (activeOnlyFilter) "No active projects" else "No matching projects",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Text(
                            text = if (activeOnlyFilter) "All pipelines are completed! Switch to All Archive to view." else "Create one using the FAB (+ button) below.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                if (viewMode == "list") {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(displayedProjects, key = { it.projectId }) { proj ->
                            ProjectItemCard(
                                project = proj,
                                onClick = { onNavigateToProjectDetails(proj.projectId) },
                                onOpenNotes = { selectedProjectForNotes = proj }
                            )
                        }
                    }
                } else {
                    // Modern responsive tracker table (Horizontally scrollable on small screens, neatly scaling layout)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val tableScrollState = rememberScrollState()
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 64.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(tableScrollState)
                            ) {
                                // Table Header row
                                Row(
                                    modifier = Modifier
                                        .widthIn(min = 750.dp)
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Project / Client",
                                        modifier = Modifier.weight(3.0f),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Workflow Status",
                                        modifier = Modifier.weight(2.0f),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Timeline Gap",
                                        modifier = Modifier.weight(2.8f),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Completion Tracker",
                                        modifier = Modifier.weight(2.2f),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Assigned Editor",
                                        modifier = Modifier.weight(1.8f),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Quick Notes",
                                        modifier = Modifier.weight(1.2f),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                
                                // Table Row elements
                                LazyColumn(
                                    modifier = Modifier
                                        .widthIn(min = 750.dp)
                                        .fillMaxWidth()
                                ) {
                                    items(displayedProjects, key = { it.projectId }) { proj ->
                                        ProjectTableRow(
                                            project = proj,
                                            onClick = { onNavigateToProjectDetails(proj.projectId) },
                                            onOpenNotes = { selectedProjectForNotes = proj }
                                        )
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                                    }
                                }
                            }
                        }
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

    if (selectedProjectForNotes != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedProjectForNotes = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            ProjectNotesSheetContent(
                project = selectedProjectForNotes!!,
                onDismiss = { selectedProjectForNotes = null },
                onSaveNotes = { newNotes ->
                    viewModel.updateNotes(selectedProjectForNotes!!.projectId, newNotes)
                    selectedProjectForNotes = null
                }
            )
        }
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
    onOpenNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (project.totalAmount > 0) (project.advanceAmount / project.totalAmount).toFloat().coerceIn(0f, 1f) else 0f
    
    // Group status to display-friendly grouped label and color mapping
    val displayStatus = when (project.status) {
        "New", "Assigned", "Editing" -> "In Progress"
        "Preview Sent", "Revision" -> "Review"
        "Final Delivery", "Completed" -> "Completed"
        else -> "On Hold"
    }
    
    val statusColor = when (displayStatus) {
        "In Progress" -> StatusEditing
        "Review" -> StatusPreviewSent
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

            Spacer(modifier = Modifier.height(10.dp))
            
            // NEW Delivery & Timeline Tracking Block
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1st: Progress Gauge for Completion Status
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PRODUCTION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(54.dp)) {
                            val completion = getCompletionProgress(project.status)
                            CircularProgressIndicator(
                                progress = { completion },
                                modifier = Modifier.fillMaxSize(),
                                color = statusColor,
                                strokeWidth = 5.dp,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            Text(
                                text = "${(completion * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = displayStatus,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }

                    // Divider separator line
                    Box(
                        modifier = Modifier
                            .height(50.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    )

                    // 2nd: Progress Gauge for Deadline Time Consumed / Days Left
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val dlInfo = getDeadlineProximity(project.startDate, project.deadlineDate)
                        
                        Text(
                            text = "TIMELINE GAP",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(54.dp)) {
                            val timeConsumed = dlInfo.ratio
                            val timelineColor = when {
                                dlInfo.isOverdue -> MaterialTheme.colorScheme.error
                                dlInfo.daysRemaining <= 2 -> PriorityHigh // Red/orange for hot deadlines
                                else -> MaterialTheme.colorScheme.primary
                            }
                            CircularProgressIndicator(
                                progress = { timeConsumed },
                                modifier = Modifier.fillMaxSize(),
                                color = timelineColor,
                                strokeWidth = 5.dp,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            Icon(
                                imageVector = if (dlInfo.isOverdue) Icons.Default.Warning else Icons.Default.HourglassEmpty,
                                contentDescription = null,
                                tint = timelineColor.copy(alpha = 0.65f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dlInfo.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (dlInfo.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
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
                    Text(text = "Est. Completion", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(statusColor, RoundedCornerShape(50.dp))
                        )
                        Text(
                            text = displayStatus,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
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

            Spacer(modifier = Modifier.height(12.dp))

            // PM Actions Quick Notes Button
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { onOpenNotes() },
                    modifier = Modifier.testTag("notes_quick_btn_${project.projectId}").height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (project.notes.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (project.notes.isNotBlank()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Quick Notes",
                        modifier = Modifier.size(13.dp),
                        tint = if (project.notes.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (project.notes.isNotBlank()) "PM Notes (Active)" else "PM Notes",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (project.notes.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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

// Elegant responsive project tracker table row
@Composable
fun ProjectTableRow(
    project: Project,
    onClick: () -> Unit,
    onOpenNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayStatus = when (project.status) {
        "New", "Assigned", "Editing" -> "In Progress"
        "Preview Sent", "Revision" -> "Review"
        "Final Delivery", "Completed" -> "Completed"
        else -> "On Hold"
    }
    
    val statusColor = when (displayStatus) {
        "In Progress" -> StatusEditing
        "Review" -> StatusPreviewSent
        "Completed" -> StatusCompleted
        else -> StatusOnHold
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Title and Client Details
        Column(modifier = Modifier.weight(3.0f)) {
            Text(
                text = project.projectTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = project.clientName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 2. Status Badge representation
        Box(modifier = Modifier.weight(2.0f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(100.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(statusColor, RoundedCornerShape(50.dp))
                )
                Text(
                    text = displayStatus,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // 3. Timeline Gap with days left and mini visual timeline progress bar
        val dlInfo = getDeadlineProximity(project.startDate, project.deadlineDate)
        val timelineColor = when {
            dlInfo.isOverdue -> MaterialTheme.colorScheme.error
            dlInfo.daysRemaining <= 2 -> PriorityHigh
            else -> MaterialTheme.colorScheme.primary
        }
        Column(
            modifier = Modifier.weight(2.8f).padding(end = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (dlInfo.isOverdue) Icons.Default.Warning else Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(11.dp),
                    tint = timelineColor
                )
                Text(
                    text = dlInfo.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (dlInfo.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { dlInfo.ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = timelineColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        // 4. Progress Tracker Completion Status
        val completion = getCompletionProgress(project.status)
        Column(
            modifier = Modifier.weight(2.2f).padding(end = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${(completion * 100).toInt()}% Done",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { completion },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        // 5. Assigned Editor with person icon
        Row(
            modifier = Modifier.weight(1.8f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Text(
                text = project.assignedEditor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 6. Quick Note actionable icon
        Box(
            modifier = Modifier.weight(1.2f),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onOpenNotes,
                modifier = Modifier.size(28.dp).testTag("notes_quick_row_${project.projectId}")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Quick Notes",
                    tint = if (project.notes.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Global visual helpers for computing progress metrics
fun getDeadlineProximity(startDateStr: String, deadlineDateStr: String): DeadlineInfo {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val now = Date()
    
    val start = try {
        if (startDateStr.isNotBlank()) formatter.parse(startDateStr) else null
    } catch (e: Exception) {
        null
    }
    
    val deadline = try {
        if (deadlineDateStr.isNotBlank()) formatter.parse(deadlineDateStr) else null
    } catch (e: Exception) {
        null
    }
    
    if (deadline == null) {
        return DeadlineInfo(ratio = 0f, daysRemaining = 0, isOverdue = false, label = "No Deadline")
    }
    
    val nowMs = now.time
    val deadlineMs = deadline.time
    val startMs = start?.time ?: (deadlineMs - 7 * 24 * 60 * 60 * 1000L) // Default 7 days duration if start date is missing
    
    val totalDurationMs = deadlineMs - startMs
    val elapsedMs = nowMs - startMs
    
    val ratio = if (totalDurationMs > 0) {
        (elapsedMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    
    val diffMs = deadlineMs - nowMs
    val daysRemaining = TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
    val isOverdue = diffMs < 0
    
    val label = when {
        isOverdue -> "Overdue by ${-daysRemaining}d"
        daysRemaining == 0 -> "Due today!"
        daysRemaining == 1 -> "1 day left"
        else -> "$daysRemaining days left"
    }
    
    return DeadlineInfo(ratio, daysRemaining, isOverdue, label)
}

data class DeadlineInfo(
    val ratio: Float, // 0.0f to 1.0f (representing percentage of timeline consumed)
    val daysRemaining: Int,
    val isOverdue: Boolean,
    val label: String
)

fun getCompletionProgress(status: String): Float {
    return when (status) {
        "New" -> 0.10f
        "Assigned" -> 0.25f
        "Editing" -> 0.50f
        "Preview Sent" -> 0.75f
        "Revision" -> 0.85f
        "Final Delivery" -> 0.95f
        "Completed" -> 1.00f
        else -> 0.00f // "On Hold"
    }
}

@Composable
fun ProjectNotesSheetContent(
    project: Project,
    onDismiss: () -> Unit,
    onSaveNotes: (String) -> Unit
) {
    var notesText by remember { mutableStateOf(project.notes) }
    
    // Quick tags for feedback / notes templates
    val templateTags = listOf(
        "Client loved the preview!",
        "Revision requested on audio mix",
        "Awaiting high-res assets",
        "Colors grading requested",
        "Call scheduled with PM",
        "Project paused"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        text = "Meeting Notes & Feedback",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${project.projectTitle} • ${project.clientName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Small Context pill row (status, assigned editor, priority)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.height(28.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = project.assignedEditor.ifBlank { "Unassigned" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.height(28.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val statusColor = when (project.status) {
                        "New", "Assigned", "Editing" -> StatusEditing
                        "Preview Sent", "Revision" -> StatusPreviewSent
                        "Final Delivery", "Completed" -> StatusCompleted
                        else -> StatusOnHold
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(statusColor, RoundedCornerShape(100.dp))
                    )
                    Text(
                        text = project.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Notes Notepad Text Field with nice Material 3 borders
        OutlinedTextField(
            value = notesText,
            onValueChange = { notesText = it },
            placeholder = { Text("Jot down quick client feedback, meeting agreements, or editor instructions here...", fontSize = 14.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .testTag("notes_input_field"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            ),
            singleLine = false
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Add Timestamp shortcut
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Meeting Templates",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            TextButton(
                onClick = {
                    val formattedStamp = String.format(
                        Locale.getDefault(), 
                        "\n\n[Meeting Note - %s]: ", 
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    )
                    notesText += formattedStamp
                },
                modifier = Modifier.height(30.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Insert Timestamp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Quick tags suggestion chip row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            templateTags.forEach { tagText ->
                SuggestionChip(
                    onClick = {
                        val formattedTag = if (notesText.isBlank()) tagText else if (notesText.endsWith("\n") || notesText.endsWith(": ")) tagText else "\n• $tagText"
                        notesText += formattedTag
                    },
                    label = { Text(tagText, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Saving action triggers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { onSaveNotes(notesText) },
                modifier = Modifier.weight(1f).height(48.dp).testTag("save_notes_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Notes", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
