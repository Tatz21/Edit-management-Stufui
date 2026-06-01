package com.example.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.ProjectRepositoryImpl
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.ProjectRepository

object ServiceLocator {
    private var database: AppDatabase? = null
    private var projectRepository: ProjectRepository? = null
    private var authRepository: AuthRepository? = null

    fun provideDatabase(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            val db = AppDatabase.getDatabase(context)
            database = db
            db
        }
    }

    fun provideProjectRepository(context: Context): ProjectRepository {
        return projectRepository ?: synchronized(this) {
            val repo = ProjectRepositoryImpl(
                projectDao = provideDatabase(context).projectDao(),
                context = context.applicationContext
            )
            projectRepository = repo
            repo
        }
    }

    fun provideAuthRepository(context: Context): AuthRepository {
        return authRepository ?: synchronized(this) {
            val repo = AuthRepositoryImpl(context.applicationContext)
            authRepository = repo
            repo
        }
    }
}
