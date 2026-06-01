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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.MoreVert
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
fun KanbanBoardScreen(
    viewModel: MainViewModel,
    onNavigateToProjectDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val columns = Project.STATUS_OPTIONS
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SaaS Kanban Board", fontWeight = FontWeight.Bold)
                        Text("Scroll horizontally | Click options to shift nodes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .horizontalScroll(scrollState)
                .padding(bottom = 16.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            
            columns.forEach { colName ->
                val colProjects = remember(projects, colName) {
                    projects.filter { it.status.equals(colName, ignoreCase = true) }
                }
                
                KanbanColumn(
                    columnName = colName,
                    projects = colProjects,
                    onMoveProject = { pId, nextSt -> viewModel.updateProjectStatus(pId, nextSt) },
                    onClickProject = onNavigateToProjectDetails
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
fun KanbanColumn(
    columnName: String,
    projects: List<Project>,
    onMoveProject: (String, String) -> Unit,
    onClickProject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colColor = when (columnName) {
        "New" -> StatusNew
        "Assigned" -> StatusAssigned
        "Editing" -> StatusEditing
        "Preview Sent" -> StatusPreviewSent
        "Revision" -> StatusRevision
        "Final Delivery" -> StatusFinalDelivery
        "Completed" -> StatusCompleted
        else -> StatusOnHold
    }

    Box(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // Column Headers Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colColor, RoundedCornerShape(5.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = columnName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Badge(containerColor = colColor.copy(alpha = 0.2f), contentColor = colColor) {
                    Text(projects.size.toString(), fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable cards list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(projects, key = { it.projectId }) { proj ->
                    KanbanProjectCard(
                        project = proj,
                        onMoveStatus = { nextSt -> onMoveProject(proj.projectId, nextSt) },
                        onClick = { onClickProject(proj.projectId) }
                    )
                }
            }
        }
    }
}

@Composable
fun KanbanProjectCard(
    project: Project,
    onMoveStatus: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedMenu by remember { mutableStateOf(false) }

    val priorityColor = when (project.priority) {
        "Low" -> PriorityLow
        "Medium" -> PriorityMedium
        "High" -> PriorityHigh
        else -> PriorityUrgent
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Priority label
                Box(
                    modifier = Modifier
                        .background(priorityColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(text = project.priority, color = priorityColor, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                }

                // Options trigger menu to shift column status
                Box {
                    IconButton(
                        onClick = { expandedMenu = !expandedMenu },
                        modifier = Modifier.size(24.dp).testTag("kanban_options_${project.projectId}")
                    ) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    
                    DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
                        Text(text = "  Move Node Status To:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        DropdownMenuDivider()
                        Project.STATUS_OPTIONS.filter { it != project.status }.forEach { op ->
                            DropdownMenuItem(
                                text = { Text(op, fontSize = 13.sp) },
                                onClick = {
                                    onMoveStatus(op)
                                    expandedMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = project.projectTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onClick() }
            )
            
            Text(
                text = project.clientName,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Deadline label
                Text(
                    text = "⌛ ${project.deadlineDate}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                // Editor label
                Text(
                    text = "🎬 ${project.assignedEditor}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DropdownMenuDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}
