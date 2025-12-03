package com.example.todoapp2025.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos")
    fun getAll(): Flow<List<Todo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(todo: Todo): Long

    @Update suspend fun update(todo: Todo)
    @Delete suspend fun delete(todo: Todo)


    @Query("DELETE FROM todos WHERE completed = 1")
    suspend fun deleteCompleted()
}
