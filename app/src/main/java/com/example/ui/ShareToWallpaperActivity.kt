package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

class ShareToWallpaperActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        var receivedVideoUri: Uri? = null
        var receivedImageUri: Uri? = null

        if (intent?.action == Intent.ACTION_SEND) {
            val type = intent.type
            if (type?.startsWith("video/") == true) {
                receivedVideoUri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            } else if (type?.startsWith("image/") == true) {
                receivedImageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
        }
        
        setContent {
            var showSheet by remember { mutableStateOf(receivedVideoUri != null || receivedImageUri != null) }
            
            Box(modifier = Modifier.fillMaxSize()) {
                if (showSheet && (receivedVideoUri != null || receivedImageUri != null)) {
                    SetWallpaperBottomSheet(
                        context = this@ShareToWallpaperActivity,
                        imageUri = receivedImageUri,
                        videoUri = receivedVideoUri,
                        onDismiss = {
                            showSheet = false
                            finish()
                        }
                    )
                }
            }
            
            LaunchedEffect(showSheet) {
                if (!showSheet) {
                    finish()
                }
            }
        }
    }
}
