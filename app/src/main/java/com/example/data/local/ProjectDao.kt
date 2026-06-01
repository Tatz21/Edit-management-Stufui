package com.example.data.local

import androidx.room.*
import com.example.domain.model.Project
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE isDeletedOffline = 0 ORDER BY createdAt DESC")
    fun getAllProjectsFlow(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE isDeletedOffline = 0 ORDER BY createdAt DESC")
    suspend fun getAllProjectsSync(): List<Project>

    @Query("SELECT * FROM projects WHERE projectId = :id")
    suspend fun getProjectById(id: String): Project?

    @Query("SELECT * FROM projects WHERE projectId = :id")
    fun getProjectByIdFlow(id: String): Flow<Project?>

    @Query("SELECT * FROM projects WHERE isSynced = 0")
    suspend fun getUnsyncedProjects(): List<Project>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<Project>)

    @Query("UPDATE projects SET isDeletedOffline = 1, isSynced = 0 WHERE projectId = :id")
    suspend fun markAsDeletedOffline(id: String)

    @Query("DELETE FROM projects WHERE projectId = :id")
    suspend fun hardDeleteProject(id: String)

    @Query("DELETE FROM projects")
    suspend fun clearAll()
}
