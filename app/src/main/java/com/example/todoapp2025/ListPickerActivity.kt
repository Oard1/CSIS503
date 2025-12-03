package com.example.todoapp2025

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.todoapp2025.ui.theme.AppTheme
import kotlinx.coroutines.flow.collectLatest

class ListPickerActivity : ComponentActivity() {

    val settingsVM: SettingsViewModel by viewModels()


    private var isGuest = false

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isGuest = intent.getBooleanExtra("isGuest", false)
        val prefs = getSharedPreferences("todo_prefs", MODE_PRIVATE)

        val settingsVM: SettingsViewModel by viewModels { SettingsVMFactory(this) }

        setContent {
            val isDark by settingsVM.isDarkMode.collectAsState(initial = false)

            AppTheme(darkTheme = isDark) {
                ListPickerScreen(isGuest, prefs)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ListPickerScreen(isGuest: Boolean, prefs: android.content.SharedPreferences) {
        var lists by remember {
            mutableStateOf(
                prefs.getString("list_names", "Default")
                    ?.split("||")
                    ?.toMutableList()
                    ?: mutableListOf("Default")
            )
        }

        var newName by remember { mutableStateOf("") }
        var renameTarget by remember { mutableStateOf<String?>(null) }
        var renameInput by remember { mutableStateOf("") }
        var currentList by remember {
            mutableStateOf(prefs.getString("current_list", "Default") ?: "Default")
        }

        val context = LocalContext.current

        Scaffold(
            topBar = { CenterAlignedTopAppBar(title = { Text("Select List") }) }
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .padding(16.dp)
            ) {

                // CREATE NEW LIST BUTTON (non-guests only)
                if (!isGuest) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New List Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                lists.add(newName)  // add to mutable list
                                prefs.edit()
                                    .putString("list_names", lists.joinToString("||"))
                                    .apply()
                                newName = ""
                            }

                        },
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Text("Create List")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // SHOW LISTS
                LazyColumn {
                    items(lists) { listName ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = listName + if (listName == currentList) " (current)" else "",
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            // SELECT LIST
                                            prefs.edit()
                                                .putString("current_list", listName)
                                                .apply()
                                            currentList = listName

                                            val intent = Intent(
                                                this@ListPickerActivity,
                                                MainActivity::class.java
                                            )
                                            intent.flags =
                                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                        }
                                )

                                if (!isGuest) {
                                    Text(
                                        "Rename",
                                        modifier = Modifier
                                            .padding(end = 12.dp)
                                            .clickable {
                                                renameTarget = listName
                                                renameInput = listName
                                            }
                                    )

                                    if (lists.size > 1) { // cannot delete last list
                                        Text(
                                            "Delete",
                                            modifier = Modifier.clickable {
                                                val updated = lists.toMutableList()
                                                updated.remove(listName)

                                                prefs.edit()
                                                    .putString("list_names", updated.joinToString("||"))
                                                    .apply()

                                                prefs.edit()
                                                    .remove("tasks_$listName")
                                                    .apply()

                                                if (currentList == listName) {
                                                    prefs.edit()
                                                        .putString("current_list", updated.first())
                                                        .apply()
                                                    currentList = updated.first()
                                                }

                                                lists = updated
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // RENAME DIALOG
            if (renameTarget != null) {
                AlertDialog(
                    onDismissRequest = { renameTarget = null },
                    title = { Text("Rename List") },
                    text = {
                        OutlinedTextField(
                            value = renameInput,
                            onValueChange = { renameInput = it },
                            label = { Text("New Name") }
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val old = renameTarget!!
                            val new = renameInput.trim()

                            if (new.isNotEmpty() && new != old) {
                                val updated = lists.toMutableList()
                                val index = updated.indexOf(old)
                                updated[index] = new

                                val oldTasks = prefs.getString("tasks_$old", null)
                                prefs.edit().remove("tasks_$old").apply()
                                if (oldTasks != null) {
                                    prefs.edit().putString("tasks_$new", oldTasks).apply()
                                }

                                if (currentList == old) {
                                    prefs.edit()
                                        .putString("current_list", new)
                                        .apply()
                                    currentList = new
                                }

                                prefs.edit()
                                    .putString("list_names", updated.joinToString("||"))
                                    .apply()

                                lists = updated
                            }

                            renameTarget = null
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { renameTarget = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }


}
