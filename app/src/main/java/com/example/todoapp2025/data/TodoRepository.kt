package com.example.todoapp2025.data

import kotlinx.coroutines.flow.Flow

class TodoRepository(private val dao: TodoDao) {
    fun all(): Flow<List<Todo>> = dao.getAll()
    suspend fun add(todo: Todo) = dao.upsert(todo)
    suspend fun update(todo: Todo) = dao.update(todo)
    suspend fun delete(todo: Todo) = dao.delete(todo)
    suspend fun deleteCompleted() = dao.deleteCompleted()



}
