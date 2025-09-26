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
    currentSection: MoreSection,
    onSectionChange: (MoreSection) -> Unit,
    modifier: Modifier = Modifier
) {
    // Define option items with icons
    val optionItems = listOf(
        MoreItem(MoreSection.HISTORY, Icons.Default.History, "View your watch and play history"),
        MoreItem(MoreSection.DOWNLOADS, Icons.Default.Download, "Manage your downloaded files"),
        MoreItem(MoreSection.PERMISSIONS, Icons.Default.Security, "App permissions and access"),
        MoreItem(MoreSection.FILE_EXPLORER, Icons.Default.Folder, "Browse and manage files"),
        MoreItem(MoreSection.SETTINGS, Icons.Default.Settings, "App preferences and configuration"),
        MoreItem(MoreSection.FEEDBACK, Icons.Default.Feedback, "Send feedback and suggestions"),
        MoreItem(MoreSection.ABOUT, Icons.Default.Info, "About Lurora and app information"),
        MoreItem(MoreSection.TIPS, Icons.Default.Lightbulb, "Tips and tricks for using the app")
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(optionItems) { item ->
            MoreItemCard(
                item = item,
                onClick = { onSectionChange(item.section) }
            )
        }
    }
}

data class MoreItem(
    val section: MoreSection,
    val icon: ImageVector,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreItemCard(
    item: MoreItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.section.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MoreTabContentPreview() {
    MaterialTheme {
        MoreTabContent(
            currentSection = MoreSection.HISTORY,
            onSectionChange = {}
        )
    }
}