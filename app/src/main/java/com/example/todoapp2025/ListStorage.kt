package com.example.todoapp2025.util

import android.content.Context

private const val PREF = "LIST_STORAGE"
private const val KEY = "todo_lists"

fun loadLists(context: Context): MutableList<String> {
    val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY, "") ?: ""
    return if (raw.isBlank()) mutableListOf("Default")
    else raw.split("|").toMutableList()
}

fun saveLists(context: Context, lists: List<String>) {
    val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY, lists.joinToString("|")).apply()
}
