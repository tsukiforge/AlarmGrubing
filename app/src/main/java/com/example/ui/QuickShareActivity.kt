package com.example.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.MainActivity

class QuickShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action = intent.action
        
        setContent {
            var showDialog by remember { mutableStateOf(true) }
            
            if (showDialog) {
                Dialog(
                    onDismissRequest = { 
                        showDialog = false
                        finish()
                    },
                    properties = DialogProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true,
                        usePlatformDefaultWidth = false
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E1E2A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (action == "ACTION_SEND_FILE") "Kirim Berkas" else "Terima Berkas",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                if (action == "ACTION_RECEIVE_FILE") {
                                    var pinInput by remember { mutableStateOf("") }
                                    Text("Masukkan Kode PIN 6-digit:", color = Color.Gray, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = pinInput,
                                        onValueChange = { if (it.length <= 6) pinInput = it.filter { c -> c.isDigit() } },
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            if (pinInput.length == 6) {
                                                Toast.makeText(this@QuickShareActivity, "Mengunduh...", Toast.LENGTH_SHORT).show()
                                                // Normally here we would call ViewModel, but for Quick Widget we can forward it to MainActivity
                                                // Or just launch MainActivity directly telling it to download
                                                val intent = Intent(this@QuickShareActivity, MainActivity::class.java).apply {
                                                    putExtra("DOWNLOAD_PIN", pinInput)
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                }
                                                startActivity(intent)
                                                finish()
                                            } else {
                                                Toast.makeText(this@QuickShareActivity, "PIN harus 6 digit", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                                    ) {
                                        Text("Unduh", color = Color.White)
                                    }
                                } else {
                                    Text("Pilih berkas yang ingin kamu kirim.", color = Color.Gray, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            // Launch Main Activity with intent to pick and send
                                            val intent = Intent(this@QuickShareActivity, MainActivity::class.java).apply {
                                                putExtra("ACTION_PICK_SEND", true)
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            }
                                            startActivity(intent)
                                            finish()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                    ) {
                                        Text("Pilih Berkas", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
