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

    private val _syncStatusFlow = MutableStateFlow("Local Sandbox Mode")
    override fun getFirebaseSyncFlow(): Flow<String> = _syncStatusFlow.asStateFlow()

    private val repoScope = CoroutineScope(Dispatchers.IO)

    init {
        _syncStatusFlow.value = "Local Sandbox Mode"
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
    }



    override suspend fun deleteProject(projectId: String, updateFirebase: Boolean) {
        // Hard delete immediately in Local Mode
        projectDao.hardDeleteProject(projectId)
    }

    override suspend fun syncOfflineChanges() {
        // No-op in Local Mode
    }
}
