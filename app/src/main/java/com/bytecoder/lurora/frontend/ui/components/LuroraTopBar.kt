package com.bytecoder.lurora.frontend.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytecoder.lurora.R
import com.bytecoder.lurora.frontend.navigation.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuroraTopBar(
    currentTab: MainTab,
    onSearchClick: () -> Unit = {},
    onSortClick: () -> Unit = {},
    onFilterClick: () -> Unit = {},
    onViewOptionClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left - App Logo + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Lurora Logo",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape) // Make logo circular
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = currentTab.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                
                // Right - Action Icons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search Icon
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                    
                    // Sort Icon
                    if (NavigationHelper.getSortOptionsForTab(currentTab).isNotEmpty()) {
                        IconButton(onClick = onSortClick) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort"
                            )
                        }
                    }
                    
                    // Filter Icon
                    IconButton(onClick = onFilterClick) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter"
                        )
                    }
                    
                    // Overflow Menu
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            // View Options
                            DropdownMenuItem(
                                text = { Text("View Options") },
                                onClick = {
                                    onViewOptionClick()
                                    showOverflowMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ViewModule, contentDescription = null)
                                }
                            )
                            
                            // Refresh
                            DropdownMenuItem(
                                text = { Text("Refresh") },
                                onClick = {
                                    onMenuClick()
                                    showOverflowMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                }
                            )
                            
                            // Select All
                            DropdownMenuItem(
                                text = { Text("Select All") },
                                onClick = {
                                    showOverflowMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.SelectAll, contentDescription = null)
                                }
                            )
                            
                            Divider()
                            
                            // Settings
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    showOverflowMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}

@Composable
fun SortDialog(
    currentTab: MainTab,
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    val sortOptions = NavigationHelper.getSortOptionsForTab(currentTab)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort by") },
        text = {
            LazyColumn {
                items(sortOptions.size) { index ->
                    val option = sortOptions[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortSelected(option) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSort == option,
                            onClick = { onSortSelected(option) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option.title)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
fun FilterDialog(
    currentFilter: FilterOption,
    onFilterSelected: (FilterOption) -> Unit,
    onDismiss: () -> Unit
) {
    val filterOptions = FilterOption.values()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter by") },
        text = {
            Column {
                filterOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFilterSelected(option) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentFilter == option,
                            onClick = { onFilterSelected(option) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option.title)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
fun ViewOptionsDialog(
    currentView: ViewOption,
    onViewSelected: (ViewOption) -> Unit,
    onDismiss: () -> Unit
) {
    val viewOptions = ViewOption.values()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("View Options") },
        text = {
            Column {
                viewOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewSelected(option) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentView == option,
                            onClick = { onViewSelected(option) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option.title)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun LuroraTopBarPreview() {
    MaterialTheme {
        LuroraTopBar(
            currentTab = MainTab.VIDEO
        )
    }
}