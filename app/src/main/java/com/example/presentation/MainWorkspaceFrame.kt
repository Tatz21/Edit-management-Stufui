package com.example.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.presentation.auth.LoginScreen
import com.example.presentation.dashboard.DashboardScreen
import com.example.presentation.editor.EditorScreen
import com.example.presentation.project.KanbanBoardScreen
import com.example.presentation.project.ProjectDetailsScreen
import com.example.presentation.project.ProjectFormScreen
import com.example.presentation.project.ProjectListScreen
import com.example.presentation.reports.ReportsScreen
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.clients.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import com.example.domain.model.Project
import com.example.domain.model.Client


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainWorkspaceFrame(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()

    // Screen Router keys
    var currentScreenRoute by remember { mutableStateOf("dashboard") }
    var nestedProjectIdParam by remember { mutableStateOf<String?>(null) } // Target project ID for Details or Form Edits

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            currentScreenRoute = "dashboard"
            nestedProjectIdParam = null
        }
    }

    if (currentUser == null) {
        // Fallback or Sandbox Login
        LoginScreen(viewModel = viewModel)
    } else {
        // Safe authenticated workspace
        var showMoreBottomSheet by remember { mutableStateOf(false) }

        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val isTablet = maxWidth > 600.dp
            
            // Sidebar Navigation Item definition
            val navItems = listOf(
                NavItem("dashboard", "Dashboard", Icons.Default.Dashboard, "Workspace performance KPI metrics"),
                NavItem("projects_list", "Projects", Icons.Default.Folder, "Pipeline detail lists, edits, and tracker"),
                NavItem("kanban", "Kanban", Icons.Default.ViewKanban, "Drag-and-drop workflow status boards"),
                NavItem("editors", "Editors", Icons.Default.People, "Manage editing team and workload balance"),
                NavItem("clients", "Clients", Icons.Default.ContactMail, "Manage client registry profiles and payments"),
                NavItem("payments", "Payments", Icons.Default.Payments, "Track billing, invoices, and earnings records"),
                NavItem("reports", "Reports", Icons.Default.Assessment, "Analyze monthly revenues and outputs"),
                NavItem("settings", "Settings", Icons.Default.Settings, "Theme preferences, cloud sync, and backup tools")
            )

            Row(modifier = Modifier.fillMaxSize()) {
                // If Tablet view, display Sidebar Navigation Rail (Canonical Layout requirement!)
                if (isTablet) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight(),
                        header = {
                            Icon(
                                imageVector = Icons.Default.VideoCameraBack,
                                contentDescription = "EditFlow Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(16.dp).size(28.dp)
                            )
                        }
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        navItems.forEach { item ->
                            NavigationRailItem(
                                selected = currentScreenRoute == item.route || 
                                          (item.route == "projects_list" && (currentScreenRoute == "projects_details" || currentScreenRoute == "projects_form")),
                                onClick = {
                                    currentScreenRoute = item.route
                                    nestedProjectIdParam = null
                                },
                                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                                label = { Text(item.label, fontSize = 10.sp) },
                                modifier = Modifier.padding(vertical = 4.dp).testTag("rail_item_${item.route}")
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // Screen body workspace content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Scaffold(
                        bottomBar = {
                            // If Phone view, display Bottom Navigation Bar (M3 compliance!)
                            if (!isTablet) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    // 1. First 4 items
                                    val primaryItems = navItems.take(4)
                                    primaryItems.forEach { item ->
                                        val isSelected = currentScreenRoute == item.route || 
                                                       (item.route == "projects_list" && (currentScreenRoute == "projects_details" || currentScreenRoute == "projects_form"))
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = {
                                                currentScreenRoute = item.route
                                                nestedProjectIdParam = null
                                            },
                                            icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                                            label = { Text(item.label, fontSize = 9.sp) },
                                            modifier = Modifier.testTag("nav_item_${item.route}")
                                        )
                                    }

                                    // 2. Fifth item is the sliding "More" action
                                    val isSecondaryActive = currentScreenRoute in listOf("reports", "settings", "payments")
                                    NavigationBarItem(
                                        selected = isSecondaryActive,
                                        onClick = { showMoreBottomSheet = true },
                                        icon = { Icon(imageVector = Icons.Default.Menu, contentDescription = "More options") },
                                        label = { Text("More", fontSize = 9.sp) },
                                        modifier = Modifier.testTag("nav_item_more")
                                    )
                                }
                            }
                        }
                    ) { scaffoldPadding ->
                        val allProjects by viewModel.projects.collectAsState()
                        val allClients by viewModel.clients.collectAsState()

                        var globalSearchQuery by remember { mutableStateOf("") }
                        var isSearchFocused by remember { mutableStateOf(false) }

                        val matchingProjects = remember(globalSearchQuery, allProjects) {
                            if (globalSearchQuery.isBlank()) emptyList() else {
                                allProjects.filter {
                                    it.projectTitle.contains(globalSearchQuery, ignoreCase = true) ||
                                    it.clientName.contains(globalSearchQuery, ignoreCase = true) ||
                                    it.assignedEditor.contains(globalSearchQuery, ignoreCase = true)
                                }
                            }
                        }

                        val matchingClients = remember(globalSearchQuery, allClients) {
                            if (globalSearchQuery.isBlank()) emptyList() else {
                                allClients.filter {
                                    it.name.contains(globalSearchQuery, ignoreCase = true) ||
                                    it.email.contains(globalSearchQuery, ignoreCase = true)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(scaffoldPadding)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Global search input field in the top navigation
                                GlobalSearchTopNavigation(
                                    query = globalSearchQuery,
                                    onQueryChange = { globalSearchQuery = it },
                                    isFocused = isSearchFocused,
                                    onFocusChange = { isSearchFocused = it }
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    when (currentScreenRoute) {
                                "dashboard" -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToProjects = { currentScreenRoute = "projects_list" },
                                    onNavigateToSettings = { currentScreenRoute = "settings" },
                                    onNavigateToProjectDetails = { id ->
                                        nestedProjectIdParam = id
                                        currentScreenRoute = "projects_details"
                                    }
                                )
                                "projects_list" -> ProjectListScreen(
                                    viewModel = viewModel,
                                    onNavigateToProjectDetails = { id ->
                                        nestedProjectIdParam = id
                                        currentScreenRoute = "projects_details"
                                    },
                                    onNavigateToAddProject = {
                                        nestedProjectIdParam = null
                                        currentScreenRoute = "projects_form"
                                    },
                                    onNavigateToSettings = { currentScreenRoute = "settings" }
                                )
                                "projects_details" -> ProjectDetailsScreen(
                                    viewModel = viewModel,
                                    projectId = nestedProjectIdParam ?: "",
                                    onNavigateBack = {
                                        currentScreenRoute = "projects_list"
                                        nestedProjectIdParam = null
                                    },
                                    onNavigateToEditProject = { id ->
                                        nestedProjectIdParam = id
                                        currentScreenRoute = "projects_form"
                                    }
                                )
                                "projects_form" -> ProjectFormScreen(
                                    viewModel = viewModel,
                                    projectId = nestedProjectIdParam,
                                    onNavigateBack = {
                                        if (nestedProjectIdParam != null) {
                                            currentScreenRoute = "projects_details"
                                        } else {
                                            currentScreenRoute = "projects_list"
                                        }
                                    }
                                )
                                "kanban" -> KanbanBoardScreen(
                                    viewModel = viewModel,
                                    onNavigateToProjectDetails = { id ->
                                        nestedProjectIdParam = id
                                        currentScreenRoute = "projects_details"
                                    }
                                )
                                "clients" -> ClientDirectoryScreen(
                                    viewModel = viewModel,
                                    onNavigateToProjectDetails = { id ->
                                        nestedProjectIdParam = id
                                        currentScreenRoute = "projects_details"
                                    }
                                )
                                "payments" -> PaymentManagementScreen(
                                    viewModel = viewModel
                                )
                                "editors" -> EditorScreen(
                                    viewModel = viewModel
                                )
                                "reports" -> ReportsScreen(
                                    viewModel = viewModel
                                )
                                "settings" -> SettingsScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }

                            // Beautiful floating quick lookup results overlay on top of screen content
                            if (isSearchFocused && globalSearchQuery.isNotEmpty()) {
                                GlobalSearchResultsOverlay(
                                    matchingProjects = matchingProjects,
                                    matchingClients = matchingClients,
                                    onProjectClicked = { projectId ->
                                        nestedProjectIdParam = projectId
                                        currentScreenRoute = "projects_details"
                                        globalSearchQuery = ""
                                        isSearchFocused = false
                                    },
                                    onClientClicked = { client ->
                                        currentScreenRoute = "clients"
                                        globalSearchQuery = ""
                                        isSearchFocused = false
                                    },
                                    onClose = {
                                        globalSearchQuery = ""
                                        isSearchFocused = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Slide Up Modal Bottom Sheet presenting secondary navigation options
            if (showMoreBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showMoreBottomSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "More Workspace Tools",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        val secondaryItems = navItems.drop(4)
                        
                        secondaryItems.forEach { item ->
                            val isItemActive = currentScreenRoute == item.route
                            Surface(
                                onClick = {
                                    currentScreenRoute = item.route
                                    nestedProjectIdParam = null
                                    showMoreBottomSheet = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isItemActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().testTag("sheet_item_${item.route}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint = if (isItemActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.label,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isItemActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = item.desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isItemActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = if (isItemActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// Navigation structure DTO
private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val desc: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchTopNavigation(
    query: String,
    onQueryChange: (String) -> Unit,
    isFocused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("global_search_container"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "Search projects or client names...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("global_search_input_field")
                    .onFocusChanged { focusState ->
                        onFocusChange(focusState.isFocused)
                    },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.testTag("global_search_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search query",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun GlobalSearchResultsOverlay(
    matchingProjects: List<Project>,
    matchingClients: List<Client>,
    onProjectClicked: (String) -> Unit,
    onClientClicked: (Client) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onClose() }
            .testTag("global_search_overlay"),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable(enabled = false) { }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .testTag("global_search_results_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Search Results Lookup",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = onClose) {
                            Text("Dismiss", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (matchingProjects.isEmpty() && matchingClients.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No projects or clients match your search query",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        if (matchingProjects.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Projects (${matchingProjects.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )

                                matchingProjects.forEach { proj ->
                                    Surface(
                                        onClick = { onProjectClicked(proj.projectId) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = proj.projectTitle,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Client: ${proj.clientName} • Assigned: ${proj.assignedEditor}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                )
                                            }
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(100.dp)
                                            ) {
                                                Text(
                                                    text = proj.status,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (matchingClients.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Clients (${matchingClients.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )

                                matchingClients.forEach { client ->
                                    Surface(
                                        onClick = { onClientClicked(client) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContactMail,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = client.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Email: ${client.email} • Ph: ${client.phone}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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
        }
    }
}

