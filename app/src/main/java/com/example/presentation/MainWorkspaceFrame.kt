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
import com.example.presentation.auth.LoginScreen
import com.example.presentation.dashboard.DashboardScreen
import com.example.presentation.editor.EditorScreen
import com.example.presentation.project.KanbanBoardScreen
import com.example.presentation.project.ProjectDetailsScreen
import com.example.presentation.project.ProjectFormScreen
import com.example.presentation.project.ProjectListScreen
import com.example.presentation.reports.ReportsScreen
import com.example.presentation.settings.SettingsScreen

@Composable
fun MainWorkspaceFrame(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()

    // Screen Router keys
    var currentScreenRoute by remember { mutableStateOf("dashboard") }
    var nestedProjectIdParam by remember { mutableStateOf<String?>(null) } // Target project ID for Details or Form Edits

    if (currentUser == null) {
        // Fallback or Sandbox Login
        LoginScreen(viewModel = viewModel)
    } else {
        // Safe authenticated workspace
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val isTablet = maxWidth > 600.dp
            
            // Sidebar Navigation Item definition
            val navItems = listOf(
                NavItem("dashboard", "Dashboard", Icons.Default.Dashboard),
                NavItem("projects_list", "Projects", Icons.Default.Folder),
                NavItem("kanban", "Kanban", Icons.Default.ViewKanban),
                NavItem("editors", "Editors", Icons.Default.People),
                NavItem("reports", "Reports", Icons.Default.Assessment),
                NavItem("settings", "Settings", Icons.Default.Settings)
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
                                    navItems.forEach { item ->
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
                                    onNavigateToProjects = { currentScreenRoute = "projects_list" }
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
                                    }
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
        }
    }
}

// Navigation structure DTO
private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
