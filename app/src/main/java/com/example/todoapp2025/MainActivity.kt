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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.todoapp2025.data.Todo
import com.example.todoapp2025.ui.theme.AppTheme
import com.example.todoapp2025.vm.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val factory by lazy { TodoVMFactory(application) }
    private val vm: TodoViewModel by viewModels { factory }
    private lateinit var tasks: SnapshotStateList<Todo>

    private var currentListName = "Default"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isGuest = intent.getBooleanExtra("isGuest", false)
        val prefs = getSharedPreferences("todo_prefs", MODE_PRIVATE)

        currentListName = prefs.getString("current_list", "Default") ?: "Default"

        tasks =
            if (isGuest) mutableStateListOf() else loadUserTasks(currentListName).toMutableStateList()

        val settingsVM: SettingsViewModel by viewModels { SettingsVMFactory(this) }

        setContent {
            val isDark by settingsVM.isDarkMode.collectAsState(initial = false)

            AppTheme(darkTheme = isDark) {
                App(vm, settingsVM, isGuest, tasks, currentListName)
            }
        }
    }

    private fun saveUserTasks(tasks: List<Todo>, listName: String) {
        val prefs = getSharedPreferences("todo_prefs", MODE_PRIVATE)
        val serialized = tasks.joinToString("||") { t ->
            listOf(t.id, t.title, t.category, t.dueAt ?: -1L, t.priority, t.completed).joinToString(
                "::"
            )
        }
        prefs.edit().putString("tasks_$listName", serialized).apply()
    }

    private fun loadUserTasks(listName: String): MutableList<Todo> {
        val prefs = getSharedPreferences("todo_prefs", MODE_PRIVATE)
        val saved = prefs.getString("tasks_$listName", "") ?: ""
        if (saved.isEmpty()) return mutableListOf()
        return saved.split("||").mapNotNull { item ->
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
            } catch (e: Exception) {
                null
            }
        }.toMutableList()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun App(
        vm: TodoViewModel,
        settingsVM: SettingsViewModel,
        isGuest: Boolean,
        tasks: MutableList<Todo>,
        currentListName: String
    ) {
        val state by vm.state.collectAsState()
        val context = LocalContext.current
        var editing by remember { mutableStateOf<Todo?>(null) }

        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        MaterialTheme {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        Text(
                            "Navigate",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("Main") },
                            selected = true,
                            onClick = { scope.launch { drawerState.close() } },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            label = { Text("Settings") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                context.startActivity(
                                    Intent(
                                        context,
                                        SettingViewActivity::class.java
                                    )
                                )
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        if (!isGuest) {
                            NavigationDrawerItem(
                                label = { Text("Select List") },
                                selected = false,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    val intent = Intent(context, ListPickerActivity::class.java)
                                    intent.putExtra("isGuest", isGuest)
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }

                        NavigationDrawerItem(
                            label = { Text("Logout") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                val intent = Intent(context, LoginActivity::class.java)
                                intent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                context.startActivity(intent)
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("To-Do") },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = "Menu"
                                    )
                                }
                            },
                            actions = {
                                SortMenu(
                                    current = state.sort,
                                    onPick = { vm.setSort(it, toggleAscIfSame = true) })
                            }
                        )
                    },
                    bottomBar = {
                        BottomAppBar(actions = { TextButton(onClick = vm::clearCompleted) { Text("Clear completed") } })
                    }
                ) { padding ->
                    Column(Modifier.padding(padding).padding(16.dp)) {
                        if (isGuest) {
                            val guestTasks = remember { tasks.toMutableStateList() }
                            AddRowGuest(onAdd = { guestTasks.add(it) })
                            Spacer(Modifier.height(12.dp))
                            TodoListGuest(
                                items = guestTasks,
                                onToggle = { todo ->
                                    val idx = guestTasks.indexOf(todo)
                                    if (idx != -1) guestTasks[idx] =
                                        guestTasks[idx].copy(completed = !guestTasks[idx].completed)
                                },
                                onDelete = { guestTasks.remove(it) },
                                onEdit = { editing = it }
                            )
                        } else {
                            AddRow(onAdd = { title, cat, due, prio ->
                                val todo = Todo(
                                    id = Random().nextLong(),
                                    title = title,
                                    category = cat,
                                    dueAt = due,
                                    priority = prio,
                                    completed = false
                                )
                                tasks.add(todo)
                                saveUserTasks(tasks, currentListName)
                            })
                            Spacer(Modifier.height(12.dp))
                            TodoList(
                                items = tasks,
                                onToggle = { todo ->
                                    val idx = tasks.indexOf(todo)
                                    if (idx != -1) {
                                        tasks[idx] =
                                            tasks[idx].copy(completed = !tasks[idx].completed)
                                        saveUserTasks(tasks, currentListName)
                                    }
                                },
                                onDelete = { todo ->
                                    tasks.remove(todo)
                                    saveUserTasks(tasks, currentListName)
                                },
                                onEdit = { editing = it }
                            )
                        }

                        if (editing != null) {
                            EditDialog(
                                editing!!,
                                onDismiss = { editing = null },
                                onSave = { updated ->
                                    val idx = tasks.indexOfFirst { it.id == updated.id }
                                    if (idx != -1) {
                                        tasks[idx] = updated
                                        saveUserTasks(tasks, currentListName)
                                    }
                                    editing = null
                                })
                        }
                    }
                }
            }
        }
    }

    // --- Composables below ---
    @Composable
    fun AddRowGuest(onAdd: (Todo) -> Unit) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("") }
        var dueText by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf("3") }

        fun parseDue(): Long? = try {
            if (dueText.isBlank()) null else SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.getDefault()
            ).parse(dueText)?.time
        } catch (_: Exception) {
            null
        }

        Column(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = dueText,
                    onValueChange = { dueText = it },
                    label = { Text("Due (yyyy-MM-dd HH:mm)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = priority,
                    onValueChange = { priority = it.filter { ch -> ch.isDigit() }.take(1) },
                    label = { Text("P(1-5)") },
                    modifier = Modifier.width(90.dp)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = {
                    val todo = Todo(
                        Random().nextLong(),
                        title,
                        category,
                        parseDue(),
                        priority.toIntOrNull()?.coerceIn(1, 5) ?: 3,
                        false
                    )
                    onAdd(todo)
                    title = ""; category = ""; dueText = ""; priority = "3"
                }, enabled = title.isNotBlank()) { Text("Add") }
            }
        }
    }

    @Composable
    fun AddRow(onAdd: (String, String, Long?, Int) -> Unit) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("") }
        var dueText by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf("3") }

        fun parseDue(): Long? = try {
            if (dueText.isBlank()) null else SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.getDefault()
            ).parse(dueText)?.time
        } catch (_: Exception) {
            null
        }

        Column(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = dueText,
                    onValueChange = { dueText = it },
                    label = { Text("Due (yyyy-MM-dd HH:mm)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = priority,
                    onValueChange = { priority = it.filter { ch -> ch.isDigit() }.take(1) },
                    label = { Text("P(1-5)") },
                    modifier = Modifier.width(90.dp)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = {
                    onAdd(title, category, parseDue(), priority.toIntOrNull()?.coerceIn(1, 5) ?: 3)
                    title = ""; category = ""; dueText = ""; priority = "3"
                }, enabled = title.isNotBlank()) { Text("Add") }
            }
        }
    }

    @Composable
    fun TodoListGuest(
        items: List<Todo>,
        onToggle: (Todo) -> Unit,
        onDelete: (Todo) -> Unit,
        onEdit: (Todo) -> Unit
    ) {
        if (items.isEmpty()) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text("No items yet — add your first task!") }; return
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { t -> TodoRow(t, onToggle, onDelete, onEdit) }
        }
    }

    @Composable
    fun TodoList(
        items: List<Todo>,
        onToggle: (Todo) -> Unit,
        onDelete: (Todo) -> Unit,
        onEdit: (Todo) -> Unit
    ) {
        if (items.isEmpty()) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text("No items yet — add your first task!") }; return
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { t -> TodoRow(t, onToggle, onDelete, onEdit) }
        }
    }

    @Composable
    fun TodoRow(
        t: Todo,
        onToggle: (Todo) -> Unit,
        onDelete: (Todo) -> Unit,
        onEdit: (Todo) -> Unit
    ) {
        val due = t.dueAt?.let {
            SimpleDateFormat(
                "MMM d, yyyy HH:mm",
                Locale.getDefault()
            ).format(Date(it))
        } ?: "—"
        ElevatedCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = t.completed, onCheckedChange = { onToggle(t) })
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(
                        t.title,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (t.completed) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text("Category: ${t.category.ifBlank { "—" }} • Due: $due • Priority: ${t.priority}")
                }
                Text("Delete", modifier = Modifier.padding(start = 12.dp).clickable { onDelete(t) })
                Text("Edit", modifier = Modifier.padding(start = 12.dp).clickable { onEdit(t) })
            }
        }
    }

    @Composable
    fun EditDialog(todo: Todo, onDismiss: () -> Unit, onSave: (Todo) -> Unit) {
        var title by remember { mutableStateOf(todo.title) }
        var category by remember { mutableStateOf(todo.category) }
        var dueText by remember {
            mutableStateOf(todo.dueAt?.let {
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm",
                    Locale.getDefault()
                ).format(Date(it))
            } ?: "")
        }
        var priority by remember { mutableStateOf(todo.priority.toString()) }


        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Edit Task") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") })
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") })
                    OutlinedTextField(
                        value = dueText,
                        onValueChange = { dueText = it },
                        label = { Text("Due (yyyy-MM-dd HH:mm)") })
                    OutlinedTextField(
                        value = priority,
                        onValueChange = { priority = it.filter { ch -> ch.isDigit() }.take(1) },
                        label = { Text("Priority (1-5)") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newDue = try {
                        if (dueText.isBlank()) null else SimpleDateFormat(
                            "yyyy-MM-dd HH:mm",
                            Locale.getDefault()
                        ).parse(dueText)?.time
                    } catch (_: Exception) {
                        null
                    }
                    onSave(
                        todo.copy(
                            title = title,
                            category = category,
                            dueAt = newDue,
                            priority = priority.toIntOrNull()?.coerceIn(1, 5) ?: todo.priority
                        )
                    )
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )


    }

    @Composable
    fun SortMenu(current: SortSpec, onPick: (SortBy) -> Unit) {
        var open by remember { mutableStateOf(false) }


        Box {
            AssistChip(
                onClick = { open = true },
                label = { Text("Sort: ${current.by.name} ${if (current.ascending) "↑" else "↓"}") })
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                SortBy.values().forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt.name) },
                        onClick = {
                            open = false
                            onPick(opt)
                        }
                    )
                }
            }
        }


    }
}

