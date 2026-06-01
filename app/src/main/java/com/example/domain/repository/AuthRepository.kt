package com.example.domain.repository

import com.example.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getSignedInUser(): Flow<AuthUser?>
    
    suspend fun signInWithGoogleToken(idToken: String): Result<AuthUser>
    
    suspend fun debugSignIn(email: String = "tatzmondal@gmail.com", name: String = "EditFlow Admin"): AuthUser
    
    suspend fun signOut()
    
    fun isFirebaseConfigured(): Boolean
}
