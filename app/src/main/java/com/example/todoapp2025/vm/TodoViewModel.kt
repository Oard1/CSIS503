package com.example.todoapp2025.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp2025.data.Todo
import com.example.todoapp2025.data.TodoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(
    val items: List<Todo> = emptyList(),
    val sort: SortSpec = SortSpec()
)

class TodoViewModel(private val repo: TodoRepository) : ViewModel() {

    private val sortSpec = MutableStateFlow(SortSpec())
    private val base: Flow<List<Todo>> = repo.all()

    val state: StateFlow<UiState> = combine(base, sortSpec) { list, sort ->
        UiState(items = list.sortedWith(comparator(sort)), sort = sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    private fun comparator(s: SortSpec): Comparator<Todo> {
        val cmp = when (s.by) {
            SortBy.TITLE      -> compareBy<Todo> { it.title.lowercase() }
            SortBy.CATEGORY   -> compareBy<Todo> { it.category.lowercase() }
            SortBy.DUE_AT     -> compareBy<Todo> { it.dueAt ?: Long.MAX_VALUE }
            SortBy.PRIORITY   -> compareBy<Todo> { it.priority }
            SortBy.CREATED_AT -> compareBy<Todo> { it.createdAt }
        }
        return if (s.ascending) cmp else cmp.reversed()
    }

    fun update(t: Todo) {
        viewModelScope.launch {
            repo.update(t)
        }
    }



    fun setSort(by: SortBy? = null, toggleAscIfSame: Boolean = false) {
        val cur = sortSpec.value
        val next = if (by == null) cur.copy(ascending = !cur.ascending)
        else if (by == cur.by && toggleAscIfSame) cur.copy(ascending = !cur.ascending)
        else cur.copy(by = by)
        sortSpec.value = next
    }


    fun add(title: String, category: String, dueAt: Long?, priority: Int) = viewModelScope.launch {
        if (title.isNotBlank()) repo.add(
            Todo(
                title = title.trim(),
                category = category.trim(),
                dueAt = dueAt,
                priority = priority
            )
        )
    }

    fun toggleComplete(t: Todo) = viewModelScope.launch { repo.update(t.copy(completed = !t.completed)) }
    fun delete(t: Todo) = viewModelScope.launch { repo.delete(t) }
    fun clearCompleted() = viewModelScope.launch { repo.deleteCompleted() }
}
