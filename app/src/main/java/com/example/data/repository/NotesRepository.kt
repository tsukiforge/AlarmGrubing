package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.Note
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class NotesRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("notes_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listMyData = Types.newParameterizedType(List::class.java, Note::class.java)
    private val adapter = moshi.adapter<List<Note>>(listMyData)

    fun getNotes(): List<Note> {
        val json = prefs.getString("all_notes", null) ?: return emptyList()
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveNotes(notes: List<Note>) {
        try {
            val json = adapter.toJson(notes)
            prefs.edit().putString("all_notes", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addNote(note: Note) {
        val current = getNotes().toMutableList()
        current.add(0, note) // add new note to the top
        saveNotes(current)
    }

    fun updateNote(updated: Note) {
        val current = getNotes().map {
            if (it.id == updated.id) updated else it
        }
        saveNotes(current)
    }

    fun deleteNote(id: String) {
        val current = getNotes().filter { it.id != id }
        saveNotes(current)
    }
}
