package com.bytecoder.lurora.frontend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bytecoder.lurora.backend.models.AppPermission
import com.bytecoder.lurora.backend.models.PermissionImportance
import com.bytecoder.lurora.backend.models.PermissionStatus

/**
 * Composable for displaying a single permission item with status indicator and allow button
 */
@Composable
fun PermissionItem(
    permission: AppPermission,
    onAllowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row with title, status icon, and allow button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and status icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    PermissionStatusIcon(
                        status = permission.status,
                        importance = permission.importance
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = permission.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        if (permission.importance == PermissionImportance.CRITICAL) {
                            Text(
                                text = "Required",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Allow button
                if (permission.status != PermissionStatus.GRANTED) {
                    Button(
                        onClick = onAllowClick,
                        modifier = Modifier.padding(start = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (permission.importance == PermissionImportance.CRITICAL) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    ) {
                        if (permission.requiresSpecialHandling) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (permission.requiresSpecialHandling) "Settings" else "Allow",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            Text(
                text = if (permission.status == PermissionStatus.LIMITED && permission.id == "read_media_visual") {
                    "Access to selected photos and videos only. For optimal use, allow all."
                } else {
                    permission.description
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (permission.status == PermissionStatus.LIMITED) {
                    Color(0xFFE65100) // Orange color for LIMITED status
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            // Status text and special handling info
            if (permission.status == PermissionStatus.GRANTED) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Permission granted",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (permission.requiresSpecialHandling) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Requires Settings access",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

/**
 * Status icon showing tick (granted) or cross (denied/not requested)
 */
@Composable
fun PermissionStatusIcon(
    status: PermissionStatus,
    importance: PermissionImportance,
    modifier: Modifier = Modifier
) {
    val (icon, backgroundColor, iconColor) = when (status) {
        PermissionStatus.GRANTED -> Triple(
            Icons.Default.Check,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary
        )
        PermissionStatus.DENIED, PermissionStatus.NOT_REQUESTED -> {
            val bgColor = if (importance == PermissionImportance.CRITICAL) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.outline
            }
            Triple(
                Icons.Default.Close,
                bgColor,
                MaterialTheme.colorScheme.onError
            )
        }
        PermissionStatus.LIMITED -> Triple(
            Icons.Default.Warning,
            Color(0xFFFFC107), // Amber/Yellow color
            Color(0xFF000000)  // Black text on yellow
        )
    }
    
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = when (status) {
                PermissionStatus.GRANTED -> "Permission granted"
                PermissionStatus.DENIED -> "Permission denied"
                PermissionStatus.NOT_REQUESTED -> "Permission not requested"
                PermissionStatus.LIMITED -> "Limited access - selected files only"
            },
            tint = iconColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Category header for grouping permissions
 */
@Composable
fun PermissionCategoryHeader(
    categoryName: String,
    grantedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = categoryName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = "$grantedCount/$totalCount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Overall permissions status summary
 */
@Composable
fun PermissionsSummary(
    grantedCount: Int,
    totalCount: Int,
    criticalPermissionsGranted: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (criticalPermissionsGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (criticalPermissionsGranted) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = if (criticalPermissionsGranted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = if (criticalPermissionsGranted) {
                            "All Essential Permissions Granted"
                        } else {
                            "Some Essential Permissions Missing"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (criticalPermissionsGranted) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    
                    Text(
                        text = "$grantedCount of $totalCount permissions granted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (criticalPermissionsGranted) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        }
                    )
                }
            }
            
            if (!criticalPermissionsGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Grant essential permissions to use all app features.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}
