package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.KaranTopBar
import com.example.ui.screens.AutomationScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.KaranTheme

enum class KaranNavTab(val title: String, val icon: ImageVector) {
    CHAT("Command", Icons.Default.Forum),
    VOICE("Voice", Icons.Default.Mic),
    CODING("Coding", Icons.Default.Code),
    AUTOMATION("Automation", Icons.Default.SmartToy),
    MEMORY("Memory", Icons.Default.Psychology),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KaranTheme {
                val viewModel: MainViewModel = viewModel()
                KaranMainApp(viewModel)
            }
        }
    }
}

@Composable
fun KaranMainApp(viewModel: MainViewModel) {
    var currentTabIndex by remember { mutableIntStateOf(0) }
    var showWelcomeDialog by remember { mutableStateOf(false) }

    // Check runtime permissions on launch
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshStatus()
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    val activeProject by viewModel.activeProject.collectAsState()
    val currentTask by viewModel.currentTask.collectAsState()
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()

    // Back button handling
    BackHandler(enabled = currentTabIndex != 0) {
        currentTabIndex = 0
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            KaranTopBar(
                activeProject = activeProject?.name,
                currentTask = currentTask,
                isOnline = deviceStatus.isOnline,
                isAccessibilityConnected = deviceStatus.accessibilityConnected,
                onProjectClick = { currentTabIndex = KaranNavTab.CODING.ordinal }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("karan_bottom_nav")
            ) {
                KaranNavTab.values().forEachIndexed { index, tab ->
                    val selected = currentTabIndex == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            selectedTextColor = CyberCyan,
                            indicatorColor = CyberCyan.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTabIndex) {
                KaranNavTab.CHAT.ordinal -> ChatScreen(viewModel = viewModel)
                KaranNavTab.VOICE.ordinal -> VoiceScreen(viewModel = viewModel)
                KaranNavTab.CODING.ordinal -> ProjectsScreen(viewModel = viewModel)
                KaranNavTab.AUTOMATION.ordinal -> AutomationScreen(viewModel = viewModel)
                KaranNavTab.MEMORY.ordinal -> MemoryScreen(viewModel = viewModel)
                KaranNavTab.SETTINGS.ordinal -> SettingsScreen(viewModel = viewModel)
            }
        }
    }

    // Confirmation dialog for consequential actions
    pendingConfirmation?.let { req ->
        ConfirmationDialog(
            request = req,
            onConfirm = { viewModel.resolveConfirmation(true) },
            onCancel = { viewModel.resolveConfirmation(false) }
        )
    }

    // First Run Onboarding Dialog
    if (showWelcomeDialog) {
        AlertDialog(
            onDismissRequest = { showWelcomeDialog = false },
            title = { Text("Welcome to KARAN", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "KARAN is your personal AI coding and device-assistance agent.\n\n" +
                    "• Speak in Hindi, Hinglish, or English.\n" +
                    "• Build, edit, and preview web apps in real-time.\n" +
                    "• Automate authorized UI workflows with Accessibility.\n" +
                    "• All local projects and memories are stored safely on your device."
                )
            },
            confirmButton = {
                Button(onClick = { showWelcomeDialog = false }) {
                    Text("Get Started")
                }
            }
        )
    }
}
