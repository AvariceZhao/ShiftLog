package com.clockin.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clockin.app.ui.components.AppIcons
import com.clockin.app.ui.history.HistoryScreen
import com.clockin.app.ui.home.HomeScreen
import com.clockin.app.ui.settings.SettingsScreen
import com.clockin.app.ui.settings.UpdateAvailableDialog
import com.clockin.app.ui.theme.ClockInTheme
import com.clockin.app.ui.theme.NightSurface
import com.clockin.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val csvExportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        pendingExport?.let { (content, _) ->
            writeText(uri, content)
        }
        pendingExport = null
    }

    private val backupExportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        pendingExport?.let { (content, _) ->
            writeText(uri, content)
        }
        pendingExport = null
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (!text.isNullOrBlank()) {
            pendingImportText = text
        }
    }

    private var pendingExport: Pair<String, String>? = null
    private var pendingImportText: String? = null

    private fun writeText(uri: Uri, content: String) {
        contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
        }
    }

    override fun onResume() {
        super.onResume()
        (application as ClockInApplication).refreshWidgets()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as ClockInApplication).repository
        val updateCoordinator = (application as ClockInApplication).updateCoordinator
        (application as ClockInApplication).refreshWidgets()

        setContent {
            ClockInTheme {
                val scope = rememberCoroutineScope()
                val promptRelease by updateCoordinator.promptRelease.collectAsStateWithLifecycle()
                var tab by remember { mutableIntStateOf(0) }
                var importText by remember { mutableStateOf<String?>(null) }
                if (pendingImportText != null) {
                    importText = pendingImportText
                    pendingImportText = null
                }
                val tabs = listOf<Triple<Int, String, ImageVector>>(
                    Triple(0, "打卡", AppIcons.Schedule),
                    Triple(1, "历史", AppIcons.History),
                    Triple(2, "设置", AppIcons.Settings),
                )

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(
                            containerColor = NightSurface,
                            tonalElevation = 0.dp,
                            windowInsets = NavigationBarDefaults.windowInsets,
                        ) {
                            tabs.forEach { (index, label, icon) ->
                                NavigationBarItem(
                                    selected = tab == index,
                                    onClick = { tab = index },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    ),
                                )
                            }
                        }
                    },
                ) { padding ->
                    when (tab) {
                        0 -> HomeScreen(
                            repository = repository,
                            modifier = Modifier.padding(padding),
                        )
                        1 -> HistoryScreen(
                            repository = repository,
                            modifier = Modifier.padding(padding),
                            importText = importText,
                            onImportConsumed = { importText = null },
                            onExportCsv = { content, fileName ->
                                pendingExport = content to fileName
                                csvExportLauncher.launch(fileName)
                            },
                            onExportBackup = { content, fileName ->
                                pendingExport = content to fileName
                                backupExportLauncher.launch(fileName)
                            },
                            onPickImport = {
                                importLauncher.launch(
                                    arrayOf(
                                        "application/json",
                                        "text/csv",
                                        "text/comma-separated-values",
                                        "text/*",
                                    ),
                                )
                            },
                        )
                        2 -> SettingsScreen(
                            repository = repository,
                            modifier = Modifier.padding(padding),
                        )
                    }
                }

                promptRelease?.let { release ->
                    UpdateAvailableDialog(
                        release = release,
                        onDismiss = { updateCoordinator.clearPrompt() },
                        onDismissVersion = {
                            scope.launch {
                                updateCoordinator.dismissRelease(release.tagName)
                            }
                        },
                    )
                }
            }
        }
    }
}
