package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.di.ServiceLocator
import com.example.presentation.MainViewModel
import com.example.presentation.MainViewModelFactory
import com.example.presentation.MainWorkspaceFrame
import com.example.ui.theme.EditFlowTheme

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-To-Edge drawing compatibility setup
        enableEdgeToEdge()
        
        // Obtain Single Repositories via dependency injection locator
        val authRepo = ServiceLocator.provideAuthRepository(this)
        val projectRepo = ServiceLocator.provideProjectRepository(this)
        
        // Inject Viewmodel with custom Factory
        val factory = MainViewModelFactory(authRepo, projectRepo)
        val viewModel: MainViewModel by viewModels { factory }

        // Load dynamically managed editors list
        viewModel.loadEditors(this)

        // Start background checks for deadlines, payments or overdue contracts
        viewModel.triggerAutomatedNotificationChecks()

        setContent {
            val isDark by viewModel.isDarkTheme.collectAsState()
            
            EditFlowTheme(darkTheme = isDark) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainWorkspaceFrame(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
