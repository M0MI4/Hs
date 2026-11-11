package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CupertinoGroupedCard
import com.example.ui.components.CupertinoSegmentedControl
import com.example.ui.components.CupertinoSettingsRow
import com.example.ui.components.FeedbackModalSheet
import com.example.ui.components.ParticleDensity
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalizedStrings
import com.example.ui.theme.ThemePreset
import com.example.ui.theme.VerifiedBadgeGreen
import com.example.ui.theme.iOSSystemBlue
import com.example.ui.theme.iOSSystemGreen
import com.example.ui.theme.iOSSystemIndigo
import com.example.ui.theme.iOSSystemOrange
import com.example.ui.theme.iOSSystemPurple
import com.example.ui.theme.iOSSystemRed
import com.example.ui.theme.iOSSystemTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    currentPreset: ThemePreset,
    isDarkTheme: Boolean,
    particlesEnabled: Boolean,
    particleDensity: ParticleDensity,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    isAdminLoggedIn: Boolean = false,
    adminEmail: String? = null,
    onAdminLogin: (String, String) -> Unit = { _, _ -> },
    onOpenAdminPanel: () -> Unit = {},
    adminLoading: Boolean = false,
    adminError: String? = null,
    onPresetSelected: (ThemePreset) -> Unit,
    onDarkThemeToggled: (Boolean) -> Unit,
    onParticlesEnabledToggled: (Boolean) -> Unit,
    onParticleDensityChanged: (ParticleDensity) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
    onSubmitFeedback: suspend (type: String, subject: String, details: String, deviceInfo: String) -> Result<String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showFeedbackSheet by remember { mutableStateOf(false) }

    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var showAdminLoginSheet by remember { mutableStateOf(false) }

    // When admin successfully logs in, dismiss the login sheet and open panel
    androidx.compose.runtime.LaunchedEffect(isAdminLoggedIn) {
        if (isAdminLoggedIn && showAdminLoginSheet) {
            showAdminLoginSheet = false
            onOpenAdminPanel()
        }
    }

    val onVersionTapped: () -> Unit = {
        val now = System.currentTimeMillis()
        if (now - lastTapTime > 2500) {
            tapCount = 1
        } else {
            tapCount += 1
        }
        lastTapTime = now

        if (tapCount >= 5) {
            tapCount = 0
            if (isAdminLoggedIn) {
                onOpenAdminPanel()
            } else {
                showAdminLoginSheet = true
            }
        }
    }

    val teamMembers = listOf(
        "Hassan S." to "Core Engine & Architecture",
        "Kuro" to "Mod Sandbox & Checksum Auditor",
        "Valerie" to "iOS Cupertino UI Design",
        "Dexter" to "Retrofit & Remote Protocols"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("about_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
    ) {
        // iOS Large Title Header
        item {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = LocalizedStrings.get("settings_kicker", currentLanguage),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                )
                Text(
                    text = LocalizedStrings.get("settings_title", currentLanguage),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp
                    )
                )
            }
        }

        // Section 1: Language Switcher (English / Kurdish Sorani)
        item {
            Text(
                text = LocalizedStrings.get("settings_language_section", currentLanguage),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
            )

            CupertinoGroupedCard {
                CupertinoSettingsRow(
                    title = LocalizedStrings.get("settings_language_title", currentLanguage),
                    subtitle = LocalizedStrings.get("settings_language_subtitle", currentLanguage),
                    icon = Icons.Default.Language,
                    iconBgColor = iOSSystemTeal,
                    showChevron = false,
                    showDivider = true
                )

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    CupertinoSegmentedControl(
                        items = listOf("English", "کوردیی سۆرانی"),
                        selectedIndex = if (currentLanguage == AppLanguage.ENGLISH) 0 else 1,
                        onItemSelected = { index ->
                            val chosenLang = if (index == 0) AppLanguage.ENGLISH else AppLanguage.KURDISH_SORANI
                            onLanguageChanged(chosenLang)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }



        // Section 3: Visual Experience & Particle Controls
        item {
            Text(
                text = LocalizedStrings.get("settings_appearance_section", currentLanguage),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
            )

            CupertinoGroupedCard {
                // Master Particle Toggle
                CupertinoSettingsRow(
                    title = LocalizedStrings.get("settings_particles_title", currentLanguage),
                    subtitle = if (particlesEnabled) LocalizedStrings.get("settings_particles_on", currentLanguage) else LocalizedStrings.get("settings_particles_off", currentLanguage),
                    icon = Icons.Default.AutoAwesome,
                    iconBgColor = iOSSystemPurple,
                    showChevron = false,
                    showDivider = true,
                    trailingContent = {
                        com.example.ui.components.CupertinoSwitch(
                            checked = particlesEnabled,
                            onCheckedChange = onParticlesEnabledToggled,
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.testTag("master_particle_switch")
                        )
                    }
                )

                // Particle Density Selector (visible when particles enabled)
                if (particlesEnabled) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            text = LocalizedStrings.get("settings_particles_density", currentLanguage),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CupertinoSegmentedControl(
                            items = listOf("Subtle", "Medium", "Dense"),
                            selectedIndex = when (particleDensity) {
                                ParticleDensity.SUBTLE -> 0
                                ParticleDensity.MEDIUM -> 1
                                ParticleDensity.DENSE -> 2
                                else -> 1
                            },
                            onItemSelected = { index ->
                                val density = when (index) {
                                    0 -> ParticleDensity.SUBTLE
                                    1 -> ParticleDensity.MEDIUM
                                    else -> ParticleDensity.DENSE
                                }
                                onParticleDensityChanged(density)
                            }
                        )
                    }

                    Divider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 58.dp)
                    )
                }

                // Dark / Light Appearance Switch
                CupertinoSettingsRow(
                    title = LocalizedStrings.get("settings_dark_mode_title", currentLanguage),
                    subtitle = if (isDarkTheme) LocalizedStrings.get("settings_dark_mode_on", currentLanguage) else LocalizedStrings.get("settings_dark_mode_off", currentLanguage),
                    icon = Icons.Default.DarkMode,
                    iconBgColor = iOSSystemIndigo,
                    showChevron = false,
                    showDivider = true,
                    trailingContent = {
                        com.example.ui.components.CupertinoSwitch(
                            checked = isDarkTheme,
                            onCheckedChange = onDarkThemeToggled,
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }
                )

                // Accent Theme Presets
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        text = LocalizedStrings.get("settings_accent_color", currentLanguage),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CupertinoSegmentedControl(
                        items = listOf("Blue", "Indigo", "Purple", "Mint"),
                        selectedIndex = when (currentPreset) {
                            ThemePreset.APPLE_BLUE -> 0
                            ThemePreset.INDIGO_PRO -> 1
                            ThemePreset.PURPLE_NEON -> 2
                            ThemePreset.EMERALD_MINT -> 3
                        },
                        onItemSelected = { index ->
                            val preset = when (index) {
                                0 -> ThemePreset.APPLE_BLUE
                                1 -> ThemePreset.INDIGO_PRO
                                2 -> ThemePreset.PURPLE_NEON
                                else -> ThemePreset.EMERALD_MINT
                            }
                            onPresetSelected(preset)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section 4: Feedback & Support (Remote JSON Host sections removed as requested)
        item {
            Text(
                text = LocalizedStrings.get("settings_support_section", currentLanguage),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
            )

            CupertinoGroupedCard {
                CupertinoSettingsRow(
                    title = LocalizedStrings.get("settings_feedback_title", currentLanguage),
                    subtitle = LocalizedStrings.get("settings_feedback_subtitle", currentLanguage),
                    icon = Icons.Default.Feedback,
                    iconBgColor = iOSSystemBlue,
                    onClick = { showFeedbackSheet = true },
                    showChevron = true,
                    showDivider = true
                )

                CupertinoSettingsRow(
                    title = "Telegram Community",
                    subtitle = "Join our modding collective & discussions",
                    icon = Icons.Default.SupportAgent,
                    iconBgColor = iOSSystemTeal,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/hamasade"))
                        context.startActivity(intent)
                    },
                    showChevron = true,
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section 4.5: Administrative Studio (Unlocked when Admin authenticated)
        if (isAdminLoggedIn) {
            item {
                Text(
                    text = "ADMINISTRATIVE STUDIO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = VerifiedBadgeGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )

                CupertinoGroupedCard {
                    CupertinoSettingsRow(
                        title = "Open Admin Studio",
                        subtitle = "Signed in as ${adminEmail ?: "Admin"} • Manage Apps & Broadcasts",
                        icon = Icons.Default.AdminPanelSettings,
                        iconBgColor = VerifiedBadgeGreen,
                        onClick = onOpenAdminPanel,
                        showDivider = false
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Section 5: Version History & Technical Specs
        item {
            Text(
                text = LocalizedStrings.get("settings_about_section", currentLanguage),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
            )

            CupertinoGroupedCard {
                CupertinoSettingsRow(
                    title = LocalizedStrings.get("settings_version_title", currentLanguage),
                    subtitle = "HsStore v1.3.0 (Build 30) • Apple Cupertino Edition",
                    icon = Icons.Default.Info,
                    iconBgColor = iOSSystemBlue,
                    onClick = onVersionTapped,
                    showDivider = true,
                    trailingContent = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "v1.3.0",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                )

                CupertinoSettingsRow(
                    title = LocalizedStrings.get("settings_engine_title", currentLanguage),
                    subtitle = "Jetpack Compose • Retrofit • Firebase Firestore",
                    icon = Icons.Default.History,
                    iconBgColor = iOSSystemIndigo,
                    showDivider = true
                )

                CupertinoSettingsRow(
                    title = "Zero-Trust Security Sandbox",
                    subtitle = "SHA-256 Checksum • Android 14 Photo Picker",
                    icon = Icons.Default.Security,
                    iconBgColor = VerifiedBadgeGreen,
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section 6: HAMA SADE TEAM Profile Banner
        item {
            Text(
                text = LocalizedStrings.get("hama_sade_team_title", currentLanguage),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
            )

            CupertinoGroupedCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar in Squircle shape
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier.size(68.dp)
                        ) {
                            coil.compose.AsyncImage(
                                model = com.example.R.drawable.hama_sade_avatar,
                                contentDescription = "Hama Sade Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "HAMA SADE",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Verified Creator",
                                    tint = VerifiedBadgeGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = LocalizedStrings.get("hama_sade_team_subtitle", currentLanguage),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            )

                            Text(
                                text = "@hamasade • Official Developer",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = LocalizedStrings.get("hama_sade_team_bio", currentLanguage),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Join Telegram Button
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/hamasade"))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LocalizedStrings.get("action_join_telegram", currentLanguage),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Copyright notice
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HsStore Cyberpunk Cupertino • 2026",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = "Crafted for high performance & privacy",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }

    // Feedback Bottom Sheet
    if (showFeedbackSheet) {
        FeedbackModalSheet(
            onDismiss = { showFeedbackSheet = false },
            onSubmitFeedback = onSubmitFeedback
        )
    }

    // Admin Login Bottom Sheet (Discrete 5-tap entry)
    if (showAdminLoginSheet) {
        AdminLoginBottomSheet(
            isLoading = adminLoading,
            errorMessage = adminError,
            onDismiss = { showAdminLoginSheet = false },
            onLogin = { email, password ->
                onAdminLogin(email, password)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminLoginBottomSheet(
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "HsStore Administrative Portal",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Text(
                text = "Authenticate with your administrator email & password to manage cloud catalog and news.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (errorMessage != null) {
                Surface(
                    color = iOSSystemRed.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, iOSSystemRed.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = iOSSystemRed,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Admin Email") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onLogin(email, password) },
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Authenticate as Administrator", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
