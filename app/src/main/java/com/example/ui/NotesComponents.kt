package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Note
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
import java.text.SimpleDateFormat
import java.util.Locale
fun NotesScreen(
    notes: List<com.example.data.model.Note>,
    onAddNote: (title: String, content: String, colorHex: String) -> Unit,
    onUpdateNote: (note: com.example.data.model.Note, title: String, content: String, colorHex: String) -> Unit,
    onDeleteNote: (id: String) -> Unit
) {
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<com.example.data.model.Note?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (notes.isEmpty()) {
            EmptyStatePlaceholder(
                title = "Belum ada catatan 🌸",
                subtitle = "Catat tugas, ide, atau memo penting di sini. Bisa dipasang sebagai widget di layar HP kamu!",
                icon = Icons.Default.Edit
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 80.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { noteToEdit = note },
                        onDelete = { onDeleteNote(note.id) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddNoteDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 8.dp),
            containerColor = PinkAccent,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Tambah Catatan",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    if (showAddNoteDialog) {
        AddEditNoteDialog(
            note = null,
            onDismiss = { showAddNoteDialog = false },
            onSave = { title, content, color ->
                onAddNote(title, content, color)
                showAddNoteDialog = false
            }
        )
    }

    if (noteToEdit != null) {
        AddEditNoteDialog(
            note = noteToEdit,
            onDismiss = { noteToEdit = null },
            onSave = { title, content, color ->
                onUpdateNote(noteToEdit!!, title, content, color)
                noteToEdit = null
            }
        )
    }
}

@Composable
fun NoteCard(
    note: com.example.data.model.Note,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = try {
        Color(android.graphics.Color.parseColor(note.colorHex))
    } catch (e: Exception) {
        Color(0xFFFFF0F2)
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title.ifBlank { "Tanpa Judul" },
                    color = Color(0xFF3E2723),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Catatan",
                        tint = Color(0xFFD84315),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = note.content,
                color = Color(0xFF4E342E),
                fontSize = 12.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            val formattedDate = remember(note.lastUpdated) {
                val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                sdf.format(java.util.Date(note.lastUpdated))
            }

            Text(
                text = "Diperbarui: $formattedDate",
                color = Color(0xFF8D6E63),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Catatan?", color = TextLight) },
            text = { Text("Apakah kamu yakin ingin menghapus catatan ini?", color = TextMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Hapus", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun AddEditNoteDialog(
    note: com.example.data.model.Note?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, colorHex: String) -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var selectedColor by remember { mutableStateOf(note?.colorHex ?: "#FFF0F2") }

    val colors = listOf(
        "#FFF0F2" to "Sakura",
        "#FFF3E0" to "Peach",
        "#E8F5E9" to "Matcha",
        "#E3F2FD" to "Sky",
        "#F3E5F5" to "Lavender"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDarkElevated),
            shape = RoundedCornerShape(20.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = if (note == null) "Tulis Catatan Baru 🌸" else "Edit Catatan 📝",
                        color = TextLight,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Judul Catatan", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = SurfaceDarkElevated,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Detail Catatan", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = SurfaceDarkElevated,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    Text("Pilih Warna Kertas Memo", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colors.forEach { pair ->
                            val c = Color(android.graphics.Color.parseColor(pair.first))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable { selectedColor = pair.first }
                                    .border(
                                        width = if (selectedColor == pair.first) 2.dp else 0.dp,
                                        color = if (selectedColor == pair.first) IndigoPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Batal", color = TextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (content.isNotBlank()) {
                                    onSave(title, content, selectedColor)
                                }
                            },
                            enabled = content.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Simpan", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
