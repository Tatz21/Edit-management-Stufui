package com.example.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.*
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.ProjectRepository
import com.example.domain.usecase.ExportXlsxUseCase
import com.example.util.JsonUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    // 1. Theme Configuration State
    private val _isDarkTheme = MutableStateFlow(true) // Default to modern premium Dark
    val isDarkTheme = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    // 1b. Dynamic Editors list persisted via SharedPreferences
    private val _editorsState = MutableStateFlow<List<String>>(listOf("Mritunjay", "Rijhu", "Didi"))
    val editorsState = _editorsState.asStateFlow()

    fun loadEditors(context: Context) {
        val prefs = context.getSharedPreferences("editflow_editors_pref", Context.MODE_PRIVATE)
        val saved = prefs.getString("editors_list", null)
        if (saved == null) {
            val defaultList = listOf("Mritunjay", "Rijhu", "Didi")
            prefs.edit().putString("editors_list", defaultList.joinToString(",")).apply()
            _editorsState.value = defaultList
        } else {
            _editorsState.value = if (saved.isEmpty()) emptyList() else saved.split(",")
        }
    }

    fun addEditor(context: Context, name: String) {
        val current = _editorsState.value.toMutableList()
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && !current.contains(trimmed)) {
            current.add(trimmed)
            _editorsState.value = current
            val prefs = context.getSharedPreferences("editflow_editors_pref", Context.MODE_PRIVATE)
            prefs.edit().putString("editors_list", current.joinToString(",")).apply()
        }
    }

    fun removeEditor(context: Context, name: String) {
        val current = _editorsState.value.toMutableList()
        if (current.remove(name)) {
            _editorsState.value = current
            val prefs = context.getSharedPreferences("editflow_editors_pref", Context.MODE_PRIVATE)
            prefs.edit().putString("editors_list", current.joinToString(",")).apply()
        }
    }

    // 2. Auth State
    val currentUser: StateFlow<AuthUser?> = authRepository.getSignedInUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val syncStatus: StateFlow<String> = projectRepository.getFirebaseSyncFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Local Mode")

    // 3. Project Management State
    val projects: StateFlow<List<Project>> = projectRepository.getProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3b. Client Management State
    val clients: StateFlow<List<Client>> = projectRepository.getClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Searching, Filtering, Sorting and Date Range
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterEditor = MutableStateFlow("All")
    val filterEditor = _filterEditor.asStateFlow()

    private val _filterStatus = MutableStateFlow("All")
    val filterStatus = _filterStatus.asStateFlow()

    private val _filterPaymentStatus = MutableStateFlow("All")
    val filterPaymentStatus = _filterPaymentStatus.asStateFlow()

    private val _filterPriority = MutableStateFlow("All")
    val filterPriority = _filterPriority.asStateFlow()

    private val _sortType = MutableStateFlow("Latest") // Latest, Oldest, Deadline, Revenue
    val sortType = _sortType.asStateFlow()

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun updateFilterEditor(editor: String) { _filterEditor.value = editor }
    fun updateFilterStatus(status: String) { _filterStatus.value = status }
    fun updateFilterPaymentStatus(status: String) { _filterPaymentStatus.value = status }
    fun updateFilterPriority(priority: String) { _filterPriority.value = priority }
    fun updateSortType(type: String) { _sortType.value = type }

    val filteredProjects: StateFlow<List<Project>> = combine(
        projects, _searchQuery, _filterEditor, _filterStatus, _filterPaymentStatus, _filterPriority, _sortType
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val list = flows[0] as List<Project>
        val search = flows[1] as String
        val editor = flows[2] as String
        val status = flows[3] as String
        val paymentStr = flows[4] as String
        val priority = flows[5] as String
        val sort = flows[6] as String

        var result = list

        // Search filter
        if (search.isNotEmpty()) {
            result = result.filter { 
                it.projectTitle.contains(search, ignoreCase = true) || 
                it.clientName.contains(search, ignoreCase = true) 
            }
        }

        // Editor Filter
        if (editor != "All") {
            result = result.filter { it.assignedEditor.equals(editor, ignoreCase = true) }
        }

        // Status Filter
        if (status != "All") {
            result = result.filter { it.status.equals(status, ignoreCase = true) }
        }

        // Payment status filter
        if (paymentStr != "All") {
            result = result.filter { it.paymentStatus.equals(paymentStr, ignoreCase = true) }
        }

        // Priority Filter
        if (priority != "All") {
            result = result.filter { it.priority.equals(priority, ignoreCase = true) }
        }

        // Apply Sorting
        when (sort) {
            "Latest" -> result.sortedByDescending { it.createdAt }
            "Oldest" -> result.sortedBy { it.createdAt }
            "Deadline" -> result.sortedBy { it.deadlineDate }
            "Revenue" -> result.sortedByDescending { it.totalAmount }
            else -> result
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Auth Actions
    fun handleGoogleSignIn(idToken: String) {
        viewModelScope.launch {
            authRepository.signInWithGoogleToken(idToken)
        }
    }

    fun handleBypassSignIn() {
        viewModelScope.launch {
            authRepository.debugSignIn()
        }
    }

    fun handleSignOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    // 6. DB Core Operations (CRUD)
    fun addOrUpdateProject(project: Project) {
        viewModelScope.launch {
            // Log action to project's activity history
            val existing = projectRepository.getProjects().first().find { it.projectId == project.projectId }
            val author = currentUser.value?.name ?: "Admin"
            
            val initialLogs = JsonUtil.fromActivityHistoryJson(existing?.activityLogs)
            val updatedLogs = initialLogs + ActivityLogEntry(
                timestamp = System.currentTimeMillis(),
                message = if (existing == null) "Project Created" else "Project details updated",
                author = author
            )
            
            val updatedProject = project.copy(
                remainingAmount = project.totalAmount - project.advanceAmount,
                paymentStatus = when {
                    project.advanceAmount >= project.totalAmount -> "Fully Paid"
                    project.advanceAmount > 0 -> "Partially Paid"
                    else -> "Pending"
                },
                activityLogs = JsonUtil.toActivityHistoryJson(updatedLogs)
            )
            
            projectRepository.saveProject(updatedProject)
            triggerSystemNotifications(updatedProject, if (existing == null) "created" else "modified")
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            projectRepository.deleteProject(projectId)
        }
    }

    // Client Directory Operations
    fun addOrUpdateClient(client: Client) {
        viewModelScope.launch {
            projectRepository.saveClient(client)
        }
    }

    fun deleteClient(clientId: String) {
        viewModelScope.launch {
            projectRepository.deleteClient(clientId)
        }
    }

    // Status transition e.g., Drag & Drop
    fun updateProjectStatus(projectId: String, newStatus: String) {
        viewModelScope.launch {
            val list = projects.value
            val p = list.find { it.projectId == projectId } ?: return@launch
            
            val author = currentUser.value?.name ?: "Admin"
            val initialLogs = JsonUtil.fromActivityHistoryJson(p.activityLogs)
            val updatedLogs = initialLogs + ActivityLogEntry(
                timestamp = System.currentTimeMillis(),
                message = "Status transitioned from ${p.status} to $newStatus",
                author = author
            )

            // If updated to final delivery because of preview approval
            val finalDeliveryDateStr = if (newStatus == "Final Delivery" || newStatus == "Completed") {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            } else {
                p.deliveryDate
            }

            val updated = p.copy(
                status = newStatus,
                deliveryDate = finalDeliveryDateStr,
                activityLogs = JsonUtil.toActivityHistoryJson(updatedLogs)
            )
            projectRepository.saveProject(updated)
            
            if (newStatus == "Completed") {
                triggerSystemNotifications(updated, "completed")
            }
        }
    }

    // Payment History Insertion
    fun addPaymentHistory(projectId: String, amount: Double, notes: String) {
        viewModelScope.launch {
            val list = projects.value
            val p = list.find { it.projectId == projectId } ?: return@launch
            
            val payments = JsonUtil.fromPaymentHistoryJson(p.paymentHistory).toMutableList()
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            payments.add(PaymentHistoryEntry(amount, dateStr, notes))
            
            val newAdvance = p.advanceAmount + amount
            val newRem = p.totalAmount - newAdvance
            val newPaymentStatus = when {
                newAdvance >= p.totalAmount -> "Fully Paid"
                newAdvance > 0 -> "Partially Paid"
                else -> "Pending"
            }
            
            val author = currentUser.value?.name ?: "Admin"
            val logs = JsonUtil.fromActivityHistoryJson(p.activityLogs) + ActivityLogEntry(
                timestamp = System.currentTimeMillis(),
                message = "Added Payment history: ₹$amount ($notes)",
                author = author
            )

            val updated = p.copy(
                advanceAmount = newAdvance,
                remainingAmount = newRem,
                paymentStatus = newPaymentStatus,
                paymentHistory = JsonUtil.toPaymentHistoryJson(payments),
                activityLogs = JsonUtil.toActivityHistoryJson(logs)
            )
            projectRepository.saveProject(updated)
        }
    }

    // Preview Tracking Approval
    fun addPreviewHistory(projectId: String, version: String, isApproved: Boolean, notes: String) {
        viewModelScope.launch {
            val list = projects.value
            val p = list.find { it.projectId == projectId } ?: return@launch
            
            val previews = JsonUtil.fromPreviewHistoryJson(p.previewHistory).toMutableList()
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            previews.add(PreviewHistoryEntry(version, dateStr, if (isApproved) "Approved" else "Not Approved", notes))
            
            // Auto transition state if approved
            val nextStatus = if (isApproved) "Final Delivery" else "Revision"
            val deliveryDateStr = if (isApproved) dateStr else p.deliveryDate

            val author = currentUser.value?.name ?: "Admin"
            val logs = JsonUtil.fromActivityHistoryJson(p.activityLogs) + ActivityLogEntry(
                timestamp = System.currentTimeMillis(),
                message = "Preview $version registered as ${if (isApproved) "Approved" else "Not Approved"}",
                author = author
            )

            val updated = p.copy(
                previewSentDate = dateStr,
                previewVersion = version,
                previewApproved = isApproved,
                status = nextStatus,
                deliveryDate = deliveryDateStr,
                previewHistory = JsonUtil.toPreviewHistoryJson(previews),
                activityLogs = JsonUtil.toActivityHistoryJson(logs)
            )
            projectRepository.saveProject(updated)
        }
    }

    // Revision Tracking Addition
    fun addRevisionHistory(projectId: String, version: String, details: String, editorNotes: String) {
        viewModelScope.launch {
            val list = projects.value
            val p = list.find { it.projectId == projectId } ?: return@launch
            
            val revisions = JsonUtil.fromRevisionHistoryJson(p.revisionHistory).toMutableList()
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            revisions.add(RevisionHistoryEntry(version, dateStr, details, editorNotes))

            val author = currentUser.value?.name ?: "Admin"
            val logs = JsonUtil.fromActivityHistoryJson(p.activityLogs) + ActivityLogEntry(
                timestamp = System.currentTimeMillis(),
                message = "Revision Requested for Version: $version",
                author = author
            )

            val updated = p.copy(
                status = "Revision",
                revisionHistory = JsonUtil.toRevisionHistoryJson(revisions),
                activityLogs = JsonUtil.toActivityHistoryJson(logs)
            )
            projectRepository.saveProject(updated)
        }
    }

    // General note adding
    fun updateNotes(projectId: String, newNotes: String) {
        viewModelScope.launch {
            val list = projects.value
            val p = list.find { it.projectId == projectId } ?: return@launch
            val updated = p.copy(notes = newNotes)
            projectRepository.saveProject(updated)
        }
    }

    // Manual cloud database sync pull
    fun triggerManualSync() {
        viewModelScope.launch {
            projectRepository.syncOfflineChanges()
        }
    }

    // Excel POI report sharing
    fun shareExcelReport(context: Context, reportFilename: String, filterType: String) {
        val useCase = ExportXlsxUseCase()
        val projectsToExport = when (filterType) {
            "All" -> projects.value
            "Completed" -> projects.value.filter { it.status == "Completed" }
            "Overdue" -> projects.value.filter { isProjectOverdue(it) }
            "Payments Pending" -> projects.value.filter { it.paymentStatus != "Fully Paid" }
            else -> projects.value
        }
        
        val result = useCase.execute(context, reportFilename, projectsToExport)
        result.onSuccess { file ->
            shareFile(context, file)
        }.onFailure {
            Log.e("MainViewModel", "Fail to export spreadsheet: ${it.message}")
        }
    }

    private fun shareFile(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "com.aistudio.editflowpro.tqzxlw.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "EditFlow Pro - Spreadsheet Report")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(intent, "Share Spreadsheet File via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error sharing file: ${e.message}")
        }
    }

    // Safe parsing helper
    private fun isProjectOverdue(project: Project): Boolean {
        if (project.status == "Completed" || project.status == "On Hold" || project.status == "Final Delivery") return false
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val deadlineDate = formatter.parse(project.deadlineDate)
            deadlineDate ?: return false
            deadlineDate.before(Date())
        } catch (e: Exception) {
            false
        }
    }

    // 7. Dynamic Alert notifications mechanism
    private val _notifications = MutableStateFlow<List<String>>(emptyList())
    val notifications = _notifications.asStateFlow()

    fun clearNotifications() { _notifications.value = emptyList() }

    private fun triggerSystemNotifications(project: Project, action: String) {
        val list = _notifications.value.toMutableList()
        val formattedMsg = "Project '${project.projectTitle}' was $action by ${currentUser.value?.name ?: "Admin"}."
        list.add(0, formattedMsg)
        _notifications.value = list
    }

    fun triggerAutomatedNotificationChecks() {
        viewModelScope.launch {
            val list = projects.value
            val alerts = mutableListOf<String>()
            val today = Date()
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            for (p in list) {
                if (p.status == "Completed") continue
                
                try {
                    val deadline = format.parse(p.deadlineDate) ?: continue
                    val diff = deadline.time - today.time
                    val diffDays = diff / (1000 * 60 * 60 * 24)
                    
                    if (diffDays in 0..2) {
                        alerts.add("⚠️ Deadline approaching within ${diffDays + 1} day(s) for '${p.projectTitle}'!")
                    } else if (diffDays < 0) {
                        alerts.add("🚨 PROJECT OVERDUE! '${p.projectTitle}' by Editor ${p.assignedEditor} is overdue since ${p.deadlineDate}.")
                    }
                } catch (e: Exception) { /* Format parsing error ignored */ }

                if (p.paymentStatus != "Fully Paid") {
                    alerts.add("💸 Remaining collection of ₹${p.remainingAmount} pending for '${p.projectTitle}' (${p.clientName}).")
                }
            }
            
            _notifications.value = alerts
        }
    }
}

// Custom view model factory
class MainViewModelFactory(
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(authRepository, projectRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
