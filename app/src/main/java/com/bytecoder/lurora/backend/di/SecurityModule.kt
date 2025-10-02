package com.bytecoder.lurora.backend.di

import android.content.Context
import com.bytecoder.lurora.backend.data.database.dao.SecurityAuditDao
import com.bytecoder.lurora.backend.database.LuroraDatabase
import com.bytecoder.lurora.backend.security.SecurityManager
import com.bytecoder.lurora.backend.security.SecurityAuditLogger
import com.bytecoder.lurora.backend.security.PermissionValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for security-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    
    @Provides
    @Singleton
    fun provideSecurityManager(
        @ApplicationContext context: Context
    ): SecurityManager {
        return SecurityManager(context)
    }
    
    @Provides
    @Singleton
    fun provideSecurityAuditDao(
        database: LuroraDatabase
    ): SecurityAuditDao {
        return database.securityAuditDao()
    }
    
    @Provides
    @Singleton
    fun provideSecurityAuditLogger(
        @ApplicationContext context: Context,
        securityAuditDao: SecurityAuditDao,
        securityManager: SecurityManager
    ): SecurityAuditLogger {
        return SecurityAuditLogger(context, securityAuditDao, securityManager)
    }
    
    @Provides
    @Singleton
    fun providePermissionValidator(
        @ApplicationContext context: Context,
        securityAuditLogger: SecurityAuditLogger
    ): PermissionValidator {
        return PermissionValidator(context, securityAuditLogger)
    }
}