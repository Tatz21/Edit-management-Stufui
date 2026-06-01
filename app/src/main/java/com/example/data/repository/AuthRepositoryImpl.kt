package com.example.data.repository

import android.content.Context
import com.example.domain.model.AuthUser
import com.example.domain.repository.AuthRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(private val context: Context) : AuthRepository {
    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    
    init {
        // Safely check if Firebase has been initialized
        val isFirebaseAvailable = try {
            FirebaseApp.initializeApp(context)
            true
        } catch (e: Exception) {
            false
        }
        
        if (isFirebaseAvailable) {
            try {
                val auth = FirebaseAuth.getInstance()
                auth.addAuthStateListener { firebaseAuth ->
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        _currentUser.value = AuthUser(
                            uid = user.uid,
                            name = user.displayName ?: "Admin",
                            email = user.email ?: "",
                            photoUrl = user.photoUrl?.toString() ?: ""
                        )
                    } else {
                        checkDemoPreferences()
                    }
                }
            } catch (e: Exception) {
                checkDemoPreferences()
            }
        } else {
            checkDemoPreferences()
        }
    }

    private fun checkDemoPreferences() {
        val prefs = context.getSharedPreferences("editflow_auth", Context.MODE_PRIVATE)
        val isLoggedOut = prefs.getBoolean("logged_out", false)
        if (isLoggedOut) {
            _currentUser.value = null
            return
        }
        val uid = prefs.getString("demo_uid", null)
        if (uid != null) {
            _currentUser.value = AuthUser(
                uid = uid,
                name = prefs.getString("demo_name", "EditFlow Admin") ?: "EditFlow Admin",
                email = prefs.getString("demo_email", "tatzmondal@gmail.com") ?: "tatzmondal@gmail.com",
                photoUrl = ""
            )
        } else {
            // Auto sign in to Sandbox on first launch or if unconfigured
            _currentUser.value = AuthUser(
                uid = "demo_admin_uid_123",
                name = "EditFlow Admin",
                email = "tatzmondal@gmail.com",
                photoUrl = ""
            )
        }
    }

    override fun getSignedInUser(): Flow<AuthUser?> = _currentUser.asStateFlow()

    override suspend fun signInWithGoogleToken(idToken: String): Result<AuthUser> {
        return try {
            val auth = FirebaseAuth.getInstance()
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val fUser = authResult.user ?: throw Exception("Google User of Firebase is null")
            val authUser = AuthUser(
                uid = fUser.uid,
                name = fUser.displayName ?: "Admin",
                email = fUser.email ?: "",
                photoUrl = fUser.photoUrl?.toString() ?: ""
            )
            val prefs = context.getSharedPreferences("editflow_auth", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("logged_out", false).apply()
            _currentUser.value = authUser
            Result.success(authUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun debugSignIn(email: String, name: String): AuthUser {
        val prefs = context.getSharedPreferences("editflow_auth", Context.MODE_PRIVATE)
        val uid = "demo_admin_uid_123"
        prefs.edit()
            .putBoolean("logged_out", false)
            .putString("demo_uid", uid)
            .putString("demo_name", name)
            .putString("demo_email", email)
            .apply()
            
        val authUser = AuthUser(uid = uid, name = name, email = email, photoUrl = "")
        _currentUser.value = authUser
        return authUser
    }

    override suspend fun signOut() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            // Ignore Firebase initialization error
        }
        val prefs = context.getSharedPreferences("editflow_auth", Context.MODE_PRIVATE)
        prefs.edit().clear().putBoolean("logged_out", true).apply()
        _currentUser.value = null
    }

    override fun isFirebaseConfigured(): Boolean {
        return try {
            FirebaseApp.getInstance()
            true
        } catch (e: Exception) {
            false
        }
    }
}
