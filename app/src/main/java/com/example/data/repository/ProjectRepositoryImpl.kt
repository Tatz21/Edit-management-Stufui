package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.ProjectDao
import com.example.domain.model.Project
import com.example.domain.repository.ProjectRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProjectRepositoryImpl(
    private val projectDao: ProjectDao,
    private val context: Context
) : ProjectRepository {

    private val _syncStatusFlow = MutableStateFlow("Local-Only Mode")
    override fun getFirebaseSyncFlow(): Flow<String> = _syncStatusFlow.asStateFlow()

    private var firestore: FirebaseFirestore? = null
    private var firestoreListener: ListenerRegistration? = null
    private val repoScope = CoroutineScope(Dispatchers.IO)

    init {
        setupFirestore()
    }

    private fun setupFirestore() {
        repoScope.launch {
            try {
                // Confirm Firebase is initialized
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    firestore = FirebaseFirestore.getInstance()
                    _syncStatusFlow.value = "Synced with Cloud"
                    observeFirestoreChanges()
                    syncOfflineChanges()
                } else {
                    _syncStatusFlow.value = "Firebase Unconfigured (Local-Only)"
                }
            } catch (e: Exception) {
                Log.e("ProjectRepositoryImpl", "Firestore not available: ${e.message}")
                _syncStatusFlow.value = "Firebase Offline (Local-Only)"
            }
        }
    }

    private fun observeFirestoreChanges() {
        val fs = firestore ?: return
        try {
            firestoreListener?.remove()
            firestoreListener = fs.collection("projects")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("ProjectRepositoryImpl", "Firestore listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        repoScope.launch {
                            val remoteProjects = snapshots.documents.mapNotNull { doc ->
                                try {
                                    // Parse document manually or via custom mapper to handle Firestore double/long issues
                                    val data = doc.data ?: return@mapNotNull null
                                    
                                    val totalAmt = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0
                                    val advAmt = (data["advanceAmount"] as? Number)?.toDouble() ?: 0.0
                                    val remAmt = (data["remainingAmount"] as? Number)?.toDouble() ?: 0.0
                                    val previewOk = data["previewApproved"] as? Boolean ?: false
                                    
                                    Project(
                                        projectId = doc.id,
                                        clientName = data["clientName"] as? String ?: "",
                                        clientPhone = data["clientPhone"] as? String ?: "",
                                        clientEmail = data["clientEmail"] as? String ?: "",
                                        projectTitle = data["projectTitle"] as? String ?: "",
                                        projectType = data["projectType"] as? String ?: "",
                                        description = data["description"] as? String ?: "",
                                        receivedDate = data["receivedDate"] as? String ?: "",
                                        startDate = data["startDate"] as? String ?: "",
                                        deadlineDate = data["deadlineDate"] as? String ?: "",
                                        deliveryDate = data["deliveryDate"] as? String ?: "",
                                        assignedEditor = data["assignedEditor"] as? String ?: "Unassigned",
                                        status = data["status"] as? String ?: "New",
                                        priority = data["priority"] as? String ?: "Low",
                                        totalAmount = totalAmt,
                                        advanceAmount = advAmt,
                                        remainingAmount = remAmt,
                                        paymentStatus = data["paymentStatus"] as? String ?: "Pending",
                                        previewSentDate = data["previewSentDate"] as? String ?: "",
                                        previewVersion = data["previewVersion"] as? String ?: "",
                                        previewApproved = previewOk,
                                        notes = data["notes"] as? String ?: "",
                                        tags = data["tags"] as? String ?: "",
                                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                        paymentHistory = data["paymentHistory"] as? String ?: "[]",
                                        previewHistory = data["previewHistory"] as? String ?: "[]",
                                        revisionHistory = data["revisionHistory"] as? String ?: "[]",
                                        activityLogs = data["activityLogs"] as? String ?: "[]",
                                        isSynced = true,
                                        isDeletedOffline = false
                                    )
                                } catch (ex: Exception) {
                                    Log.e("ParseFS", "Parse project failed: ${ex.message}")
                                    null
                                }
                            }
                            
                            // Merge Firestore into Room: Only overwrite if remote is newer or local is already synced
                            for (remoteProj in remoteProjects) {
                                val localProj = projectDao.getProjectById(remoteProj.projectId)
                                if (localProj == null || localProj.isSynced || remoteProj.updatedAt > localProj.updatedAt) {
                                    projectDao.insertProject(remoteProj)
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("ProjectRepositoryImpl", "Could not start snapshot listener: ${e.message}")
        }
    }

    override fun getProjects(): Flow<List<Project>> {
        return projectDao.getAllProjectsFlow()
    }

    override fun getProjectById(projectId: String): Flow<Project?> {
        return projectDao.getProjectByIdFlow(projectId)
    }

    override suspend fun saveProject(project: Project, updateFirebase: Boolean) {
        // Compute remaining always
        val calculatedProject = project.copy(
            remainingAmount = project.totalAmount - project.advanceAmount,
            updatedAt = System.currentTimeMillis()
        )
        
        // Save locally first
        projectDao.insertProject(calculatedProject)
        
        // Save to Firebase asynchronously if requested and available
        if (updateFirebase) {
            uploadToFirestore(calculatedProject)
        }
    }

    private suspend fun uploadToFirestore(project: Project) {
        val fs = firestore
        if (fs == null) {
            _syncStatusFlow.value = "Local Saving (No Sync)"
            return
        }
        
        try {
            _syncStatusFlow.value = "Updating Cloud..."
            
            // Map the custom data format perfectly
            val map = hashMapOf(
                "projectId" to project.projectId,
                "clientName" to project.clientName,
                "clientPhone" to project.clientPhone,
                "clientEmail" to project.clientEmail,
                "projectTitle" to project.projectTitle,
                "projectType" to project.projectType,
                "description" to project.description,
                "receivedDate" to project.receivedDate,
                "startDate" to project.startDate,
                "deadlineDate" to project.deadlineDate,
                "deliveryDate" to project.deliveryDate,
                "assignedEditor" to project.assignedEditor,
                "status" to project.status,
                "priority" to project.priority,
                "totalAmount" to project.totalAmount,
                "advanceAmount" to project.advanceAmount,
                "remainingAmount" to project.remainingAmount,
                "paymentStatus" to project.paymentStatus,
                "previewSentDate" to project.previewSentDate,
                "previewVersion" to project.previewVersion,
                "previewApproved" to project.previewApproved,
                "notes" to project.notes,
                "tags" to project.tags,
                "createdAt" to project.createdAt,
                "updatedAt" to project.updatedAt,
                "paymentHistory" to project.paymentHistory,
                "previewHistory" to project.previewHistory,
                "revisionHistory" to project.revisionHistory,
                "activityLogs" to project.activityLogs
            )
            
            fs.collection("projects").document(project.projectId).set(map, SetOptions.merge()).await()
            projectDao.insertProject(project.copy(isSynced = true))
            _syncStatusFlow.value = "Synced with Cloud"
        } catch (e: Exception) {
            Log.e("ProjectRepositoryImpl", "Could not upload to firestore: ${e.message}")
            projectDao.insertProject(project.copy(isSynced = false))
            _syncStatusFlow.value = "Cached Offline"
        }
    }

    override suspend fun deleteProject(projectId: String, updateFirebase: Boolean) {
        // Mark locally deleted
        projectDao.markAsDeletedOffline(projectId)
        
        val fs = firestore
        if (fs == null) {
            _syncStatusFlow.value = "Cached Sync Delete"
            return
        }

        try {
            _syncStatusFlow.value = "Cloud Deleting..."
            fs.collection("projects").document(projectId).delete().await()
            // Hard delete locally since cloud confirmed deletion
            projectDao.hardDeleteProject(projectId)
            _syncStatusFlow.value = "Synced with Cloud"
        } catch (e: Exception) {
            Log.e("ProjectRepositoryImpl", "Could not delete from firestore: ${e.message}")
            _syncStatusFlow.value = "Pending Cloud Delete"
        }
    }

    override suspend fun syncOfflineChanges() {
        val fs = firestore ?: return
        try {
            val unsynced = projectDao.getUnsyncedProjects()
            if (unsynced.isEmpty()) return

            _syncStatusFlow.value = "Syncing local changes..."
            for (p in unsynced) {
                if (p.isDeletedOffline) {
                    try {
                        fs.collection("projects").document(p.projectId).delete().await()
                        projectDao.hardDeleteProject(p.projectId)
                    } catch (e: Exception) {
                        Log.e("Sync", "Delete pending sync failed: ${e.message}")
                    }
                } else {
                    uploadToFirestore(p)
                }
            }
            _syncStatusFlow.value = "Synced with Cloud"
        } catch (e: Exception) {
            Log.e("Sync", "Sync offline changes failed: ${e.message}")
            _syncStatusFlow.value = "Sync Interrupted"
        }
    }
}
