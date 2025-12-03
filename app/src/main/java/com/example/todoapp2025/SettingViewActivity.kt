package com.example.todoapp2025

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoapp2025.ui.theme.AppTheme
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

import com.example.todoapp2025.data.Todo

class SettingViewActivity : ComponentActivity() {


    private val vm: SettingsViewModel by viewModels { SettingsVMFactory(this) }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var tasks by remember { mutableStateOf(loadAllTasks()) }

            val isDark by vm.isDarkMode.collectAsState(initial = false)
            val isReminders by vm.isRemindersEnabled.collectAsState(initial = false)
            val isDailySummary by vm.isDailySummaryEnabled.collectAsState(initial = false)

            fun refreshStats() {
                tasks = loadAllTasks()
            }

            val totalTasks = tasks.size
            val pendingTasks = tasks.count { !it.completed }
            val completedTasks = tasks.count { it.completed }
            val overdueTasks = tasks.count { it.dueAt != null && it.dueAt!! < System.currentTimeMillis() && !it.completed }

            AppTheme(darkTheme = isDark) {
                Scaffold(
                    topBar = { CenterAlignedTopAppBar(
                        title = { Text("Settings") },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )}
                ) { padding ->
                    Column(
                        Modifier
                            .padding(padding)
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Appearance", style = MaterialTheme.typography.titleMedium)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Dark Mode")
                            Switch(
                                checked = isDark,
                                onCheckedChange = { vm.setDarkMode(it) }
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("Notifications", style = MaterialTheme.typography.titleMedium)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Enable Reminders")
                            Switch(
                                checked = isReminders,
                                onCheckedChange = { vm.setReminders(it) }
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Daily Summary")
                            Switch(
                                checked = isDailySummary,
                                onCheckedChange = { vm.setDailySummary(it) }
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("Stats", style = MaterialTheme.typography.titleMedium)
                        Text("Total Tasks: $totalTasks")
                        Text("Pending Tasks: $pendingTasks")
                        Text("Completed Tasks: $completedTasks")
                        Text("Overdue Tasks: $overdueTasks")

                        Spacer(Modifier.height(16.dp))
                        Divider()
                        Button(
                            onClick = {
                                clearAllTasks()
                                refreshStats()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clear All Tasks")
                        }
                    }
                }
            }
        }
    }

    private fun loadAllTasks(): List<Todo> {
        val prefs = getSharedPreferences("todo_prefs", MODE_PRIVATE)
        val listNames = prefs.getString("list_names", "Default")?.split("||") ?: listOf("Default")
        val allTasks = mutableListOf<Todo>()
        for (listName in listNames) {
            val serialized = prefs.getString("tasks_$listName", "") ?: ""
            if (serialized.isEmpty()) continue
            allTasks += serialized.split("||").mapNotNull { item ->
                val parts = item.split("::")
                try {
                    Todo(
                        id = parts[0].toLong(),
                        title = parts[1],
                        category = parts[2],
                        dueAt = parts[3].toLong().takeIf { it >= 0 },
                        priority = parts[4].toInt(),
                        completed = parts[5].toBoolean()
                    )
                } catch (_: Exception) { null }
            }
        }
        return allTasks
    }

    private fun clearAllTasks() {
        val prefs = getSharedPreferences("todo_prefs", MODE_PRIVATE)
        val listNames = prefs.getString("list_names", "Default")?.split("||") ?: listOf("Default")
        for (listName in listNames) {
            prefs.edit().remove("tasks_$listName").apply()
        }
    }


}
