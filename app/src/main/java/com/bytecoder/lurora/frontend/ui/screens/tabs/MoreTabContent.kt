package com.bytecoder.lurora.frontend.ui.screens.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bytecoder.lurora.frontend.navigation.*

@Composable
fun MoreTabContent(
    currentMoreTab: MoreTab,
    onMoreTabChange: (MoreTab) -> Unit,
    modifier: Modifier = Modifier
) {
    // Get all available MoreTab items directly from the enum
    val moreTabItems = MoreTab.values().toList()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(moreTabItems) { moreTab ->
            MoreTabCard(
                moreTab = moreTab,
                isSelected = moreTab == currentMoreTab,
                onClick = { onMoreTabChange(moreTab) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreTabCard(
    moreTab: MoreTab,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = moreTab.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = moreTab.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = moreTab.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Navigate",
                    tint = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Show available features for this tab
            val features = NavigationHelper.getMoreTabFeatures(moreTab)
            if (features.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Features: ${features.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f) 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MoreTabContentPreview() {
    MaterialTheme {
        MoreTabContent(
            currentMoreTab = MoreTab.HISTORY,
            onMoreTabChange = {}
        )
    }
}