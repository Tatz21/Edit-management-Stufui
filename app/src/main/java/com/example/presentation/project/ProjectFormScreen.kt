package com.example.presentation.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Project
import com.example.presentation.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectFormScreen(
    viewModel: MainViewModel,
    projectId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val isEditMode = !projectId.isNullOrEmpty()
    val existingProject = remember(projectId, projects) {
        projects.find { it.projectId == projectId }
    }

    // Form inputs state
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var clientEmail by remember { mutableStateOf("") }
    
    var projectTitle by remember { mutableStateOf("") }
    var projectType by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    var receivedDate by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var deadlineDate by remember { mutableStateOf("") }
    var deliveryDate by remember { mutableStateOf("") }
    
    var assignedEditor by remember { mutableStateOf("Unassigned") }
    var status by remember { mutableStateOf("New") }
    var priority by remember { mutableStateOf("Low") }
    
    var totalAmount by remember { mutableStateOf("") }
    var advanceAmount by remember { mutableStateOf("") }
    
    var notes by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }

    // Init values
    LaunchedEffect(existingProject) {
        if (existingProject != null) {
            clientName = existingProject.clientName
            clientPhone = existingProject.clientPhone
            clientEmail = existingProject.clientEmail
            projectTitle = existingProject.projectTitle
            projectType = existingProject.projectType
            description = existingProject.description
            receivedDate = existingProject.receivedDate
            startDate = existingProject.startDate
            deadlineDate = existingProject.deadlineDate
            deliveryDate = existingProject.deliveryDate
            assignedEditor = existingProject.assignedEditor
            status = existingProject.status
            priority = existingProject.priority
            totalAmount = existingProject.totalAmount.toString()
            advanceAmount = existingProject.advanceAmount.toString()
            notes = existingProject.notes
            tags = existingProject.tags
        } else {
            // Prepopulate some default date strings with UTC today
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = formatter.format(Date())
            receivedDate = todayStr
            startDate = todayStr
            deadlineDate = todayStr
            deliveryDate = ""
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Project Details" else "Launch New Project", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("form_back_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Client section Card
            Text("Client Information", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = clientName,
                onValueChange = { clientName = it },
                label = { Text("Client Name *") },
                modifier = Modifier.fillMaxWidth().testTag("client_name_input"),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = clientPhone,
                onValueChange = { clientPhone = it },
                label = { Text("Client Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = clientEmail,
                onValueChange = { clientEmail = it },
                label = { Text("Client Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Project section
            Text("Project Information", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = projectTitle,
                onValueChange = { projectTitle = it },
                label = { Text("Project Title *") },
                modifier = Modifier.fillMaxWidth().testTag("project_title_input"),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = projectType,
                onValueChange = { projectType = it },
                label = { Text("Project Type (e.g. YouTube, Reel, Promo)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Project / Editing Brief Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            // Timeline section
            Text("Date & Timeline information (YYYY-MM-DD)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = receivedDate,
                    onValueChange = { receivedDate = it },
                    label = { Text("Received Date") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = deadlineDate,
                    onValueChange = { deadlineDate = it },
                    label = { Text("Deadline Date *") },
                    modifier = Modifier.weight(1f).testTag("deadline_date_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = deliveryDate,
                    onValueChange = { deliveryDate = it },
                    label = { Text("Delivery Date") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Operations & Workflow Dropdowns
            Text("SaaS Pipeline & Assignment", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            
            CustomDropdownSelector(
                label = "Assign Editor",
                selected = assignedEditor,
                options = listOf("Unassigned") + Project.EDITORS,
                onSelected = { assignedEditor = it }
            )

            CustomDropdownSelector(
                label = "Workflow Status",
                selected = status,
                options = Project.STATUS_OPTIONS,
                onSelected = { status = it }
            )

            CustomDropdownSelector(
                label = "Priority Options",
                selected = priority,
                options = Project.PRIORITY_OPTIONS,
                onSelected = { priority = it }
            )

            // Finances
            Text("Payment Management", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = totalAmount,
                    onValueChange = { totalAmount = it },
                    label = { Text("Total Project Amount *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("total_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = advanceAmount,
                    onValueChange = { advanceAmount = it },
                    label = { Text("Advance Received") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Text("Extra Details", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (comma separated e.g., 4K, Color Grading)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Shared Creative Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save Actions
            Button(
                onClick = {
                    if (clientName.isEmpty() || projectTitle.isEmpty() || deadlineDate.isEmpty() || totalAmount.toDoubleOrNull() == null) {
                        // Fail gracefully
                        return@Button
                    }
                    val tot = totalAmount.toDoubleOrNull() ?: 0.0
                    val adv = advanceAmount.toDoubleOrNull() ?: 0.0
                    
                    val proj = Project(
                        projectId = existingProject?.projectId ?: UUID.randomUUID().toString(),
                        clientName = clientName,
                        clientPhone = clientPhone,
                        clientEmail = clientEmail,
                        projectTitle = projectTitle,
                        projectType = projectType,
                        description = description,
                        receivedDate = receivedDate,
                        startDate = startDate,
                        deadlineDate = deadlineDate,
                        deliveryDate = deliveryDate,
                        assignedEditor = assignedEditor,
                        status = status,
                        priority = priority,
                        totalAmount = tot,
                        advanceAmount = adv,
                        remainingAmount = tot - adv,
                        paymentStatus = when {
                            adv >= tot -> "Fully Paid"
                            adv > 0 -> "Partially Paid"
                            else -> "Pending"
                        },
                        previewSentDate = existingProject?.previewSentDate ?: "",
                        previewVersion = existingProject?.previewVersion ?: "",
                        previewApproved = existingProject?.previewApproved ?: false,
                        notes = notes,
                        tags = tags,
                        createdAt = existingProject?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        paymentHistory = existingProject?.paymentHistory ?: "[]",
                        previewHistory = existingProject?.previewHistory ?: "[]",
                        revisionHistory = existingProject?.revisionHistory ?: "[]",
                        activityLogs = existingProject?.activityLogs ?: "[]"
                    )
                    
                    viewModel.addOrUpdateProject(proj)
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_project_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Project Node", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
