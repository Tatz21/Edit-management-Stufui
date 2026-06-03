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
                                    val isSecondaryActive = currentScreenRoute in listOf("reports", "settings")
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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(scaffoldPadding)
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
