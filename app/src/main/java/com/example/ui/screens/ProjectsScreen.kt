package com.example.ui.screens

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.coding.FileNode
import com.example.ui.MainViewModel
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonPurple

@Composable
fun ProjectsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.allProjects.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()
    val projectFiles by viewModel.projectFiles.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()
    val fileContent by viewModel.fileContent.collectAsState()
    val projectAnalysis by viewModel.projectAnalysis.collectAsState()
    val webPreviewHtml by viewModel.webPreviewHtml.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Editor, 1: Live Preview, 2: Analysis
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var editableContent by remember(fileContent) { mutableStateOf(fileContent) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Project Switcher Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                projects.forEach { proj ->
                    val isActive = proj.projectId == activeProject?.projectId
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isActive) CyberCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
                        modifier = Modifier.clickable { viewModel.selectProject(proj.projectId) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = proj.name,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) CyberCyan else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${proj.technology})",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Button(
                    onClick = { showNewProjectDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("create_project_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "New", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Project", fontSize = 12.sp)
                }
            }
        }

        // Mode Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = CyberCyan
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Code & Files") },
                icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Live Preview") },
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Diagnostics") },
                icon = { Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        when (selectedTab) {
            0 -> {
                // Code & Files Split Layout
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left sidebar: File Tree (130dp)
                    Surface(
                        modifier = Modifier
                            .width(140.dp)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "FILES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(projectFiles) { node ->
                                    FileNodeRow(
                                        node = node,
                                        selectedFile = selectedFile,
                                        onSelect = { viewModel.loadFile(it) }
                                    )
                                }
                            }
                        }
                    }

                    // Right pane: Code Editor
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(8.dp)
                    ) {
                        // File Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = selectedFile ?: "Select a file",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                            Row {
                                if (selectedFile != null) {
                                    IconButton(
                                        onClick = { viewModel.saveCurrentFile(editableContent) },
                                        modifier = Modifier.size(32.dp).testTag("save_file_button")
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = "Save", tint = MatrixGreen)
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteFile(selectedFile!!) },
                                        modifier = Modifier.size(32.dp).testTag("delete_file_button")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertCoral)
                                    }
                                }
                            }
                        }

                        // Editor Area
                        OutlinedTextField(
                            value = editableContent,
                            onValueChange = { editableContent = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("code_editor_field"),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            placeholder = { Text("Code editor content...") }
                        )
                    }
                }
            }

            1 -> {
                // Live Web Preview via WebView
                if (webPreviewHtml != null) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("live_web_preview"),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webChromeClient = WebChromeClient()
                                webViewClient = WebViewClient()
                                tag = webPreviewHtml
                                loadDataWithBaseURL("https://local.karan/", webPreviewHtml!!, "text/html", "UTF-8", null)
                            }
                        },
                        update = { webView ->
                            if (webView.tag != webPreviewHtml) {
                                webView.tag = webPreviewHtml
                                webView.loadDataWithBaseURL("https://local.karan/", webPreviewHtml!!, "text/html", "UTF-8", null)
                            }
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No index.html found or preview could not be bundled.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            2 -> {
                // Diagnostics Pane
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "PROJECT DIAGNOSTICS & ANALYSIS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (projectAnalysis != null) {
                        val a = projectAnalysis!!
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Total Files: ${a.fileCount}", fontWeight = FontWeight.SemiBold)
                                Text("Total Lines of Code: ${a.totalLines}", fontWeight = FontWeight.SemiBold)
                                Text("Technologies: ${a.detectedTech.joinToString(", ")}", color = NeonPurple)
                                Text("Entrypoint: ${if (a.hasIndexHtml) "Found (index.html)" else "Missing"}")
                                Text("Firebase Setup: ${if (a.hasFirebaseConfig) "Configured" else "None"}")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Issues / Warnings (${a.issues.size}):", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (a.issues.isEmpty()) {
                            Text("No issues detected! Clean build.", color = MatrixGreen)
                        } else {
                            a.issues.forEach { issue ->
                                Text("• $issue", color = AlertCoral, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // New Project Dialog
    if (showNewProjectDialog) {
        var newName by remember { mutableStateOf("") }
        var newDesc by remember { mutableStateOf("") }
        var selectedTech by remember { mutableStateOf("HTML/JS") }

        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("Create New Project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Project Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Technology Stack:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("HTML/JS", "Firebase", "Python", "React").forEach { tech ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedTech == tech) CyberCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (selectedTech == tech) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
                                modifier = Modifier.clickable { selectedTech = tech }
                            ) {
                                Text(
                                    text = tech,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.createNewProject(newName, newDesc, selectedTech)
                            showNewProjectDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_project_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FileNodeRow(
    node: FileNode,
    selectedFile: String?,
    onSelect: (String) -> Unit
) {
    val isSelected = selectedFile == node.path

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else Color.Transparent)
            .clickable {
                if (!node.isDirectory) onSelect(node.path)
            }
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Icon(
            imageVector = if (node.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            tint = if (node.isDirectory) NeonPurple else CyberCyan,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = node.name,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = if (isSelected) CyberCyan else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }

    if (node.isDirectory && node.children.isNotEmpty()) {
        Column(modifier = Modifier.padding(start = 8.dp)) {
            node.children.forEach { child ->
                FileNodeRow(child, selectedFile, onSelect)
            }
        }
    }
}
