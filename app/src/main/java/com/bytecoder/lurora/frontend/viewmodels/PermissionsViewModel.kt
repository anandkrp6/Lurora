package com.bytecoder.lurora.frontend.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytecoder.lurora.backend.models.AppPermission
import com.bytecoder.lurora.backend.models.AppPermissions
import com.bytecoder.lurora.backend.models.PermissionImportance
import com.bytecoder.lurora.backend.models.PermissionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fixed PermissionsViewModel using correct data models
 */
@HiltViewModel
class PermissionsViewModel @Inject constructor() : ViewModel() {
    
    private val _permissions = MutableStateFlow<List<AppPermission>>(emptyList())
    val permissions: StateFlow<List<AppPermission>> = _permissions.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _showOnlyRequired = MutableStateFlow(false)
    val showOnlyRequired: StateFlow<Boolean> = _showOnlyRequired.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<PermissionImportance?>(null)
    val selectedCategory: StateFlow<PermissionImportance?> = _selectedCategory.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    val categorizedPermissions: StateFlow<Map<PermissionImportance, List<AppPermission>>> = 
        _permissions.map { permissions ->
            permissions.groupBy { it.importance }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
    
    init {
        loadPermissions()
    }
    
    private fun loadPermissions() {
        // Use the predefined permissions from AppPermissions
        _permissions.value = AppPermissions.getAllPermissions()
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun setShowOnlyRequired(showOnly: Boolean) {
        _showOnlyRequired.value = showOnly
    }
    
    fun setSelectedCategory(category: PermissionImportance?) {
        _selectedCategory.value = category
    }
    
    fun togglePermission(permission: AppPermission) {
        if (permission.importance == PermissionImportance.CRITICAL) return
        
        viewModelScope.launch {
            try {
                // Simulate permission toggle with proper state management
                val currentPermissions = _permissions.value.toMutableList()
                val permissionIndex = currentPermissions.indexOfFirst { it.name == permission.name }
                
                if (permissionIndex != -1) {
                    val newStatus = if (permission.status == PermissionStatus.GRANTED) 
                        PermissionStatus.DENIED else PermissionStatus.GRANTED
                    
                    currentPermissions[permissionIndex] = currentPermissions[permissionIndex].copy(
                        status = newStatus
                    )
                    _permissions.value = currentPermissions
                }
                
            } catch (e: Exception) {
                // Handle permission toggle error
                _errorMessage.value = "Failed to toggle permission: ${e.message}"
            }
        }
    }
    
    fun requestMissingPermissions() {
        viewModelScope.launch {
            val requiredPermissions = _permissions.value.filter { 
                it.isRequired && it.status != PermissionStatus.GRANTED 
            }
            
            if (requiredPermissions.isNotEmpty()) {
                try {
                    // Simulate permission request process
                    // In a real app, this would call ActivityCompat.requestPermissions()
                    val updatedPermissions = _permissions.value.toMutableList()
                    
                    requiredPermissions.forEach { permission ->
                        val index = updatedPermissions.indexOfFirst { it.name == permission.name }
                        if (index != -1) {
                            // Simulate some permissions being granted, others denied
                            val granted = when (permission.importance) {
                                PermissionImportance.CRITICAL -> true
                                PermissionImportance.HIGH -> kotlin.random.Random.nextFloat() > 0.2f // 80% chance
                                PermissionImportance.MEDIUM -> kotlin.random.Random.nextFloat() > 0.4f // 60% chance
                                PermissionImportance.LOW -> kotlin.random.Random.nextFloat() > 0.6f // 40% chance
                            }
                            
                            updatedPermissions[index] = updatedPermissions[index].copy(
                                status = if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED
                            )
                        }
                    }
                    
                    _permissions.value = updatedPermissions
                    
                } catch (e: Exception) {
                    _errorMessage.value = "Failed to request permissions: ${e.message}"
                }
            }
        }
    }
    
    fun clearFilters() {
        _searchQuery.value = ""
        _showOnlyRequired.value = false
        _selectedCategory.value = null
    }
}

/**
 * Permission statistics data class
 */
data class PermissionStats(
    val total: Int,
    val granted: Int,
    val required: Int,
    val grantedRequired: Int,
    val deniedRequired: Int
) {
    val grantedPercentage: Float
        get() = if (total > 0) (granted.toFloat() / total) * 100f else 0f
        
    val isHealthy: Boolean
        get() = grantedRequired >= deniedRequired
}