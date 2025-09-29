package com.bytecoder.lurora.frontend.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytecoder.lurora.R
import com.bytecoder.lurora.frontend.viewmodels.AboutViewModel
import com.bytecoder.lurora.frontend.viewmodels.TeamMember
import com.bytecoder.lurora.frontend.viewmodels.AppInfo
import com.bytecoder.lurora.frontend.viewmodels.AppStatistics

/**
 * About Screen with comprehensive app information and team details
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: AboutViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { Text("About Lurora") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(
                    onClick = { viewModel.shareApp() }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share app")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Logo and Name
            item {
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
                        // App Icon
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Lurora Logo",
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Lurora",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "The Ultimate Media Player",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Version Badge
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Version ${uiState.appVersion}",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // App Description
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "About Lurora",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "Lurora is a powerful, feature-rich media player designed to provide the ultimate entertainment experience. With support for multiple formats, advanced playback controls, and a beautiful Material 3 interface, Lurora makes enjoying your media collection effortless and enjoyable.",
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Key Features
                        Text(
                            text = "Key Features",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        val features = listOf(
                            "🎬 High-quality video playback with multiple format support",
                            "🎵 Advanced audio player with equalizer and effects",
                            "📱 Picture-in-picture mode for seamless multitasking", 
                            "📁 Powerful file management and organization",
                            "🌙 Beautiful dark and light themes",
                            "🔄 Cross-platform synchronization",
                            "🎛️ Extensive customization options",
                            "🔒 Privacy-focused with offline capabilities"
                        )
                        
                        features.forEach { feature ->
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // Development Team
            item {
                TeamSection(
                    teamMembers = uiState.teamMembers,
                    onMemberClick = { member -> viewModel.openMemberProfile(member.profileUrl) }
                )
            }
            
            // Quick Actions
            item {
                QuickActionsSection(
                    onRateApp = { viewModel.rateApp() },
                    onReportBug = { viewModel.reportBug() },
                    onFeatureRequest = { viewModel.requestFeature() },
                    onJoinCommunity = { viewModel.joinCommunity() }
                )
            }
            
            // Version Information
            item {
                VersionInfoSection(
                    appInfo = uiState.appInfo,
                    onCheckUpdates = { viewModel.checkForUpdates() }
                )
            }
            
            // Legal Information
            item {
                LegalSection(
                    onPrivacyPolicy = { viewModel.openPrivacyPolicy() },
                    onTermsOfService = { viewModel.openTermsOfService() },
                    onLicenses = { viewModel.openLicenses() },
                    onSourceCode = { viewModel.openSourceCode() }
                )
            }
            
            // Statistics
            item {
                StatisticsSection(statistics = uiState.appStatistics)
            }
            
            // Social Links
            item {
                SocialLinksSection(
                    onGitHub = { viewModel.openGitHub() },
                    onTwitter = { viewModel.openTwitter() },
                    onDiscord = { viewModel.openDiscord() },
                    onWebsite = { viewModel.openWebsite() }
                )
            }
            
            // Footer
            item {
                FooterSection()
            }
        }
    }
}

@Composable
fun TeamSection(
    teamMembers: List<TeamMember>,
    onMemberClick: (TeamMember) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Meet the Team",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            teamMembers.forEach { member ->
                TeamMemberCard(
                    member = member,
                    onClick = { onMemberClick(member) }
                )
            }
        }
    }
}

@Composable
fun TeamMemberCard(
    member: TeamMember,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = member.role,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (member.bio.isNotEmpty()) {
                    Text(
                        text = member.bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View profile",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickActionsSection(
    onRateApp: () -> Unit,
    onReportBug: () -> Unit,
    onFeatureRequest: () -> Unit,
    onJoinCommunity: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AboutQuickActionButton(
                    icon = Icons.Default.Star,
                    label = "Rate App",
                    onClick = onRateApp,
                    modifier = Modifier.weight(1f)
                )
                AboutQuickActionButton(
                    icon = Icons.Default.BugReport,
                    label = "Report Bug",
                    onClick = onReportBug,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AboutQuickActionButton(
                    icon = Icons.Default.Lightbulb,
                    label = "Request Feature",
                    onClick = onFeatureRequest,
                    modifier = Modifier.weight(1f)
                )
                AboutQuickActionButton(
                    icon = Icons.Default.Groups,
                    label = "Join Community",
                    onClick = onJoinCommunity,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun AboutQuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun VersionInfoSection(
    appInfo: AppInfo,
    onCheckUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Version Information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                TextButton(onClick = onCheckUpdates) {
                    Text("Check Updates")
                }
            }
            
            VersionInfoRow("Version", appInfo.version)
            VersionInfoRow("Build", appInfo.buildNumber)
            VersionInfoRow("Released", appInfo.releaseDate)
            VersionInfoRow("Min Android", "API ${appInfo.minSdkVersion}")
            VersionInfoRow("Target Android", "API ${appInfo.targetSdkVersion}")
            VersionInfoRow("Architecture", appInfo.architecture)
        }
    }
}

@Composable
fun VersionInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
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
fun LegalSection(
    onPrivacyPolicy: () -> Unit,
    onTermsOfService: () -> Unit,
    onLicenses: () -> Unit,
    onSourceCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Legal & Open Source",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LegalItem(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy Policy",
                subtitle = "How we protect your data",
                onClick = onPrivacyPolicy
            )
            
            LegalItem(
                icon = Icons.Default.Gavel,
                title = "Terms of Service",
                subtitle = "Usage terms and conditions",
                onClick = onTermsOfService
            )
            
            LegalItem(
                icon = Icons.Default.Description,
                title = "Open Source Licenses",
                subtitle = "Third-party libraries and licenses",
                onClick = onLicenses
            )
            
            LegalItem(
                icon = Icons.Default.Code,
                title = "Source Code",
                subtitle = "View the project on GitHub",
                onClick = onSourceCode
            )
        }
    }
}

@Composable
fun LegalItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = "Open",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatisticsSection(
    statistics: AppStatistics,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "App Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    value = statistics.totalDownloads,
                    label = "Downloads",
                    icon = Icons.Default.Download
                )
                StatisticItem(
                    value = statistics.userRating,
                    label = "Rating",
                    icon = Icons.Default.Star
                )
                StatisticItem(
                    value = statistics.activeUsers,
                    label = "Active Users",
                    icon = Icons.Default.People
                )
            }
        }
    }
}

@Composable
fun StatisticItem(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SocialLinksSection(
    onGitHub: () -> Unit,
    onTwitter: () -> Unit,
    onDiscord: () -> Unit,
    onWebsite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Connect With Us",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SocialButton(
                    icon = Icons.Default.Code,
                    label = "GitHub",
                    onClick = onGitHub
                )
                SocialButton(
                    icon = Icons.Default.Chat,
                    label = "Twitter",
                    onClick = onTwitter
                )
                SocialButton(
                    icon = Icons.Default.Forum,
                    label = "Discord",
                    onClick = onDiscord
                )
                SocialButton(
                    icon = Icons.Default.Language,
                    label = "Website",
                    onClick = onWebsite
                )
            }
        }
    }
}

@Composable
fun SocialButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledIconButton(
            onClick = onClick,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun FooterSection(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Made with ❤️ by the Lurora Team",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "© 2024 Lurora. All rights reserved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Thank you for using Lurora! 🎬",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}