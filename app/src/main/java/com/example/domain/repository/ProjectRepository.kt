package com.example.domain.repository

import com.example.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getProjects(): Flow<List<Project>>
    
    fun getProjectById(projectId: String): Flow<Project?>
    
    suspend fun saveProject(project: Project, updateFirebase: Boolean = true)
    
    suspend fun deleteProject(projectId: String, updateFirebase: Boolean = true)
    
    suspend fun syncOfflineChanges()
    
    fun getFirebaseSyncFlow(): Flow<String> // Returns status string
}
