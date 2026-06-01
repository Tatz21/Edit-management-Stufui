package com.example.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.domain.model.Project
import com.example.presentation.MainViewModel
import com.example.util.JsonUtil
import com.squareup.moshi.Types
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val darkTheme by viewModel.isDarkTheme.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val projects by viewModel.projects.collectAsState()

    var showRestoreDialog by remember { mutableStateOf(false) }
    var backupJsonInput by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Agency Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Account card layout
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(26.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = currentUser?.name ?: "Local Admin Sandbox",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = currentUser?.email ?: "tatzmondal@gmail.com",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Agency Owner", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Visual toggle rows
            Text("SYSTEM CONFIGURATIONS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            
            // Dark mode Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (darkTheme) Icons.Default.DarkMode else Icons.Default.LightMode, 
                        contentDescription = null, 
                        modifier = Modifier.size(20.dp), 
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Workspace Theme Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(if (darkTheme) "Cinematic Dark Mode active" else "Workspace Light active", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                
                Switch(
                    checked = darkTheme, 
                    onCheckedChange = { viewModel.toggleTheme() },
                    modifier = Modifier.testTag("dark_theme_switch")
                )
            }

            // Cloud Sync trigger info card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .clickable { viewModel.triggerManualSync() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Firebase Sync Connection", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Database state: $syncStatus", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                Button(
                    onClick = { viewModel.triggerManualSync() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981).copy(alpha = 0.12f), contentColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Backup & Restore
            Text("DATA BACKUP UTILS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        exportDatabaseBackupFile(context, projects)
                    },
                    modifier = Modifier.weight(1f).testTag("export_backup_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Backup", fontSize = 12.sp)
                }

                Button(
                    onClick = { showRestoreDialog = true },
                    modifier = Modifier.weight(1f).testTag("restore_backup_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore Backup", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout card Action
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.handleSignOut() }
                    .testTag("logout_button")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout from Agent Workspace", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Local Database Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Paste in exported JSON backup string below to restore pipeline details immediately:")
                    OutlinedTextField(
                        value = backupJsonInput,
                        onValueChange = { backupJsonInput = it },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        placeholder = { Text("Paste JSON string structure here...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = restoreDatabaseFromJson(context, backupJsonInput, viewModel)
                        if (success) {
                            showRestoreDialog = false
                            backupJsonInput = ""
                        }
                    },
                    modifier = Modifier.testTag("restore_confirm_btn")
                ) {
                    Text("Settle and Restore")
                }
            }
        )
    }
}

// Portable Local SharedPreferences db exporter tool
private fun exportDatabaseBackupFile(context: Context, list: List<Project>) {
    try {
        val adapter = JsonUtil.moshi.adapter<List<Project>>(
            Types.newParameterizedType(List::class.java, Project::class.java)
        )
        val jsonString = adapter.toJson(list)
        
        val file = File(context.cacheDir, "EditFlowPro_Data_Backup.json")
        FileOutputStream(file).use { out -> out.write(jsonString.toByteArray()) }

        val uri = FileProvider.getUriForFile(context, "com.aistudio.editflowpro.tqzxlw.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Save JSON Backup via"))
    } catch (e: Exception) {
        Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// JSON Database restorator tool
private fun restoreDatabaseFromJson(context: Context, json: String, viewModel: MainViewModel): Boolean {
    if (json.isEmpty()) return false
    return try {
        val adapter = JsonUtil.moshi.adapter<List<Project>>(
            Types.newParameterizedType(List::class.java, Project::class.java)
        )
        val restoredList = adapter.fromJson(json) ?: return false
        
        restoredList.forEach { proj ->
            viewModel.addOrUpdateProject(proj)
        }
        Toast.makeText(context, "Database restored with ${restoredList.size} items successfully!", Toast.LENGTH_LONG).show()
        true
    } catch (e: Exception) {
        Toast.makeText(context, "Restore failed: invalid JSON structure.", Toast.LENGTH_LONG).show()
        false
    }
}
