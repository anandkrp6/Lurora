package com.bytecoder.lurora.frontend.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytecoder.lurora.backend.models.*
import com.bytecoder.lurora.frontend.viewmodels.AboutViewModel

/**
 * Comprehensive About Screen with vPlay branding and complete information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: AboutViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val aboutData by viewModel.aboutData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { Text("About vPlay") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App Information Header
                item {
                    AppInfoHeader(aboutData.appInfo)
                }

                // Version Information
                item {
                    VersionInfoSection(aboutData.appInfo)
                }

                // Developer Information
                item {
                    DeveloperInfoSection(
                        developerInfo = aboutData.developerInfo,
                        onEmailClick = { email -> viewModel.sendEmail(context, email) },
                        onUrlClick = { url -> viewModel.openUrl(context, url) }
                    )
                }

                // Technical Details
                item {
                    TechnicalDetailsSection(aboutData.technicalInfo)
                }

                // Legal Information
                item {
                    LegalInfoSection(
                        legalInfo = aboutData.legalInfo,
                        onUrlClick = { url -> viewModel.openUrl(context, url) }
                    )
                }

                // Third-party Libraries
                item {
                    ThirdPartyLibrariesSection(
                        libraries = aboutData.libraries,
                        onUrlClick = { url -> viewModel.openUrl(context, url) }
                    )
                }

                // Changelog
                item {
                    ChangelogSection(aboutData.changelog)
                }

                // Footer
                item {
                    FooterSection()
                }
            }
        }
    }
}

@Composable
private fun AppInfoHeader(appInfo: AppInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon/Logo placeholder
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "vPlay Logo",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = appInfo.appName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = "Your Ultimate Media Player",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun VersionInfoSection(appInfo: AppInfo) {
    InfoCard(
        title = "Version Information",
        icon = Icons.Default.Info
    ) {
        InfoRow("Version", appInfo.appVersion)
        InfoRow("Build Number", appInfo.buildNumber)
        InfoRow("Last Update", appInfo.lastUpdateDate)
        InfoRow("Package Name", appInfo.packageName)
    }
}

@Composable
private fun DeveloperInfoSection(
    developerInfo: DeveloperInfo,
    onEmailClick: (String) -> Unit,
    onUrlClick: (String) -> Unit
) {
    InfoCard(
        title = "Developer",
        icon = Icons.Default.Person
    ) {
        InfoRow("Developer", developerInfo.name)
        
        ClickableInfoRow(
            label = "Website",
            value = developerInfo.website,
            onClick = { onUrlClick(developerInfo.website) }
        )
        
        ClickableInfoRow(
            label = "Portfolio",
            value = "GitHub Profile",
            onClick = { onUrlClick(developerInfo.portfolioUrl) }
        )
        
        ClickableInfoRow(
            label = "Contact",
            value = developerInfo.email,
            onClick = { onEmailClick(developerInfo.email) }
        )
    }
}

@Composable
private fun TechnicalDetailsSection(technicalInfo: TechnicalInfo) {
    InfoCard(
        title = "Technical Details",
        icon = Icons.Default.Settings
    ) {
        InfoRow("Minimum Android", technicalInfo.minAndroidVersion)
        InfoRow("Target Android", technicalInfo.targetAndroidVersion)
        InfoRow("App Size", "${technicalInfo.appSizeMB} MB")
        
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Key Permissions:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            technicalInfo.keyPermissions.forEach { permission ->
                Text(
                    text = "• $permission",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun LegalInfoSection(
    legalInfo: LegalInfo,
    onUrlClick: (String) -> Unit
) {
    InfoCard(
        title = "Legal Information",
        icon = Icons.Default.Gavel
    ) {
        ClickableInfoRow(
            label = "Privacy Policy",
            value = "View Policy",
            onClick = { onUrlClick(legalInfo.privacyPolicyUrl) }
        )
        
        ClickableInfoRow(
            label = "Terms of Service",
            value = "View Terms",
            onClick = { onUrlClick(legalInfo.termsOfServiceUrl) }
        )
        
        ClickableInfoRow(
            label = "Open Source Licenses",
            value = "View Licenses",
            onClick = { onUrlClick(legalInfo.openSourceLicensesUrl) }
        )
    }
}

@Composable
private fun ThirdPartyLibrariesSection(
    libraries: List<LibraryCredit>,
    onUrlClick: (String) -> Unit
) {
    InfoCard(
        title = "Third-party Libraries",
        icon = Icons.Default.LibraryBooks
    ) {
        libraries.forEach { library ->
            LibraryItem(
                library = library,
                onUrlClick = onUrlClick
            )
            if (library != libraries.last()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun LibraryItem(
    library: LibraryCredit,
    onUrlClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUrlClick(library.url) }
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = library.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = library.license,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Text(
            text = library.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ChangelogSection(changelog: List<ChangelogEntry>) {
    InfoCard(
        title = "Changelog",
        icon = Icons.Default.History
    ) {
        changelog.take(3).forEach { entry ->
            ChangelogItem(entry)
            if (entry != changelog.take(3).last()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }
        
        if (changelog.size > 3) {
            Text(
                text = "View Full Changelog",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clickable { /* TODO: Open full changelog */ }
            )
        }
    }
}

@Composable
private fun ChangelogItem(entry: ChangelogEntry) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Version ${entry.version}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            val typeColor = when (entry.type) {
                ChangelogType.MAJOR -> MaterialTheme.colorScheme.primary
                ChangelogType.MINOR -> MaterialTheme.colorScheme.secondary
                ChangelogType.PATCH -> MaterialTheme.colorScheme.tertiary
                ChangelogType.HOTFIX -> MaterialTheme.colorScheme.error
            }
            
            Text(
                text = entry.date,
                style = MaterialTheme.typography.labelSmall,
                color = typeColor
            )
        }
        
        entry.changes.forEach { change ->
            Text(
                text = change,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp, start = 8.dp)
            )
        }
    }
}

@Composable
private fun FooterSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Made with ❤️ by ByteCoder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "© 2025 ByteCoder. All rights reserved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ClickableInfoRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open",
                modifier = Modifier
                    .size(16.dp)
                    .padding(start = 4.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}