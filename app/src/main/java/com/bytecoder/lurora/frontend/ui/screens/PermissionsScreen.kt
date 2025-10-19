package com.bytecoder.lurora.frontend.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytecoder.lurora.backend.models.AppPermission
import com.bytecoder.lurora.frontend.ui.components.PermissionItem
import com.bytecoder.lurora.frontend.ui.components.PermissionsSummary
import com.bytecoder.lurora.frontend.viewmodels.PermissionsViewModel

/**
 * Comprehensive Permissions Screen with all permissions listed by category
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    viewModel: PermissionsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissionsState by viewModel.permissionsState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    // State to track last requested permission for fallback handling
    var lastRequestedPermission by remember { mutableStateOf<AppPermission?>(null) }
    
    // Permission launcher for handling permission requests
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if any permissions were denied and might need settings fallback
        permissions.forEach { (permission, isGranted) ->
            if (!isGranted) {
                // Find the permission object
                val permissionObj = permissionsState
                    .find { it.androidPermission == permission }
                
                permissionObj?.let { perm ->
                    // Check if we should redirect to settings (permission permanently denied)
                    val activity = context as? androidx.activity.ComponentActivity
                    if (activity != null) {
                        val shouldShowRationale = androidx.core.app.ActivityCompat
                            .shouldShowRequestPermissionRationale(activity, permission)
                        
                        // If shouldShowRationale is false and permission is denied,
                        // it means user denied it permanently
                        if (!shouldShowRationale) {
                            // Redirect to settings
                            viewModel.openAppSettings(context)
                        }
                    }
                }
            }
        }
        // Update permission status after request
        viewModel.updatePermissionStatus(context)
    }
    
    // Single permission launcher
    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // If permission was denied, check if we should redirect to settings
        if (!isGranted && lastRequestedPermission != null) {
            val activity = context as? androidx.activity.ComponentActivity
            if (activity != null) {
                val shouldShowRationale = androidx.core.app.ActivityCompat
                    .shouldShowRequestPermissionRationale(activity, lastRequestedPermission!!.androidPermission)
                
                // If shouldShowRationale is false and permission is denied,
                // it means user denied it permanently
                if (!shouldShowRationale) {
                    // Redirect to settings
                    viewModel.openAppSettings(context)
                }
            }
        }
        
        // Clear the last requested permission
        lastRequestedPermission = null
        
        // Update permission status after request
        viewModel.updatePermissionStatus(context)
    }
    
    // Load and update permission status when screen becomes visible or when returning from settings
    LaunchedEffect(Unit) {
        viewModel.loadAndUpdatePermissions(context)
    }
    
    // Listen for app resume to update status when returning from settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updatePermissionStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { Text("App Permissions") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(
                    onClick = { viewModel.updatePermissionStatus(context) }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        )
        
        if (isLoading) {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Loading permissions...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else if (permissionsState.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "No permissions",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "No permissions found",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Please check your app configuration",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Permissions content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary card
                item {
                    val grantedCount = viewModel.getGrantedPermissionsCount()
                    val totalCount = viewModel.getTotalPermissionsCount()
                    val criticalPermissionsGranted = viewModel.areAllCriticalPermissionsGranted()
                    
                    PermissionsSummary(
                        grantedCount = grantedCount,
                        totalCount = totalCount,
                        criticalPermissionsGranted = criticalPermissionsGranted
                    )
                }
                
                // Grant all essential permissions button
                item {
                    val criticalPermissionsGranted = viewModel.areAllCriticalPermissionsGranted()
                    if (!criticalPermissionsGranted) {
                        Button(
                            onClick = {
                                val criticalPermissions = permissionsState
                                    .filter { it.importance == com.bytecoder.lurora.backend.models.PermissionImportance.CRITICAL && 
                                              it.status != com.bytecoder.lurora.backend.models.PermissionStatus.GRANTED }
                                
                                val regularPermissions = criticalPermissions.filter { !it.requiresSpecialHandling }
                                val specialPermissions = criticalPermissions.filter { it.requiresSpecialHandling }
                                
                                // Request regular permissions using launcher
                                if (regularPermissions.isNotEmpty()) {
                                    val permissionsToRequest = regularPermissions.map { it.androidPermission }.toTypedArray()
                                    permissionLauncher.launch(permissionsToRequest)
                                }
                                
                                // Handle special permissions
                                specialPermissions.forEach { permission ->
                                    viewModel.requestSpecialPermission(context, permission)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Grant All Essential Permissions",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
                
                // All permissions list
                items(permissionsState) { permission ->
                    PermissionItem(
                        permission = permission,
                        onAllowClick = {
                            when {
                                permission.requiresSpecialHandling -> {
                                    // Handle special permissions that require settings navigation
                                    viewModel.requestSpecialPermission(context, permission)
                                }
                                permission.id == "read_media_visual" -> {
                                    // Handle combined photos/videos permission
                                    val permissionsToRequest = mapOf(
                                        android.Manifest.permission.READ_MEDIA_VIDEO to false,
                                        android.Manifest.permission.READ_MEDIA_IMAGES to false
                                    )
                                    permissionLauncher.launch(permissionsToRequest.keys.toTypedArray())
                                }
                                else -> {
                                    // Check if permission is actually needed and can be requested
                                    if (viewModel.shouldRequestPermission(context, permission)) {
                                        // Set the last requested permission for fallback handling
                                        lastRequestedPermission = permission
                                        singlePermissionLauncher.launch(permission.androidPermission)
                                    } else {
                                        // Update status anyway
                                        viewModel.updatePermissionStatus(context)
                                    }
                                }
                            }
                        }
                    )
                }
                
                // Footer spacing
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}