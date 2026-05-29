package com.example.core.di

import android.content.Context
import com.example.core.logger.ILogger
import com.example.core.logger.LoggerFactory
import com.example.core.network.NetworkMonitor
import com.example.data.AppDatabase
import com.example.data.AccountingRepository

/**
 * A simple type-safe lightweight dependency injection container for the ERP core
 */
class ServiceContainer private constructor(val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    val repository: AccountingRepository by lazy {
        AccountingRepository(database)
    }

    val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(context)
    }

    val logger: ILogger by lazy {
        LoggerFactory.getLogger(isDebug = true) // Set to false under release configuration
    }

    val settingsManager: com.example.core.settings.SettingsManager by lazy {
        com.example.core.settings.SettingsManager.initialize(context)
    }

    val userManagementService: com.example.core.auth.UserManagementService by lazy {
        com.example.core.auth.UserManagementService.initialize(context)
    }

    companion object {
        @Volatile
        private var INSTANCE: ServiceContainer? = null

        fun initialize(context: Context): ServiceContainer {
            return INSTANCE ?: synchronized(this) {
                val instance = ServiceContainer(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(): ServiceContainer {
            return INSTANCE ?: throw IllegalStateException("ServiceContainer has not been initialized. Call initialize(context) first.")
        }
    }
}
