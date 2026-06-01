package com.example

import android.app.Application
import com.example.di.ServiceLocator

class EditFlowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Force pre-init of repos
        ServiceLocator.provideDatabase(this)
        ServiceLocator.provideProjectRepository(this)
        ServiceLocator.provideAuthRepository(this)
    }
}
